/**
 * Logic for presenters handling simulation results display.
 * 
 * @license BSD-3-Clause
 */

import {BasemapDialogPresenter} from "baselayer";
import {ExportPresenter} from "exporter";
import {DataQuery, summarizeDatasets} from "summarize";
import {GridPresenter, ScrubPresenter, MapConfigPresenter} from "viz";


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
      self._root.querySelector("#viz-panel"),
      () => self._renderDisplay(self._metadata)
    );
    self._exportPresenter = new ExportPresenter(
      self._root.querySelector("#export-button"),
      self._root.querySelector("#download-dialog")
    );
    self._baselayerDialogPresenter = new BasemapDialogPresenter(
      self._root.querySelector("#map-button"),
      self._root.querySelector("#map-dialog"),
      (url) => self._onBasemapChange(url)
    );

    self._results = null;
    self._metadata = null;
    self._secondsOnStart = null;
    self._basemapUrl = null;
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
   * @param {SimulationMetadata} metadata - The metadata of the simulation being displayed.
   * @param {Array<SimulationResult>} results - Array of simulation results containing output
   *     records.
   */
  onComplete(metadata, results) {
    const self = this;
    self._metadata = metadata;
    self._results = results;
    self._updateStatus();
    self._updateVariables();
    self._renderDisplay(metadata);
    self._exportPresenter.setDataset(metadata, results);
    self._baselayerDialogPresenter.setMetadata(metadata);
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
   * Build a query from the user's current query selection.
   *
   * @returns {DataQuery} Record describing the user's current query selection.
   */
  getCurrentQuerySelection() {
    const self = this;
    return self._resultsDisplayPresenter.getCurrentQuerySelection();
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

  /**
   * Re-render the internal display showing the results.
   *
   * @param {SimulationMetadata} metadata - Metadata of the simulation being displayed.
   */
  _renderDisplay(metadata) {
    const self = this;

    if (self._results === null) {
      throw "No results available to display.";
    }

    self._metadata = metadata;
    self._refreshDisplay();
  }

  /**
   * Update the status display at the top of the results panel.
   */
  _updateStatus() {
    const self = this;
    
    const totalSeconds = self._getEpochSeconds() - self._secondsOnStart;
    const numRecords = self._results.map((record) => {
      return [
        record.getSimResults().length,
        record.getPatchResults().length,
        record.getEntityResults().length
      ].reduce((a, b) => a + b);
    }).reduce((a, b) => a + b, 0);

    self._statusPresenter.showComplete(totalSeconds, numRecords);

    if (numRecords == 0) {
      self._resultsDisplayPresenter.indiciateNoData();
    } else {
      self._resultsDisplayPresenter.indicateDataPresent();
    }
  }

  /**
   * Update the available patch variables in the results display presenter.
   */
  _updateVariables() {
    const self = this;

    const allVariables = new Set();
    self._results.forEach(replicate => {
      const variables = replicate.getPatchVariables();
      variables.forEach(variable => allVariables.add(variable));
    });

    self._resultsDisplayPresenter.setVariables(allVariables);
  }

  /**
   * Callback for when the basemap URL is updated.
   *
   * @param {?string} basemapUrl - URL at which the basemap image can be found or null if no
   *     basemap image should be provided.
   */
  _onBasemapChange(basemapUrl) {
    const self = this;
    self._basemapUrl = basemapUrl;
    self._refreshDisplay();
  }

  /**
   * Refresh the components within this display using last known values.
   */
  _refreshDisplay() {
    const self = this;
    const query = self._resultsDisplayPresenter.getCurrentQuerySelection();
    const summarized = summarizeDatasets(self._results, query);
    self._resultsDisplayPresenter.render(self._metadata, summarized, self._basemapUrl);
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
    self._progressBar = self._root.querySelector(".step-progress-bar");
  }

  /**
   * Resets the progress counter and updates display.
   */
  resetProgress() {
    const self = this;
    self._root.querySelector(".status-text").innerHTML = "Running...";
    self._root.querySelector(".running-icon").style.display = "inline-block";
    self._root.querySelector(".complete-icon").style.display = "none";
    self._root.querySelector(".error-display").style.display = "none";
    self._root.querySelectorAll(".finish-display").forEach((x) => x.style.display = "none");
    self._resetProgressBar();
  }

  /**
   * Increments the progress counter and updates display.
   *
   * @param {Number} numSteps - The number of steps completed.
   * @param {string} units - The units (timesteps, replicates) being reported.
   */
  updateProgress(numSteps, units) {
    const self = this;
    const completedStr = `Completed ${numSteps} ${units}...`;
    self._root.querySelector(".status-text").innerHTML = completedStr;
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
    
    self.hideProgressBar();
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

  /**
   * Resets the progress bar to 0% and makes it visible.
   */
  _resetProgressBar() {
    const self = this;
    self._progressBar.value = 0;
    self._progressBar.style.display = "block";
  }

  /**
   * Updates the progress bar with the current progress percentage.
   *
   * @param {number} completedSteps - The number of steps completed across all replicates.
   * @param {number} totalSteps - The total number of steps across all replicates.
   */
  updateProgressBar(completedSteps, totalSteps) {
    const self = this;
    const percentComplete = (completedSteps / totalSteps) * 100;
    const percentageBounded = Math.max(Math.min(100, percentComplete), 0);
    self._progressBar.value = percentageBounded;
  }

  /**
   * Hides the progress bar when simulation is complete.
   */
  hideProgressBar() {
    const self = this;
    self._progressBar.style.display = "none";
  }

}


/**
 * Presenter which runs the in-editor visualization panel.
 */
class ResultsDisplayPresenter {

  /**
   * Create a new visualization presenter.
   *
   * @param {Element} selection - Selection over the div containing the visualization.
   * @param {function} callback - Callback to invoke when the user's requested data selection
   *     changes.
   */
  constructor(selection, callback) {
    const self = this;
    self._root = selection;
    self._currentTimestep = null;
    self._metadata = null;
    self._summary = null;
    
    self._dataSelector = new DataQuerySelector(
      self._root.querySelector("#data-selector"),
      () => callback()
    );
    self._scrubPresenter = new ScrubPresenter(
      self._root.querySelector("#scrub-viz-holder"),
      (step) => self._onStepSelected(step)
    );
    self._gridPresenter = new GridPresenter(
      self._root.querySelector("#grid-viz-holder")
    );
    self._mapConfigPresenter = new MapConfigPresenter(
      self._root.querySelector("#map-config"),
      (dimensions) => self._onMapResize(dimensions)
    );
  }

  /**
   * Hide the visualization display.
   */
  hide() {
    const self = this;
    self._root.style.display = "none";
    self._currentTimestep = null;
    self._metadata = null;
  }

  /**
   * Show the user a message indicating that no data were recieved.
   */
  indiciateNoData() {
    const self = this;
    self._root.style.display = "block";
    self._root.querySelector("#no-data-message").style.display = "block";
    self._root.querySelector("#data-display").style.display = "none";
  }
  
  /**
   * Indicate to the user that data are available to visualize / review.
   */
  indicateDataPresent() {
    const self = this;
    self._root.style.display = "block";
    self._root.querySelector("#no-data-message").style.display = "none";
    self._root.querySelector("#data-display").style.display = "block";
  }

  /**
   * Indicate which variables are available in the dataset.
   *
   * @param {Set<string>} allVariables - Set of all variables available in the dataset.
   */
  setVariables(allVariables) {
    const self = this;
    self._dataSelector.setVariables(allVariables);
  }

  /**
   * Build a query from the user's current query selection.
   *
   * @returns {DataQuery} Record describing the user's current query selection.
   */
  getCurrentQuerySelection() {
    const self = this;
    return self._dataSelector.getCurrentSelection();
  }

  /**
   * Instruct the visualizations to display a summary.
   *
   * Instruct the visualizations to display a summary which was computed from the underlying raw
   * data using user defined parameters.
   *
   * @param {SimulationMetadata} metadata - Metadata about the simulation to be displayed. 
   * @param {SummarizedResult} summary - The data summarized according to user instructions.
   * @param {?string} basemapUrl - URL at which the basemap image can be found or null if no
   *     basemap.
   */
  render(metadata, summary, basemapUrl) {
    const self = this;
    self._metadata = metadata;
    self._summary = summary;
    self._basemapUrl = basemapUrl;
    if (self._currentTimestep === null) {
      self._currentTimestep = summary.getMaxTimestep();
    }
    
    const defaultDimensions = self._gridPresenter.calculateDefaultDimensions(metadata);
    self._mapConfigPresenter.setDefaultDimensions(defaultDimensions);
    self._renderInternal(true);
  }

  /**
   * Callback when a step is selected.
   *
   * @param {number} step - The timestep selected.
   */
  _onStepSelected(step) {
    const self = this;
    self._currentTimestep = step;
    self._renderInternal(false);
  }

  /**
   * Callback when map dimensions are changed.
   *
   * @param {MapDimensions} dimensions - Map dimensions object with width and height in pixels.
   */
  _onMapResize(dimensions) {
    const self = this;
    self._gridPresenter.setCustomDimensions(dimensions);
    self._renderInternal(false);
  }

  /**
   * Update the components in this display using last known values.
   *
   * @param {boolean} timestepChangedExternally - True if the timestep was changed from outside the
   *     UI widgets and false if changed within the UI widgets or unchanged.
   */
  _renderInternal(timestepChangedExternally) {
    const self = this;

    if (timestepChangedExternally) {
      self._scrubPresenter.render(self._summary);
    }
    
    self._gridPresenter.render(
      self._metadata,
      self._summary,
      self._currentTimestep,
      self._basemapUrl
    );
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
    const isProbability = metric === "probability";
    const metricType = isProbability ? self._probabilityTypeSelect.value : null;
    const variable = self._variableSelect.value;
    
    let targetA = null;
    let targetB = null;
    
    if (isProbability) {
      targetA = parseFloat(self._probabilityTargetA.value);
      if (self._probabilityTypeSelect.value === "is between") {
        targetB = parseFloat(self._probabilityTargetB.value);
      }
    }
    
    return new DataQuery(variable, metric, metricType, targetA, targetB);
  }

  /**
   * Adds event listeners to update visible elements and fire a callback on selection change.
   */
  _addEventListeners() {
    const self = this;
    self._root.querySelectorAll(".data-select-option").forEach(
      (elem) => {
        elem.addEventListener("change", (event) => {
          event.preventDefault();
          self._updateInternalDisplay();
          self._callback();
        });
        elem.addEventListener("keyup", (event) => {
          self._updateInternalDisplay();
          self._callback();
        });
      }
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

    const probabilityType = self._probabilityTypeSelect.value;
    if (probabilityType === "is between") {
      self._probabilityTargetBSpan.style.display = "inline-block";
    } else {
      self._probabilityTargetBSpan.style.display = "none";
    }
  }
  
}


export {ResultsPresenter};
