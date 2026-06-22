/**
 * PlaygroundPresenter: manages the interactive Ace editor playground for demo.joshsim.org.
 *
 * Owns an Ace editor showing the full ForeverTree model (joshlang mode, textmate theme) and a
 * collapsible configuration textarea. Ace is lazy-initialized on the first onShow() call so that
 * it never tries to size itself while the containing section is display:none.
 *
 * Component 7: On run completion, fades out the running indicator and fades in the meanHeight
 * heatmap visualization (ScrubPresenter + GridPresenter) into #playground-results.
 *
 * @license BSD-3-Clause
 */

import {WasmLayer} from "wasm";
import {DataQuery, summarizeDatasets} from "summarize";
import {GridPresenter, ScrubPresenter} from "viz";


/**
 * Fetch a URL as a base64-encoded string (for binary .jshd files).
 *
 * @param {string} url - URL to fetch.
 * @returns {Promise<string>} Base64 string of the binary content.
 */
async function fetchAsBase64(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error("HTTP " + response.status + " fetching " + url);
  }
  const buffer = await response.arrayBuffer();
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}


class PlaygroundPresenter {

  /**
   * @param {string} editorId         - id of the <div> Ace will own.
   * @param {string} configTextareaId - id of the <textarea> for the .jshc config.
   * @param {string[]} codeLines      - Full model code as an array of lines (from FOREVERTREE_WASM_SNAPSHOT).
   * @param {string} configPath       - URL/path to fetch the .jshc config file at runtime.
   * @param {Object} [options]        - Optional overrides; all default to the forevertree behavior so
   *     existing call sites are unaffected.
   * @param {Object<string,string>} [options.dataManifest] - Map of virtual .jshd filename (the name
   *     the WASM engine looks up, e.g. "temperature.jshd") to the URL to fetch it from. Defaults to
   *     the two forevertree climate layers.
   * @param {string} [options.configKey] - Virtual filename under which the live config text is passed
   *     to the engine. Defaults to "forevertree.jshc".
   * @param {string} [options.aceBasePath] - Base path for Ace's worker/mode assets. Defaults to
   *     "./third_party"; the management page (one directory deep) passes "../third_party".
   * @param {Array<Object>} [options.resultVariables] - Ordered list of {key, reducer, label} result
   *     variables offered in the viz. When more than one is given, a selector is shown. Defaults to a
   *     single mean meanHeight entry (so the forevertree viz is unchanged).
   */
  constructor(editorId, configTextareaId, codeLines, configPath, options = {}) {
    this._editorId = editorId;
    this._configTextareaId = configTextareaId;
    this._codeLines = codeLines;
    this._configPath = configPath;
    this._dataManifest = options.dataManifest || {
      "temperature.jshd": "data/temperature.jshd",
      "precipitation.jshd": "data/precipitation.jshd",
    };
    this._configKey = options.configKey || "forevertree.jshc";
    this._aceBasePath = options.aceBasePath || "./third_party";
    this._resultVariables = options.resultVariables
      || [{key: "meanHeight", reducer: "mean", label: "Mean tree height"}];
    this._selectedVariableKey = this._resultVariables[0].key;
    this._aceInitialized = false;
    this._editor = null;
    this._configTextarea = null;

    // Run handler state
    this._wasmLayer = null;
    this._jshdCache = null;
    this._originalConfig = null;
    this._isRunning = false;
    this._lastResult = null;
    this._lastMetadata = null;
    this._scrubPresenter = null;
    this._gridPresenter = null;
    this._currentTimestep = null;
    this._runHandlerAttached = false;
    this._hasRun = false;
    this._onFirstRun = null;
  }

  /**
   * Register a callback fired once, after the first successful run completes.
   *
   * NarrativePresenter uses this to enable the playground's "Next" button only after the user has
   * actually run the simulation, so the Run step can't be silently skipped.
   *
   * @param {function} callback - Invoked with no arguments after the first successful run.
   */
  setOnFirstRun(callback) {
    this._onFirstRun = callback;
  }

  /**
   * @returns {boolean} True once the simulation has been run at least once this session.
   */
  hasRun() {
    return this._hasRun;
  }

