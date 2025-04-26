/**
 * Logic for presenters handling simulation results display.
 * 
 * @license BSD-3-Clause
 */


/**
 * Presenter which manages the display of simulation results.
 */
class ResultsPresenter {

  /**
   * Creates a new results presenter.
   * 
   * @param {string} rootId - The ID of the element containing the results display.
   */
  constructor(rootId) {
    const self = this;

    self._root = document.getElementById(rootId);
    self._statusPresenter = new StatusPresenter(self._root.querySelector("#status-panel"));
    self._vizPresenter = new VizPresenter(self._root.querySelector("#viz-panel"));

    self._secondsOnStart = null;
  }

  /**
   * Indicate the start of a new simulation run.
   */
  onSimStart() {
    const self = this;
    self._statusPresenter.resetProgress();
    self._vizPresenter.hide();
    self._root.style.display = "block";
    self._secondsOnStart = self._getEpochSeconds();
  }

  /**
   * Updates progress when a simulation step completes.
   *
   * @param {Number} numSteps - The number of steps completed.
   * @param {string} units - The units (timesteps, replicates) being reported.
   */
  onStep(numSteps, units) {
    const self = this;
    self._statusPresenter.updateProgress(numSteps, units);
  }

  /**
   * Handles completion of simulation run, calculating and displaying results.
   * 
   * @param {Array<SimulationResult>} results - Array of simulation results containing output records.
   */
  onComplete(results) {
    const self = this;

    const totalSeconds = self._getEpochSeconds() - self._secondsOnStart;
    const numRecords = results.map((record) => {
      return [
        record.getSimResults().length,
        record.getPatchResults().length,
        record.getEntityResults().length
      ].reduce((a, b) => a + b);
    }).reduce((a, b) => a + b, 0);
    
    self._statusPresenter.showComplete(totalSeconds, numRecords);

    if (numRecords == 0) {
      self._vizPresenter.showNoData();
    } else {
      self._vizPresenter.show(results);
    }
  }

  /**
   * Displays an error message in the status display.
   * 
   * @param {string} message - The error message to display.
   */
  onError(message) {
    const self =this;
    self._statusPresenter.showError(message);
  }

  /**
   * Gets the current time in epoch seconds.
   * 
   * @returns {number} Current time in seconds since epoch.
   * @private
   */
  _getEpochSeconds() {
    const self = this;
    const now = new Date();
    return now.getTime() / 1000;
  }
}


/**
 * Presenter which handles the status display for simulation progress.
 */
class StatusPresenter {

  /**
   * Creates a new status presenter.
   * 
   * @param {Element} selection - The DOM element for displaying status.
   */
  constructor(selection) {
    const self = this;
    self._root = selection;
  }

  /**
   * Resets the progress counter and updates display.
   */
  resetProgress() {
    const self = this;
    self.updateProgress(0, "steps");
    self._root.querySelector(".running-icon").style.display = "inline-block";
    self._root.querySelector(".complete-icon").style.display = "none";
    self._root.querySelector(".error-display").style.display = "none";
    self._root.querySelectorAll(".finish-display").forEach((x) => x.style.display = "none");
  }

  /**
   * Increments the progress counter and updates display.
   *
   * @param {Number} numSteps - The number of steps completed.
   * @param {string} units - The units (timesteps, replicates) being reported.
   */
  updateProgress(numSteps, units) {
    const self = this;
    self._root.querySelector(".completed-count").innerHTML = numSteps;
    self._root.querySelector(".completed-type").innerHTML = units;
  }

  /**
   * Displays the completion status of the simulation.
   *
   * @param {number} totalSeconds - Total time taken by the simulation in seconds.
   * @param {number} numRecords - Total number of records processed during the simulation.
   */
  showComplete(totalSeconds, numRecords) {
    const self = this;
    
    self._root.querySelector(".running-icon").style.display = "none";
    self._root.querySelector(".complete-icon").style.display = "inline-block";
    self._root.querySelectorAll(".finish-display").forEach((x) => x.style.display = "block");

    const minutes = Math.floor(totalSeconds / 60);
    const seconds = Math.ceil(totalSeconds - 60 * minutes);
    self._root.querySelector(".completed-minutes").innerHTML = minutes;
    self._root.querySelector(".completed-seconds").innerHTML = seconds;
    self._root.querySelector(".completed-records").innerHTML = numRecords;
  }

