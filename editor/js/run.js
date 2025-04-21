
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

    self._setupDialog();
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
      
      const simulations = self._getSimulations();
      const simSelect = self._runLocalDialog.querySelector(".simulation-select");

      simSelect.innerHTML = "";
      simulations.forEach((simulation) => {
        const option = document.createElement("option");
        option.text = simulation;
        option.value = simulation;
        simSelect.appendChild(option);
      });
    })
    
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
