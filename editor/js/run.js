
/**
 * Logic for presenters handling simulation run controls.
 * 
 * @license BSD-3-Clause
 */

import {RunRequest} from "engine";

const DEFAULT_ENDPOINT = "https://josh-executor-prod-1007495489273.us-west1.run.app";


/**
 * Presenter which manages the run control panel and dialogs.
 */
class RunPanelPresenter {

  /**
   * Creates a new run panel presenter.
   * 
   * @param {string} rootId - The ID of the element containing the run controls.
   * @param {function} getSimulations - Function that returns available simulations.
   * @param {function} onRun - Callback function when run is initiated.
   */
  constructor(rootId, getSimulations, onRun) {
    const self = this;
    self._root = document.getElementById(rootId);
    self._getSimulations = getSimulations;
    self._onRun = onRun;

    self._availablePanel = self._root.querySelector("#available-panel");
    self._runDialog = self._root.querySelector("#run-dialog");

    self._localPanel = self._runDialog.querySelector("#local-run-instructions");
    self._joshCloudPanel = self._runDialog.querySelector("#josh-run-settings");
    self._customCloudPanel = self._runDialog.querySelector("#custom-cloud-run-settings");

    self._browserRadio = self._runDialog.querySelector("#engine-browser");
    self._localRadio = self._runDialog.querySelector("#engine-computer");
    self._joshCloudRadio = self._runDialog.querySelector("#engine-josh-cloud");
    self._customCloudRadio = self._runDialog.querySelector("#engine-your-cloud");

    self._notFoundMessage = self._runDialog.querySelector("#app-not-found-message");
    self._foundMessage = self._runDialog.querySelector("#app-found-message");

    tippy("[data-tippy-content]", { appendTo: self._runDialog });

    self._setupDialog();
    self._updateVisibility();
  }

  /**
   * Shows the run control buttons.
   */
  showButtons() {
    const self = this;
    self._availablePanel.style.display = "block";
  }

  /**
   * Hides the run control buttons.
   */
  hideButtons() {
    const self = this;
    self._availablePanel.style.display = "none";
  }

  /**
   * Sets up the run dialog event handlers.
   * 
   * @private
   */
  _setupDialog() {
    const self = this;

    self._root.querySelector("#open-run-dialog-button").addEventListener("click", (event) => {
      event.preventDefault();
      self._runDialog.showModal();

      const simSelect = self._runDialog.querySelector(".simulation-select");
      simSelect.innerHTML = "";
      self._getSimulations().then((simulations) => {
        simulations.forEach((simulation) => {
          const option = document.createElement("option");
          option.text = simulation;
          option.value = simulation;
          simSelect.appendChild(option);
        });
      });

      [
        self._browserRadio,
        self._localRadio,
        self._joshCloudRadio,
        self._customCloudRadio
      ].forEach((x) => x.addEventListener("click", () => self._updateVisibility()));

      self._detectLocalApp();
    });
    
    self._runDialog.querySelector(".cancel-button").addEventListener("click", (event) => {
      event.preventDefault();
      self._runDialog.close();
    });

    self._runDialog.querySelector(".run-button").addEventListener("click", (event) => {
      event.preventDefault();

      const simulationName = self._runDialog.querySelector(".simulation-select").value;

      const replicatesStr = self._runDialog.querySelector(".replicates-input").value;
      const replicates = parseInt(replicatesStr);
      if (isNaN(replicates) || replicates <= 0) {
        alert("Replicates must be a positive number.");
        return;
      }

      const precisionStr = self._runDialog.querySelector(".precision-input").value;
      const preferBigDecimal = precisionStr === "high";

      const outputStepsValue = self._runDialog.querySelector(".output-steps-input").value.trim();

      // Add validation
      if (outputStepsValue && !outputStepsValue.match(/^(\d+,)*\d+$/)) {
        alert("Output steps must be comma-separated numbers (e.g., 5,7,8,9,20)");
        return;
      }

      const useServer = self._browserRadio.checked ? false : true;
      const apiKey = self._getApiKey();
      const endpoint = self._determineEndpoint();

      const runRequest = new RunRequest(
        simulationName,
        replicates,
        useServer,
        apiKey,
        endpoint,
        preferBigDecimal,
        outputStepsValue
      );
      
      self._runDialog.close();
      self._onRun(runRequest);
    });
  }

  /**
   * Determine the endpoint where the simulation should run.
   * 
   * @returns {string} The URL at which the simulation should run.
   */
  _determineEndpoint() {
    const self = this;
    if (self._customCloudRadio.checked) {
      return self._runDialog.querySelector("#your-cloud-endpoint").value;
    } else if (self._joshCloudRadio.checked) {
      return DEFAULT_ENDPOINT;
    } else {
      return "";
    }
  }

  /**
   * Get the effective API key that should be used.
   *
   * @returns {string} API key to use in communication with a server for executing simulations.
   */
  _getApiKey() {
    const self = this;
    if (self._joshCloudRadio.checked) {
      return self._runDialog.querySelector("#josh-cloud-api-key").value;
    } else if (self._customCloudRadio.checked) {
      return self._runDialog.querySelector("#your-cloud-api-key").value;
    } else {
      return "";
    }
  }
  
  /**
   * Updates the visibility of different panels based on the radio button selection.
   */
  _updateVisibility() {
    const self = this;

    const updateVisibilityComponent = (panel, radio) => {
      panel.style.display = radio.checked ? "block" : "none";
    };
    
    updateVisibilityComponent(self._localPanel, self._localRadio);
    updateVisibilityComponent(self._joshCloudPanel, self._joshCloudRadio);
    updateVisibilityComponent(self._customCloudPanel, self._customCloudRadio);
  }

  /**
   * Attempt to determine if a Josh server is running at this location.
   *
   * Make a request to /health and, if it gives back a 200, show the found message. Otherwise, show
   * the not found message indicating that a Josh server could not be found.
   */
  _detectLocalApp() {
    const self = this;
    fetch("/health")
      .then(response => {
        if (response.status === 200) {
          self._notFoundMessage.style.display = "none";
          self._foundMessage.style.display = "block";
        } else {
          self._notFoundMessage.style.display = "block";
          self._foundMessage.style.display = "none";
        }
      })
      .catch(() => {
        self._notFoundMessage.style.display = "block";
        self._foundMessage.style.display = "none";
      });
  }
}

export {RunPanelPresenter};
