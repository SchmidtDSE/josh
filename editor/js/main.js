
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
import {getWasmLayer} from "wasm";


/**
 * Main presenter class that coordinates all editor and results display components.
 */
class MainPresenter {

  /**
   * Creates a new MainPresenter instance.
   */
  constructor() {
    const self = this;

    self._currentRequest = null;
    self._replicatesCompleted = 0;
    self._replicateResults = [];

    self._wasmLayer = getWasmLayer(
      (numSteps) => self._onStepCompleted(numSteps)
    );

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
   * Shows the main editor interface and hide the loading screen.
   */
  _showContents() {
    const self = this;
    document.getElementById("loading").style.display = "none";
    document.getElementById("main-holder").style.display = "block";
  }

  /**
   * Callback for when code is changed in the editor.
   *
   * @param {string} code - The content of the editor.
   */
  _onCodeChange(code) {
    const self = this;
    self._filePresenter.saveCodeToFile(code);
    
    self._wasmLayer.getError(code).then((errorMaybe) => {
      if (errorMaybe.hasError()) {
        self._editorPresenter.showError(errorMaybe.getError());
        self._runPresenter.hideButtons();
      } else {
        self._editorPresenter.hideError();
        self._runPresenter.showButtons();
      }
    });
  }

  /**
   * Executes a simulation run request.
   *
   * @param {Object} request - The run request containing simulation parameters.
   */
  _executeRunRequest(request) {
    const self = this;
    
    self._resultsPresenter.onSimStart();
    self._runPresenter.hideButtons();

    self._currentRequest = request;
    self._replicatesCompleted = 0;
    self._replicateResults = [];
    
    self._executeSingleReplicate();
  }

  _executeSingleReplicate() {
    const self = this;
    self._wasmLayer.runSimulation(
        self._editorPresenter.getCode(),
        self._currentRequest.getSimName()
    ).then(
        (x) => { self._onSimulationComplete(x); },
        (x) => { self._onError(x); }
    );
  }

  _onSimulationComplete(results) {
    const self = this;
    
    self._replicateResults.push(results);
    self._replicatesCompleted++;

    const multiReplicate = self._currentRequest.getReplicates() > 1;
    if (multiReplicate) {
      self._resultsPresenter.onStep(self._replicatesCompleted, "replicates");
    }

    const completed = self._replicatesCompleted >= self._currentRequest.getReplicates();
    if (completed) {
      self._onRunComplete();
    } else {
      self._executeSingleReplicate();
    }
  }

  _onRunComplete() {
    const self = this;
    self._runPresenter.showButtons();
    self._resultsPresenter.onComplete(self._replicateResults);
  }

  /**
   * Callback for when a simulation step is completed.
   */
  _onStepCompleted(stepsCompleted) {
    const self = this;

    const singleReplicate = self._currentRequest.getReplicates() == 1;
    if (singleReplicate) {
      self._resultsPresenter.onStep(stepsCompleted, "steps");
    }
  }

  _onError(message) {
    const self = this;
    
    self._runPresenter.showButtons();
    self._resultsPresenter.onError(message);
  }
}

/**
 * Initializes the editor and file handling components.
 * 
 * @returns {MainPresenter} The main presenter instance.
 */
function main() {
  return new MainPresenter();
}

export {main};