  /**
   * Displays an error message encountered in runtime in the error display.
   *
   * @param {string} message - The error message to display in the status area.
   */
  showError(message) {
    const self = this;
    self._root.querySelector(".error-display").style.display = "block";
    
    const errorMessageHolder = self._root.querySelector(".error-message");
    errorMessageHolder.innerHTML = "";
    
    const textNode = document.createTextNode(message);
    errorMessageHolder.appendChild(textNode);
  }

}


/**
 * Presenter which runs the in-editor visualization panel.
 */
class VizPresenter {

  /**
   * Create a new visualization presenter.
   *
   * @param {Element} selection - Selection over the div containing the visualization.
   */
  constructor(selection) {
    const self = this;
    self._root = selection;
    self._dataSelector = new DataQuerySelector(self._root.querySelector("#data-selector"));
  }

  /**
   * Hide the visualization display.
   */
  hide() {
    const self = this;
    self._root.style.display = "none";
  }

  /**
   * Show the user a message indicating that no data were recieved.
   */
  showNoData() {
    const self = this;
    self._root.style.display = "block";
    self._root.querySelector("#no-data-message").style.display = "block";
    self._root.querySelector("#data-display").style.display = "none";
  }

  /**
   * Show the visualization results.
   *
   * @param {Array<SimulationResult>} results - The results to be displayed where each element is a
   *     replicate.
   */
  show(results) {
    const self = this;
    
    self._root.style.display = "block";
    self._root.querySelector("#no-data-message").style.display = "none";
    self._root.querySelector("#data-display").style.display = "block";

    const allVariables = new Set();
    results.forEach(replicate => {
      const variables = replicate.getPatchVariables();
      variables.forEach(variable => allVariables.add(variable));
    });

    self._dataSelector.setVariables(allVariables);
  }
  
}


class DataQuerySelector {

  constructor(selection) {
    const self = this;
    self._root = selection;
    self._addEventListeners();
    self._updateInternalDisplay();
  }

  setVariables(newVariables) {
    const self = this;

    const variableSelection = self._root.querySelector(".variable-select");
    
    const originalValue = variableSelection.value;
    variableSelection.innerHTML = ""; // Clear current options

    newVariables.forEach((variable) => {
      const option = document.createElement("option");
      option.text = variable;
      option.value = variable;
      variableSelection.add(option);
    });

    if (newVariables.has(originalValue)) {
      variableSelection.value = originalValue;
    }
  }

  _addEventListeners() {
    const self = this;
    self._root.querySelectorAll(".data-select-option").forEach(
      (elem) => elem.addEventListener("click", (event) => {
        event.preventDefault();
        self._updateInternalDisplay();
      })
    );
  }

  _updateInternalDisplay() {
    const self = this;
    
    const metricSelect = self._root.querySelector(".metric-select");
    const metric = metricSelect.value;
    const probabilityControls = self._root.querySelectorAll(".probability-controls");
    const regularControls = self._root.querySelectorAll(".regular-metric-controls");
    if (metric === "probability") {
      probabilityControls.forEach((x) => x.style.display = "inline-block");
      regularControls.forEach((x) => x.style.display = "none");
    } else {
      probabilityControls.forEach((x) => x.style.display = "none");
      regularControls.forEach((x) => x.style.display = "inline-block");
    }

    const probabilityType = self._root.querySelector(".probability-range-target").value;
    const secondTarget = self._root.querySelector(".target-b");
    if (probabilityType === "is between") {
      secondTarget.style.display = "inline-block";
    } else {
      secondTarget.style.display = "none";
    }
  }
  
}


export {ResultsPresenter};
