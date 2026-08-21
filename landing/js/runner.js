/**
 * RunnerPresenter: runs a library page's model in the reader's browser.
 *
 * The engine is the same WebAssembly build the editor and the demo use, reached through the same
 * WasmLayer. What differs is where the code comes from: this presenter reads the model out of the
 * listing already on the page rather than holding a copy of it, so the code that runs is
 * necessarily the code the reader is looking at. There is nothing here to keep in sync with the
 * authored `.josh`, because there is no second copy of it.
 *
 * The variables offered in the result viz are discovered from the result itself, not declared. A
 * model's exports are whatever its `export.` handlers wrote, and the records know their own
 * attribute names, so a page gains a new plot by the model exporting a new value.
 *
 * @license BSD-3-Clause
 */

import {WasmLayer} from "wasm";
import {DataQuery, summarizeDatasets} from "summarize";
import {GridPresenter, ScrubPresenter} from "viz";


/**
 * Attributes every patch record carries to say where and when it was taken.
 *
 * They are how a result is placed on the map and the timeline, not things to plot, so they are kept
 * out of the variable picker.
 */
const STRUCTURAL_VARIABLES = new Set(["step", "position.x", "position.y"]);


/**
 * Fetch a URL as a base64-encoded string.
 *
 * Binary `.jshd` files cross the wire to the engine as base64: ExternalDataSerializer marks
 * anything that is not .csv/.txt/.jshc/.josh as binary and expects it encoded.
 *
 * @param {string} url - URL to fetch.
 * @returns {Promise<string>} Base64 string of the binary content.
 */
async function fetchAsBase64(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error("HTTP " + response.status + " fetching " + url);
  }
  const bytes = new Uint8Array(await response.arrayBuffer());
  let binary = "";
  // Chunked because String.fromCharCode is applied to the whole array at once below, and a
  // multi-megabyte spread overflows the argument limit in every engine.
  const chunk = 8192;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}


class RunnerPresenter {

  /**
   * @param {Element} root - The run section, which owns every element this presenter touches.
   * @param {Object} options - What this page needs to run its own model.
   * @param {string} options.simulation - Name of the simulation to run.
   * @param {Object<string,string>} options.dataManifest - Map of the virtual `.jshd` filename the
   *     engine resolves an external by, to the URL this page fetches it from. Empty when the model
   *     reads no external data.
   * @param {Element} options.source - The element holding the model text to run.
   */
  constructor(root, options) {
    const self = this;
    self._root = root;
    self._simulation = options.simulation;
    self._dataManifest = options.dataManifest || {};
    self._source = options.source;

    self._button = root.querySelector(".run-button");
    self._status = root.querySelector(".run-status");
    self._results = root.querySelector(".run-results");

    self._wasmLayer = null;
    self._jshdCache = null;
    self._isRunning = false;
    self._lastResult = null;
    self._lastMetadata = null;
    self._variables = [];
    self._selectedVariable = null;
    self._gridPresenter = null;
    self._scrubPresenter = null;
  }

  /**
   * Wire the run button. The button starts disabled in the markup so that a page whose scripts
   * failed to load offers nothing to click rather than something that does nothing.
   */
  attach() {
    const self = this;
    if (!self._button) {
      return;
    }
    self._button.removeAttribute("disabled");
    self._button.addEventListener("click", () => self._run());
  }

  /**
   * The model to run, read from the listing on the page.
   *
   * @returns {string} The model text.
   */
  getCode() {
    const self = this;
    return self._source ? self._source.textContent : "";
  }

  /**
   * Run the simulation and show what it produced.
   */
  async _run() {
    const self = this;
    if (self._isRunning) {
      return;
    }

    self._isRunning = true;
    self._button.disabled = true;
    self._results.innerHTML = "";
    self._say("Loading the engine…");

    try {
      if (!self._jshdCache) {
        self._say("Fetching data…");
        self._jshdCache = await self._loadJshd();
      }

      if (!self._wasmLayer) {
        self._wasmLayer = new WasmLayer();
      }

      const code = self.getCode();

      // Metadata is read before the run rather than after it, so the progress line can say how far
      // along the run is. A heavy model takes minutes in a browser, and "step 12 of 31" is the
      // difference between a reader waiting and a reader deciding the page is broken.
      self._say("Reading the simulation…");
      self._lastMetadata = await self._wasmLayer.getSimulationMetadata(code, self._simulation);
      const total = self._lastMetadata.getTotalSteps();

      self._say("Running…");
      self._lastResult = await self._wasmLayer.runSimulation(
        code,
        self._simulation,
        self._jshdCache,
        (step) => self._say(total ? `Running… step ${step} of ${total}` : `Running… step ${step}`),
        false,
        ""
      );

      self._show();
    } catch (err) {
      // Assertion failures arrive here too: a failed `assert` aborts the run and the engine
      // reports it as an error, so the message a reader sees is the one the model wrote.
      self._say("Did not finish: " + (err.message || String(err)), true);
    } finally {
      self._isRunning = false;
      self._button.disabled = false;
    }
  }

