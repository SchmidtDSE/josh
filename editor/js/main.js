
/**
 * Main entry point for the Josh web editor application.
 * 
 * Initializes and coordinates the different components of the editor including file handling, code
 * editing, run panel, and results display.
 * 
 * @license BSD-3-Clause
 */

import {EditorPresenter} from "editor";
import {FilePresenter} from "file";
import {ResultsPresenter} from "results";
import {RunPanelPresenter} from "run";
import {WasmLayer} from "wasm";

/**
 * Main presenter class that coordinates all editor and results display components.
 */
class MainPresenter {

  /**
   * Creates a new MainPresenter instance.
   * 
   * @param {Object} wasmLayerRaw - The raw WASM layer object containing VM and exported functions.
   */
  constructor(wasmLayerRaw) {
    const self = this;

    self._wasmLayer = new WasmLayer(wasmLayerRaw);

    self._filePresenter = new FilePresenter("file-buttons", (code) => {
      self._editorPresenter.setCode(code);
      self._onCodeChange(code);
    });
    
    self._editorPresenter = new EditorPresenter("code-editor", (code) => {
      self._onCodeChange(code);
    });

    self._runPresenter = new RunPanelPresenter(
        "code-buttons-panel",
        () => self._wasmLayer.getSimulations(self._editorPresenter.getCode()),
        (request) => { self._executeRunRequest(request); }
    );

    self._resultsPresenter = new ResultsPresenter("results");

    const priorCode = self._filePresenter.getCodeInFile();
    if (priorCode) {
      self._editorPresenter.setCode(priorCode);
    }

    self._showContents();
  }

  /**
   * Callback for when a simulation step is completed.
   * 
   * @param {number} count - The number of steps completed.
   */
  onStepCompleted(count) {
    const self = this;
    self._resultsPresenter.onStep();
  }

  /**
   * Shows the main editor interface and hide the loading screen.
   * 
   * @private
   */
  _showContents() {
    const self = this;
    document.getElementById("loading").style.display = "none";
    document.getElementById("main-holder").style.display = "block";
  }

  /**
   * Callback for when code is changed in the editor.
   * 
   * @private
   * @param {string} code - The content of the editor.
   */
  _onCodeChange(code) {
    const self = this;
    self._filePresenter.saveCodeToFile(code);
    
    const errorMaybe = self._wasmLayer.getError(code);

    if (errorMaybe.hasError()) {
      self._editorPresenter.showError(errorMaybe.getError());
      self._runPresenter.hideButtons();
    } else {
      self._editorPresenter.hideError();
      self._runPresenter.showButtons();
    }
  }

  /**
   * Executes a simulation run request.
   * 
   * @private
   * @param {Object} request - The run request containing simulation parameters.
   */
  _executeRunRequest(request) {
    const self = this;
    self._resultsPresenter.onSimStart();
    self._wasmLayer.runSimulation(
        self._editorPresenter.getCode(),
        request.getSimName()
    );
  }
}

/**
 * Initializes the editor and file handling components.
 * 
 * @param {Object} wasmLayer - The WASM VM and exported functions.
 * @returns {MainPresenter} The main presenter instance.
 */
function main(wasmLayer) {
  return new MainPresenter(wasmLayer);
}

export {main};
