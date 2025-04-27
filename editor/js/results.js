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
    self._resultsDisplayPresenter = new ResultsDisplayPresenter(
      self._root.querySelector("#viz-panel")
    );

    self._secondsOnStart = null;
  }

  /**
   * Indicate the start of a new simulation run.
   */
  onSimStart() {
    const self = this;
    self._statusPresenter.resetProgress();
    self._resultsDisplayPresenter.hide();
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
      self._resultsDisplayPresenter.showNoData();
    } else {
      self._resultsDisplayPresenter.show(results);
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
class ReslutsDisplayPresenter {

  /**
   * Create a new visualization presenter.
   *
   * @param {Element} selection - Selection over the div containing the visualization.
   */
  constructor(selection) {
    const self = this;
    self._root = selection;
    self._dataSelector = new DataQuerySelector(
      self._root.querySelector("#data-selector"),
      
    );
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


/**
 * Presenter that handles the data query selector dropdown menus and inputs.
 */
class DataQuerySelector {

  /**
   * Create a new presenter for the data query selector dropdown menus and inputs.
   *
   * @param {Element} selection - The root element containing all of the data query selector
   *     elements.
   * @param {function} callaback - Function to call with a DataQuery when the user changes the
   *     selection.
   */
  constructor(selection, callback) {
    const self = this;
    
    self._root = selection;
    self._callback = callback;

    self._metricSelect = self._root.querySelector(".metric-select");
    self._probabilityControls = self._root.querySelectorAll(".probability-controls");
    self._regularControls = self._root.querySelectorAll(".regular-metric-controls");
    self._variableSelect = self._root.querySelector(".variable-select");
    self._probabilityTypeSelect = self._root.querySelector(".probability-range-target");
    self._probabilityTargetASpan = self._root.querySelector(".target-a");
    self._probabilityTargetA = self._probabilityTargetASpan.querySelector(".target-a-input");
    self._probabilityTargetBSpan = self._root.querySelector(".target-b");
    self._probabilityTargetB = self._probabilityTargetBSpan.querySelector(".target-a-input");
    
    self._addEventListeners();
    self._updateInternalDisplay();
  }

  /**
   * Set available variables in the selector.
   *
   * @param {Set<string>} newVariables - A set of new variable names to populate the selector.
   */
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

  /**
   * Read the current state of the elements within this selector.
   *
   * @returns {DataQuery} Record describing the current selection made by the user within this
   *     widget.
   */
  getCurrentSelection() {
    const self = this;
    
    const metric = self._metricSelect.value;
    const variable = self._variableSelect.value;
    
    let targetA = null;
    let targetB = null;
    
    if (metric === "probability") {
      targetA = parseFloat(self._probabilityTargetA.value);
      if (self._probabilityTypeSelect.value === "is between") {
        targetB = parseFloat(self._probabilityTargetB.value);
      }
    }
    
    return new DataQuery(variable, metric, targetA, targetB);
  }

  /**
   * Adds event listeners to update visible elements and fire a callback on selection change.
   */
  _addEventListeners() {
    const self = this;
    self._root.querySelectorAll(".data-select-option").forEach(
      (elem) => elem.addEventListener("click", (event) => {
        event.preventDefault();
        self._updateInternalDisplay();
        self._callaback(self.getCurrentSelection());
      })
    );
  }
  
  /**
   * Updates the internal display elements based on selected metrics.
   */
  _updateInternalDisplay() {
    const self = this;
    
    const metric = self._metricSelect.value;
    if (metric === "probability") {
      self._probabilityControls.forEach((x) => x.style.display = "inline-block");
      self._regularControls.forEach((x) => x.style.display = "none");
    } else {
      self._probabilityControls.forEach((x) => x.style.display = "none");
        self._regularControls.forEach((x) => x.style.display = "inline-block");
    }

    const probabilityType = self._probablityTypeSelect.value;
    if (probabilityType === "is between") {
      self._probabilityTargetBSpan.style.display = "inline-block";
    } else {
      self._probabilityTargetBSpan.style.display = "none";
    }
  }
  
}


/**
 * Record describing which variable the user wants to analyze and how.
 *
 * Record describing which variable exported from the script that the user wants to analyze and
 * indicate how those values should be reated (mean, median, etc). If the user is calculating
 * probabilities, this will also have one or two target values.
 */
class DataQuery {

  /**
   * Create a new record of a user-requested DataQuery.
   *
   * @param {string} variable The name of the variable as exported from the user's script to be
   *     analyzed.
   * @param {string} metric The kind of metric to be calculated like mean. This will be applied both
   *     at the simulation level (like mean across all patches across all timesteps) for the scrub
   *     element or similar and patch level (like mean for each patch across all timesteps).
   * @param {?number} targetA The first reference value to use for probability metrics like the
   *     minimum threshold for proability of exceeds, maximum for probablity below, and minimum
   *     for probability within range. Should be null if not a probability (value ignored).
   * @param {?number} targetB The second reference value to use for probability metrics like the
   *     maximum for probability within range. Should be null if not a probability within range.
   */
  constructor(variable, metric, targetA, targetB) {
    const self = this;
    self._variable = variable;
    self._metric = metric;
    self._targetA = targetA;
    self._targetB = targetB;
  }
  
  /**
   * Get the variable name being analyzed.
   * 
   * @returns {string} The variable name.
   */
  getVariable() {
    const self = this;
    return self._variable;
  }

  /**
   * Get the metric type being calculated.
   * 
   * @returns {string} The metric type.
   */
  getMetric() {
    const self = this;
    return self._metric;
  }

  /**
   * Get the first target value for probability metrics.
   * 
   * @returns {?number} The first target value or null.
   */
  getTargetA() {
    const self = this;
    return self._targetA;
  }

  /**
   * Get the second target value for probability metrics.
   * 
   * @returns {?number} The second target value or null.
   */
  getTargetB() {
    const self = this;
    return self._targetB;
  }
  
}


export {ResultsPresenter};
