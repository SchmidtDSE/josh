
/**
 * Main entry point for the Josh web editor application.
 * 
 * Initializes and coordinates the different components of the editor including file handling, code
 * editing, run panel, and results display.
 * 
 * @license BSD-3-Clause
 */

import {AiAssistantDialogPresenter} from "ai";
import {ConfigDialogPresenter} from "config";
import {DataFilesPresenter, LocalFileLayer} from "data";
import {EditorPresenter} from "editor";
import {RemoteEngineBackend, WasmEngineBackend} from "engine";
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
   */
  constructor(initalizationCallback) {
    const self = this;

    self._currentRequest = null;
    self._replicatesCompleted = 0;
    self._replicateResults = [];
    self._metadata = null;
    self._totalStepsAcrossReplicates = 0;
    self._completedStepsAcrossReplicates = 0;
    self._startStep = 0;

    self._wasmLayer = new WasmLayer();

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

    // Create a shared LocalFileLayer instance for both data and config dialogs
    const sharedFileLayer = new LocalFileLayer();

    self._dataPresenter = new DataFilesPresenter(
      "open-data-dialog-button",
      "data-dialog",
      sharedFileLayer
    );

    self._configPresenter = new ConfigDialogPresenter(
      "open-config-dialog-button",
      "config-dialog",
      sharedFileLayer
    );

    self._aiAssistantPresenter = new AiAssistantDialogPresenter(
      "open-ai-assistant-dialog-button",
      "ai-assistant-dialog"
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
    self._completedStepsAcrossReplicates = 0;

    const simCode = self._editorPresenter.getCode();
    const simName = self._currentRequest.getSimName();

    const futureExternalData = self._dataPresenter.getFilesAsJson();
    const futureMetadata = self._wasmLayer.getSimulationMetadata(simCode, simName);

    Promise.all([futureMetadata, futureExternalData]).then(
      (results) => {
        self._metadata = results[0];

        // Calculate total steps across all replicates
        const totalStepsPerReplicate = self._metadata.getTotalSteps() || 0;
        const numReplicates = self._currentRequest.getReplicates();
        self._totalStepsAcrossReplicates = totalStepsPerReplicate * numReplicates;

        // Store startStep for progress normalization (default to 0 if not specified)
        self._startStep = self._metadata.getStartStep() || 0;

        self._executeInBackend(results[1]);
      },
      (x) => {
        self._onError(x);
      }
    );
  }

  /**
   * Execute the simulation code in the currently configured engine backend.
   *
   * @param {Object} externalData - Data to load as avilable external resources which can be
   *     serialized to JSON.
   */
  _executeInBackend(externalData) {
    const self = this;
    const engineBackend = self._buildEngineBackend();
    self._currentEngineBackend = engineBackend; // Store for type detection
    engineBackend.execute(
      self._editorPresenter.getCode(),
      self._currentRequest,
      externalData,
      (x) => self._onStepCompleted(x, "steps"),
      (x) => self._onStepCompleted(x, "replicates"),
      self._startStep
    ).then(
      (x) => self._onRunComplete(x),
      (x) => self._onError(x)
    );
  }

  /**
   * Build the backend given the current request.
   */
  _buildEngineBackend() {
    const self = this;

    if (self._currentRequest === null) {
      throw "No request active. Cannot build backend";
    }
    
    if (self._currentRequest.useServer()) {
      return new RemoteEngineBackend(
        self._currentRequest.getEndpoint(),
        self._currentRequest.getApiKey()
      );
    } else {
      return new WasmEngineBackend(self._wasmLayer);
    }
  }
  
  /**
   * Callback for when a simulation run is completed.
   *
   * @param {Array<SimulationResult>} results - The completed simulation results.
   */
  _onRunComplete(results) {
    const self = this;
    self._replicateResults = results;
    self._runPresenter.showButtons();
    self._resultsPresenter.onComplete(self._metadata, self._replicateResults);
  }

  /**
   * Callback for when a simulation step is completed.
   *
   * @param {number} numCompleted - The number of items completed.
   * @param {string} typeCompleted - The type of items being reported like steps or replicates.
   */
  _onStepCompleted(numCompleted, typeCompleted) {
    const self = this;
    
    if (typeCompleted === "steps") {
      // For step-level progress, numCompleted is the cumulative steps across replicates
      self._completedStepsAcrossReplicates = numCompleted;
      
      // Update progress bar only for step events
      self._resultsPresenter._statusPresenter.updateProgressBar(
        self._completedStepsAcrossReplicates, 
        self._totalStepsAcrossReplicates
      );
    } else if (typeCompleted === "replicates") {
      // For replicate-level progress, calculate cumulative steps
      const totalStepsPerReplicate = self._metadata ? self._metadata.getTotalSteps() || 0 : 0;
      self._completedStepsAcrossReplicates = numCompleted * totalStepsPerReplicate;
      // Don't update progress bar here to avoid wiggling
    }
    
    // Conditionally update status text
    if (self._shouldUpdateStatusText(typeCompleted)) {
      self._resultsPresenter.onStep(numCompleted, typeCompleted);
    }
  }

  /**
   * Determine if status text should be updated based on backend type and replicate count.
   *
   * @param {string} typeCompleted - The type of items being reported like steps or replicates.
   * @returns {boolean} True if status text should be updated.
   */
  _shouldUpdateStatusText(typeCompleted) {
    const self = this;
    const hasMultipleReplicates = self._currentRequest && self._currentRequest.getReplicates() > 1;

    if (hasMultipleReplicates) {
      // Multiple replicates: only update status text on "replicates" events
      return typeCompleted === "replicates";
    } else {
      // Single replicate: always update status text on "steps" events
      return typeCompleted === "steps";
    }
  }

  /**
   * Handles errors that occur during simulation run requests.
   *
   * @param {string} message - The error message to be displayed.
   */
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
