/**
 * PlaygroundPresenter: manages the interactive Ace editor playground for demo.joshsim.org.
 *
 * Owns an Ace editor showing the full ForeverTree model (joshlang mode, textmate theme) and a
 * collapsible configuration textarea. Ace is lazy-initialized on the first onShow() call so that
 * it never tries to size itself while the containing section is display:none.
 *
 * Component 6: Run button wired to execute the forevertree_wasm simulation via WASM (single
 * replicate), with "Simulation running..." indicator shown while running. On completion stores
 * the SimulationResult for Component 7. No heatmap yet.
 *
 * @license BSD-3-Clause
 */

import {WasmLayer} from "wasm";


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
   */
  constructor(editorId, configTextareaId, codeLines, configPath) {
    this._editorId = editorId;
    this._configTextareaId = configTextareaId;
    this._codeLines = codeLines;
    this._configPath = configPath;
    this._aceInitialized = false;
    this._editor = null;
    this._configTextarea = null;

    // Run handler state
    this._wasmLayer = null;
    this._externalDataCache = null;
    this._isRunning = false;
    this._lastResult = null;
    this._runHandlerAttached = false;
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

    ace.config.set("basePath", "./third_party");

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
   * Wire the Run button click to the WASM simulation flow.
   */
  _attachRunHandler() {
    const self = this;
    const runButton = document.getElementById("playground-run-button");
    const resultsPanel = document.getElementById("playground-results");

    if (!runButton) {
      return;
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
        // Load external data (cache after first fetch)
        if (!self._externalDataCache) {
          self._externalDataCache = await self._loadExternalData();
        }

        // Lazily construct WasmLayer
        if (!self._wasmLayer) {
          self._wasmLayer = new WasmLayer();
        }

        const code = self.getCode();

        const result = await self._wasmLayer.runSimulation(
          code,
          "Main",
          self._externalDataCache,
          function(n) {},
          false,
          ""
        );

        self._lastResult = result;

        const patchCount = result.getPatchResults().length;
        console.log("Simulation complete:", result, "Patch records:", patchCount);

        // Hide indicator and show placeholder (Component 7 replaces this)
        resultsPanel.innerHTML = '<p id="playground-done-message">Done — ' + patchCount + ' records. Visualization coming soon.</p>';

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
   * Fetch climate data files and config, returning the externalData object for runSimulation.
   *
   * Binary .jshd files are fetched as base64 (ExternalDataSerializer expects this for isBinary=1
   * files — see wire.js _isTextFile: .jshd is not .csv/.txt/.jshc/.josh so isBinary=1).
   * The .jshc config is plain text (isBinary=0).
   *
   * Keys must match what the model references:
   *   - "temperature"      -> external temperature
   *   - "precipitation"    -> external precipitation
   *   - "forevertree.jshc" -> config forevertree.*
   *
   * @returns {Promise<Object>} externalData object ready for WasmLayer.runSimulation.
   */
  async _loadExternalData() {
    const self = this;

    const [tempBase64, precipBase64] = await Promise.all([
      fetchAsBase64("data/temperature.jshd"),
      fetchAsBase64("data/precipitation.jshd")
    ]);

    const configText = self.getConfig();

    return {
      "temperature": tempBase64,
      "precipitation": precipBase64,
      "forevertree.jshc": configText
    };
  }

}


export {PlaygroundPresenter};