  /**
   * Called by NarrativePresenter after the playground section becomes visible (post-fade-in).
   *
   * Lazy-initializes the Ace editor the first time, then just resizes on subsequent shows.
   * Also fetches the config file and populates the config textarea (first call only).
   * Enables the Run button and attaches the click handler (first call only).
   */
  onShow() {
    const self = this;

    if (self._aceInitialized) {
      if (self._editor) {
        self._editor.resize(true);
      }
      return;
    }

    self._aceInitialized = true;

    self._configTextarea = document.getElementById(self._configTextareaId);

    ace.config.set("basePath", self._aceBasePath);

    self._editor = ace.edit(self._editorId);
    self._editor.getSession().setUseWorker(false);
    self._editor.setTheme("ace/theme/textmate");
    self._editor.getSession().setMode("ace/mode/joshlang");
    self._editor.session.setOptions({ tabSize: 2, useSoftTabs: true });
    self._editor.getSession().setValue(self._codeLines.join("\n"), 1);

    setTimeout(() => {
      if (self._editor) {
        self._editor.resize(true);
      }
    }, 50);

    fetch(self._configPath)
      .then(function(r) {
        if (!r.ok) {
          throw new Error("HTTP " + r.status);
        }
        return r.text();
      })
      .then(function(text) {
        self._originalConfig = text;
        if (self._configTextarea) {
          self._configTextarea.value = text;
        }
      })
      .catch(function(err) {
        console.warn("PlaygroundPresenter: could not load config from " + self._configPath + ":", err);
      });

    // Enable run button and attach handler on first show
    const runButton = document.getElementById("playground-run-button");
    if (runButton) {
      runButton.removeAttribute("disabled");
    }

    if (!self._runHandlerAttached) {
      self._attachRunHandler();
      self._runHandlerAttached = true;
    }
  }

  /**
   * Returns the current Ace editor content as a string.
   *
   * @returns {string} Current model code.
   */
  getCode() {
    if (this._editor) {
      return this._editor.getValue();
    }
    return this._codeLines.join("\n");
  }

  /**
   * Returns the current config textarea content as a string.
   *
   * @returns {string} Current config text.
   */
  getConfig() {
    if (this._configTextarea) {
      return this._configTextarea.value;
    }
    return "";
  }

  /**
   * Returns the SimulationResult from the last completed run, or null if none.
   *
   * @returns {SimulationResult|null} The last simulation result (for Component 7).
   */
  getLastResult() {
    return this._lastResult;
  }

  /**
   * Wire the Run and Reset button clicks.
   */
  _attachRunHandler() {
    const self = this;
    const runButton = document.getElementById("playground-run-button");
    const resetButton = document.getElementById("playground-reset-button");
    const resultsPanel = document.getElementById("playground-results");

    if (!runButton) {
      return;
    }

    // Reset handler: restore original code + config, clear results
    if (resetButton) {
      resetButton.addEventListener("click", function() {
        if (self._isRunning) {
          return;
        }
        if (self._editor) {
          self._editor.setValue(self._codeLines.join("\n"), 1);
        }
        if (self._configTextarea && self._originalConfig !== null) {
          self._configTextarea.value = self._originalConfig;
        }
        if (resultsPanel) {
          resultsPanel.innerHTML = "";
        }
      });
    }

    runButton.addEventListener("click", async function() {
      if (self._isRunning) {
        return;
      }

      self._isRunning = true;
      runButton.disabled = true;

      // Show "Simulation running..." indicator
      resultsPanel.innerHTML = '<div id="playground-running-indicator" class="fade-in">Simulation running...</div>';

      try {
        // Fetch and cache the binary .jshd files once; re-read config on every run
        if (!self._jshdCache) {
          self._jshdCache = await self._loadJshd();
        }

        // Build externalData fresh each run so config edits take effect
        const externalData = {
          ...self._jshdCache,
          [self._configKey]: self.getConfig()
        };

        // Lazily construct WasmLayer
        if (!self._wasmLayer) {
          self._wasmLayer = new WasmLayer();
        }

        const code = self.getCode();

        const result = await self._wasmLayer.runSimulation(
          code,
          "Main",
          externalData,
          function(n) {},
          false,
          ""
        );

        self._lastResult = result;

        const metadata = await self._wasmLayer.getSimulationMetadata(code, "Main");
        self._lastMetadata = metadata;

        await self._showViz(result, metadata, resultsPanel);

        // Mark the first successful run so the narrative can enable "Next".
        if (!self._hasRun) {
          self._hasRun = true;
          if (typeof self._onFirstRun === "function") {
            self._onFirstRun();
          }
        }

      } catch (err) {
        console.error("Simulation error:", err);
        resultsPanel.innerHTML = '<p class="playground-error">Simulation error: ' + (err.message || String(err)) + '</p>';
      } finally {
        self._isRunning = false;
        runButton.disabled = false;
      }
    });
  }

