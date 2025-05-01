
/**
 * Logic for presenters handling simulation run controls.
 * 
 * @license BSD-3-Clause
 */

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
    self._runLocalDialog = self._root.querySelector("#run-local-dialog");

    self._localPanel = self._runLocalDialog.querySelector("#local-run-instructions");
    self._joshCloudPanel = self._runLocalDialog.querySelector("#josh-run-settings");
    self._customCloudPanel = self._runLocalDialog.querySelector("#custom-cloud-run-settings");

    self._browserRadio = self._runLocalDialog.querySelector("#engine-browser");
    self._localRadio = self._runLocalDialog.querySelector("#engine-computer");
    self._joshCloudRadio = self._runLocalDialog.querySelector("#engine-josh-cloud");
    self._customCloudRadio = self._runLocalDialog.querySelector("#engine-your-cloud");

    self._notFoundMessage = self._runLocalDialog.querySelector("#app-not-found-message");
    self._foundMessage = self._runLocalDialog.querySelector("#app-found-message");

    tippy("[data-tippy-content]", { appendTo: self._runLocalDialog });

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
      self._runLocalDialog.showModal();

      const simSelect = self._runLocalDialog.querySelector(".simulation-select");
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
    
    self._runLocalDialog.querySelector(".cancel-button").addEventListener("click", (event) => {
      event.preventDefault();
      self._runLocalDialog.close();
    });

    self._runLocalDialog.querySelector(".run-button").addEventListener("click", (event) => {
      event.preventDefault();

      const simulationName = self._runLocalDialog.querySelector(".simulation-select").value;

      const replicatesStr = self._runLocalDialog.querySelector(".replicates-input").value;
      const replicates = parseInt(replicatesStr);
      if (isNaN(replicates) || replicates <= 0) {
        alert("Replicates must be a positive number.");
        return;
      }

      const runRequest = new RunRequest(simulationName, replicates);
      self._runLocalDialog.close();
      self._onRun(runRequest);
    });
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

/**
 * Record for a simulation run request.
 */
class RunRequest {

  /**
   * Creates a new run request.
   * 
   * @param {string} simName - Name of the simulation to run.
   * @param {number} replicates - Number of times to replicate the simulation.
   */
  constructor(simName, replicates) {
    const self = this;
    self._simName = simName;
    self._replicates = replicates;
  }

  /**
   * Gets the simulation name.
   * 
   * @returns {string} The simulation name.
   */
  getSimName() {
    const self = this;
    return self._simName;
  }

  /**
   * Gets the number of replicates.
   * 
   * @returns {number} The number of replicates.
   */
  getReplicates() {
    const self = this;
    return self._replicates;
  }
}

export {RunPanelPresenter};