  /**
   * Put a line of status text above the results.
   *
   * @param {string} message - What to say.
   * @param {boolean} [failed] - Whether this is a failure, which is styled and announced as one.
   */
  _say(message, failed) {
    const self = this;
    if (!self._status) {
      return;
    }
    self._status.textContent = message;
    self._status.classList.toggle("run-failed", Boolean(failed));
  }

  /**
   * Render the finished run: a note of what it produced, then the viz if there is anything to plot.
   */
  _show() {
    const self = this;
    const result = self._lastResult;
    const records = result.getPatchResults().length;

    self._variables = Array.from(result.getPatchVariables())
      .filter((name) => !STRUCTURAL_VARIABLES.has(name))
      .sort();

    if (self._variables.length === 0) {
      // A model with no `export.` handlers still ran, and for one whose assertions are the point
      // that is the whole result. Saying so is better than an empty panel.
      self._say("Ran to completion. This model exports no patch variables to plot.");
      return;
    }

    self._say("Ran to completion: " + records.toLocaleString() + " patch records.");
    self._selectedVariable = self._variables[0];

    const options = self._variables
      .map((name) => '<option value="' + name + '">' + name + "</option>")
      .join("");

    self._results.innerHTML = `
<div class="run-viz">
  <div class="viz-variable-picker">
    <label for="run-variable-select">Show</label>
    <select id="run-variable-select">${options}</select>
    <span> per patch, per step.</span>
  </div>
  <div class="viz-holder" id="scrub-viz-holder">
    <p class="playground-hint">Each bar is one step &mdash; click one to update the map below.</p>
    <svg id="scrub-viz"></svg>
  </div>
  <div class="viz-holder" id="grid-viz-holder">
    <div id="grid-viz-info"></div>
    <div class="horiz-scroll-area">
      <svg id="grid-viz"></svg>
    </div>
    <table id="grid-legend">
      <tr class="label">
        <td class="lowest"></td><td class="low"></td><td class="high"></td><td class="highest"></td>
      </tr>
      <tr class="color">
        <td class="lowest"></td><td class="low"></td><td class="high"></td><td class="highest"></td>
      </tr>
    </table>
  </div>
</div>`;

    const select = self._results.querySelector("#run-variable-select");
    select.addEventListener("change", () => {
      self._selectedVariable = select.value;
      self._plot();
    });

    self._plot();
  }

  /**
   * Summarize the cached result for the selected variable and draw it.
   *
   * The result is reused rather than re-run, so changing the variable costs a summarize rather than
   * a simulation.
   */
  _plot() {
    const self = this;
    const query = new DataQuery(self._selectedVariable, "mean", null, null, null);
    const summarized = summarizeDatasets([self._lastResult], query);

    const scrubEl = self._results.querySelector("#scrub-viz-holder");
    const gridEl = self._results.querySelector("#grid-viz-holder");

    self._gridPresenter = new GridPresenter(gridEl);
    self._scrubPresenter = new ScrubPresenter(scrubEl, (step) => {
      self._gridPresenter.render(self._lastMetadata, summarized, step, null);
    });
    self._scrubPresenter.render(summarized);
  }

  /**
   * Fetch this page's data files and return them keyed by the name the engine looks them up under.
   *
   * @returns {Promise<Object>} The externalData map handed to the engine.
   */
  async _loadJshd() {
    const self = this;
    const names = Object.keys(self._dataManifest);
    const encoded = await Promise.all(names.map((name) => fetchAsBase64(self._dataManifest[name])));

    const loaded = {};
    names.forEach((name, index) => {
      loaded[name] = encoded[index];
    });
    return loaded;
  }

}


export {RunnerPresenter};