  /**
   * Fade out the running indicator, then inject and render the result heatmap viz.
   *
   * When more than one result variable is offered (options.resultVariables), a selector is shown
   * above the timeline; changing it re-summarizes the cached result for the chosen variable and
   * re-renders without re-running the simulation. With a single variable (the forevertree default)
   * no selector is shown and the layout is unchanged.
   *
   * @param {SimulationResult} result - The completed simulation result.
   * @param {SimulationMetadata} metadata - Metadata parsed from the simulation code.
   * @param {Element} resultsPanel - The #playground-results container element.
   */
  async _showViz(result, metadata, resultsPanel) {
    const self = this;

    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const fadeDuration = reducedMotion ? 0 : 650;

    // Apply fade-out to running indicator then swap in viz markup
    const indicator = document.getElementById("playground-running-indicator");
    if (indicator) {
      indicator.classList.add("fade-out");
    }

    await new Promise((resolve) => setTimeout(resolve, fadeDuration));

    let selectorMarkup = "";
    if (self._resultVariables.length > 1) {
      const opts = self._resultVariables.map((v) =>
        '<option value="' + v.key + '"' + (v.key === self._selectedVariableKey ? " selected" : "")
        + ">" + v.label + "</option>"
      ).join("");
      selectorMarkup = `
  <div class="viz-variable-picker">
    <label for="viz-variable-select">Show</label>
    <select id="viz-variable-select">${opts}</select>
    <span> per patch, per year.</span>
  </div>`;
    }

    const vizMarkup = `
<div id="playground-viz" class="fade-in">${selectorMarkup}
  <div class="viz-holder" id="scrub-viz-holder" title="Timeline of the selected variable per year">
    <p class="playground-hint">Each bar is one year — click a bar to update the map below for that year.</p>
    <svg id="scrub-viz"></svg>
  </div>
  <div class="viz-holder" id="grid-viz-holder" title="Spatial heatmap of the selected variable for the selected year">
    <div id="grid-viz-info"></div>
    <div class="horiz-scroll-area">
      <svg id="grid-viz"></svg>
    </div>
    <table id="grid-legend">
      <tr class="label">
        <td class="lowest"></td>
        <td class="low"></td>
        <td class="high"></td>
        <td class="highest"></td>
      </tr>
      <tr class="color">
        <td class="lowest"></td>
        <td class="low"></td>
        <td class="high"></td>
        <td class="highest"></td>
      </tr>
    </table>
  </div>
</div>`;

    resultsPanel.innerHTML = vizMarkup;

    self._renderSelectedVariable();

    const select = document.getElementById("viz-variable-select");
    if (select) {
      select.addEventListener("change", () => {
        self._selectedVariableKey = select.value;
        self._renderSelectedVariable();
      });
    }
  }

  /**
   * Summarize the cached last result for the currently-selected variable and (re)render the scrub
   * timeline + grid heatmap. Reuses _lastResult/_lastMetadata so switching variables never re-runs
   * the simulation. Preserves the selected timestep across a variable switch when possible.
   */
  _renderSelectedVariable() {
    const self = this;
    if (!self._lastResult || !self._lastMetadata) {
      return;
    }

    const variable = self._resultVariables.find((v) => v.key === self._selectedVariableKey)
      || self._resultVariables[0];
    const query = new DataQuery(variable.key, variable.reducer, null, null, null);
    const summarized = summarizeDatasets([self._lastResult], query);

    const scrubEl = document.getElementById("scrub-viz-holder");
    const gridEl = document.getElementById("grid-viz-holder");
    if (!scrubEl || !gridEl) {
      return;
    }

    self._gridPresenter = new GridPresenter(gridEl);
    self._scrubPresenter = new ScrubPresenter(scrubEl, (step) => {
      self._currentTimestep = step;
      self._gridPresenter.render(self._lastMetadata, summarized, step, null);
    });

    self._scrubPresenter.render(summarized);
  }

  /**
   * Fetch the binary climate .jshd files and return them as a base64 map.
   *
   * These files never change during a session so the result is cached in
   * `this._jshdCache`. The .jshc config is NOT included here — it is merged
   * in on each run so that edits take effect immediately.
   *
   * Binary .jshd files are fetched as base64 (ExternalDataSerializer expects this for isBinary=1
   * files — see wire.js _isTextFile: .jshd is not .csv/.txt/.jshc/.josh so isBinary=1).
   *
   * Keys are the VIRTUAL FILENAMES the WASM engine looks up, including extension (e.g.
   * "temperature.jshd" -> external temperature). The set of files and their source URLs comes from
   * the injected dataManifest, so the management page can supply its burn/managed masks too.
   *
   * @returns {Promise<Object>} Partial externalData map containing only the .jshd entries.
   */
  async _loadJshd() {
    const names = Object.keys(this._dataManifest);
    const base64s = await Promise.all(
      names.map((name) => fetchAsBase64(this._dataManifest[name]))
    );

    const result = {};
    names.forEach((name, i) => {
      result[name] = base64s[i];
    });
    return result;
  }

}


export {PlaygroundPresenter};
