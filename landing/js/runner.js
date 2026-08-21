/**
 * RunnerPresenter: runs a library page's model in the reader's browser.
 *
 * Progressive enhancement: the server renders a static <pre><code> listing that works without JS.
 * When this module loads, it replaces that listing with an Ace editor so the reader can modify the
 * model before running it. A Reset button restores the original text.
 *
 * @license BSD-3-Clause
 */

import {WasmLayer} from "wasm";
import {DataQuery, summarizeDatasets} from "summarize";
import {GridPresenter, ScrubPresenter} from "viz";


const STRUCTURAL_VARIABLES = new Set(["step", "position.x", "position.y"]);


async function fetchAsBase64(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error("HTTP " + response.status + " fetching " + url);
  }
  const bytes = new Uint8Array(await response.arrayBuffer());
  let binary = "";
  const chunk = 8192;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}


class RunnerPresenter {

  constructor(root, options) {
    const self = this;
    self._root = root;
    self._simulation = options.simulation;
    self._dataManifest = options.dataManifest || {};
    self._source = options.source;
    self._originalCode = self._source ? self._source.textContent : "";

    self._button = root.querySelector(".run-button");
    self._resetButton = root.querySelector(".reset-button");
    self._status = root.querySelector(".run-status");
    self._results = root.querySelector(".run-results");

    self._editor = null;
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

  attach() {
    const self = this;
    if (!self._button) {
      return;
    }
    self._initEditor();
    self._button.removeAttribute("disabled");
    self._button.addEventListener("click", () => self._run());
    if (self._resetButton) {
      self._resetButton.removeAttribute("disabled");
      self._resetButton.addEventListener("click", () => self._reset());
    }
  }

  _initEditor() {
    const self = this;
    if (!self._source) {
      return;
    }

    const pre = self._source.closest("pre");
    if (!pre) {
      return;
    }

    const editorDiv = document.createElement("div");
    editorDiv.id = "run-editor";
    editorDiv.className = "run-editor";
    pre.parentNode.replaceChild(editorDiv, pre);

    ace.config.set("basePath", "/");
    self._editor = ace.edit(editorDiv);
    self._editor.getSession().setUseWorker(false);
    self._editor.setTheme("ace/theme/textmate");
    self._editor.getSession().setMode("ace/mode/joshlang");
    self._editor.session.setOptions({ tabSize: 2, useSoftTabs: true });
    self._editor.setOption("printMarginColumn", 100);
    self._editor.setOption("enableKeyboardAccessibility", true);
    self._editor.getSession().setValue(self._originalCode, 1);

    const lineCount = self._editor.getSession().getLength();
    const lineHeight = self._editor.renderer.lineHeight || 16;
    const padding = 16;
    editorDiv.style.height = Math.min(lineCount * lineHeight + padding, 600) + "px";
    self._editor.resize();
  }

  getCode() {
    const self = this;
    if (self._editor) {
      return self._editor.getValue();
    }
    return self._originalCode;
  }

  _reset() {
    const self = this;
    if (self._isRunning) {
      return;
    }
    if (self._editor) {
      self._editor.getSession().setValue(self._originalCode, 1);
    }
    self._results.innerHTML = "";
    self._say("");
  }

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
      self._say("Did not finish: " + (err.message || String(err)), true);
    } finally {
      self._isRunning = false;
      self._button.disabled = false;
    }
  }

  _say(message, failed) {
    const self = this;
    if (!self._status) {
      return;
    }
    self._status.textContent = message;
    self._status.classList.toggle("run-failed", Boolean(failed));
  }

  _show() {
    const self = this;
    const result = self._lastResult;
    const records = result.getPatchResults().length;

    self._variables = Array.from(result.getPatchVariables())
      .filter((name) => !STRUCTURAL_VARIABLES.has(name))
      .sort();

    if (self._variables.length === 0) {
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
