/**
 * Logic for presenters handling simulation results display.
 *
 * @license BSD-3-Clause
 */

import {BasemapDialogPresenter} from "baselayer";
import {ExportPresenter} from "exporter";
import {DebugMessage, DebugMessageStore} from "model";
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
    self._debugPresenter = new DebugPresenter(
      self._root.querySelector("#debug-panel")
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
    self._debugPresenter.clear();
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
   * @param {DebugMessageStore} debugStore - Optional store containing debug messages.
   */
  onComplete(metadata, results, debugStore) {
    const self = this;
    self._metadata = metadata;
    self._results = results;
    self._updateStatus();
    self._updateVariables();
    self._renderDisplay(metadata);
    self._exportPresenter.setDataset(metadata, results);
    self._baselayerDialogPresenter.setMetadata(metadata);
    if (debugStore) {
      self._debugPresenter.setDebugStore(debugStore);
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


/**
 * Presenter for the debug output panel with filtering capabilities.
 */
class DebugPresenter {

  /**
   * Creates a new debug presenter.
   *
   * @param {Element} panelElement - The debug panel DOM element.
   */
  constructor(panelElement) {
    const self = this;
    self._panel = panelElement;
    self._debugStore = null;
    self._entityIds = [];
    self._currentEntityIndex = -1; // -1 means "All"

    self._locationFilter = self._panel.querySelector("#debug-location-filter");
    self._stepFilter = self._panel.querySelector("#debug-step-filter");
    self._entityDisplay = self._panel.querySelector("#debug-entity-display");
    self._entityNextBtn = self._panel.querySelector("#debug-entity-next");
    self._entityResetBtn = self._panel.querySelector("#debug-entity-reset");
    self._typeFilter = self._panel.querySelector("#debug-type-filter");
    self._filteredCount = self._panel.querySelector("#debug-filtered-count");
    self._totalCount = self._panel.querySelector("#debug-total-count");
    self._messagesList = self._panel.querySelector("#debug-messages-list");

    self._addEventListeners();
  }

  /**
   * Sets the debug message store and updates the display.
   *
   * @param {DebugMessageStore} debugStore - The store containing debug messages.
   */
  setDebugStore(debugStore) {
    const self = this;
    self._debugStore = debugStore;

    if (debugStore && debugStore.getAll().length > 0) {
      self._panel.classList.add("visible");
      self._populateFilters();
      self._renderMessages();
    } else {
      self._panel.classList.remove("visible");
    }
  }

  /**
   * Clears the debug display and hides the panel.
   */
  clear() {
    const self = this;
    self._debugStore = null;
    self._entityIds = [];
    self._currentEntityIndex = -1;
    self._panel.classList.remove("visible");
    self._messagesList.innerHTML = "";
    self._resetFilters();
  }

  /**
   * Adds event listeners to filter controls.
   */
  _addEventListeners() {
    const self = this;

    // When other filters change, rebuild the filtered entity list
    self._locationFilter.addEventListener("change", () => self._onFilterChange());
    self._stepFilter.addEventListener("change", () => self._onFilterChange());
    self._typeFilter.addEventListener("change", () => self._onFilterChange());

    self._entityNextBtn.addEventListener("click", () => self._nextEntity());
    self._entityResetBtn.addEventListener("click", () => self._resetEntity());
  }

  /**
   * Handles changes to location, step, or type filters.
   * Rebuilds the filtered entity list and resets entity selection.
   */
  _onFilterChange() {
    const self = this;
    self._rebuildFilteredEntityIds();
    self._currentEntityIndex = -1;
    self._updateEntityDisplay();
    self._renderMessages();
  }

  /**
   * Rebuilds the list of entity IDs that match current filters (excluding entity filter).
   */
  _rebuildFilteredEntityIds() {
    const self = this;
    if (!self._debugStore) {
      self._entityIds = [];
      return;
    }

    const location = self._locationFilter.value || null;
    const step = self._stepFilter.value ? parseInt(self._stepFilter.value) : null;
    const entityType = self._typeFilter.value || null;

    // Get messages matching current filters (without entity filter)
    const filtered = self._debugStore.filter(location, step, null, entityType);

    // Extract unique entity IDs from filtered messages
    const entityIdSet = new Set();
    filtered.forEach(msg => entityIdSet.add(msg.getEntityId()));
    self._entityIds = Array.from(entityIdSet).sort();
  }

  /**
   * Advances to the next entity ID.
   */
  _nextEntity() {
    const self = this;
    if (self._entityIds.length === 0) return;

    self._currentEntityIndex++;
    if (self._currentEntityIndex >= self._entityIds.length) {
      self._currentEntityIndex = 0;
    }

    self._updateEntityDisplay();
    self._renderMessages();
  }

  /**
   * Resets entity filter to show all entities.
   */
  _resetEntity() {
    const self = this;
    self._currentEntityIndex = -1;
    self._updateEntityDisplay();
    self._renderMessages();
  }

  /**
   * Updates the entity display text.
   */
  _updateEntityDisplay() {
    const self = this;
    if (self._currentEntityIndex < 0 || self._entityIds.length === 0) {
      self._entityDisplay.textContent = "All";
    } else {
      const id = self._entityIds[self._currentEntityIndex];
      const num = self._currentEntityIndex + 1;
      const total = self._entityIds.length;
      self._entityDisplay.textContent = `${num}/${total}`;
    }
  }

  /**
   * Resets all filters to their default state.
   */
  _resetFilters() {
    const self = this;
    self._locationFilter.innerHTML = '<option value="">All locations</option>';
    self._stepFilter.innerHTML = '<option value="">All steps</option>';
    self._currentEntityIndex = -1;
    self._updateEntityDisplay();
    self._typeFilter.value = "";
  }

  /**
   * Populates filter controls based on available data in the debug store.
   */
  _populateFilters() {
    const self = this;
    if (!self._debugStore) return;

    // Populate locations
    self._locationFilter.innerHTML = '<option value="">All locations</option>';
    const locations = self._debugStore.getLocations();
    locations.forEach(loc => {
      const option = document.createElement("option");
      option.value = loc;
      option.text = loc;
      self._locationFilter.appendChild(option);
    });

    // Populate steps
    self._stepFilter.innerHTML = '<option value="">All steps</option>';
    const steps = self._debugStore.getSteps();
    steps.sort((a, b) => a - b);
    steps.forEach(step => {
      const option = document.createElement("option");
      option.value = step;
      option.text = `Step ${step}`;
      self._stepFilter.appendChild(option);
    });

    // Build filtered entity IDs for cycling
    self._rebuildFilteredEntityIds();
    self._currentEntityIndex = -1;
    self._updateEntityDisplay();

    // Set default filter to first location if there are many messages
    const allMessages = self._debugStore.getAll();
    if (allMessages.length > 100 && locations.length > 0) {
      self._locationFilter.value = locations[0];
    }
  }

  /**
   * Gets the currently selected entity ID.
   *
   * @returns {?string} The selected entity ID or null for all.
   */
  _getCurrentEntityId() {
    const self = this;
    if (self._currentEntityIndex < 0 || self._entityIds.length === 0) {
      return null;
    }
    return self._entityIds[self._currentEntityIndex];
  }

  /**
   * Renders messages based on current filter selections.
   */
  _renderMessages() {
    const self = this;
    if (!self._debugStore) {
      self._messagesList.innerHTML = '<div class="debug-no-messages">No debug messages</div>';
      return;
    }

    const location = self._locationFilter.value || null;
    const step = self._stepFilter.value ? parseInt(self._stepFilter.value) : null;
    const entityId = self._getCurrentEntityId();
    const entityType = self._typeFilter.value || null;

    const filtered = self._debugStore.filter(location, step, entityId, entityType);
    const total = self._debugStore.getAll().length;

    self._filteredCount.textContent = filtered.length;
    self._totalCount.textContent = total;

    if (filtered.length === 0) {
      self._messagesList.innerHTML = '<div class="debug-no-messages">No messages match the current filters</div>';
      return;
    }

    // Limit display to prevent browser slowdown
    const displayLimit = 500;
    const toDisplay = filtered.slice(0, displayLimit);

    const html = toDisplay.map(msg => self._renderMessage(msg)).join("");
    self._messagesList.innerHTML = html;

    if (filtered.length > displayLimit) {
      const moreDiv = document.createElement("div");
      moreDiv.className = "debug-no-messages";
      moreDiv.textContent = `... and ${filtered.length - displayLimit} more messages. Use filters to narrow results.`;
      self._messagesList.appendChild(moreDiv);
    }
  }

  /**
   * Renders a single debug message to HTML.
   *
   * @param {DebugMessage} msg - The debug message to render.
   * @returns {string} HTML string for the message.
   */
  _renderMessage(msg) {
    const step = msg.getStep();
    const entityType = msg.getEntityType();
    const entityId = msg.getEntityId();
    const x = msg.getX();
    const y = msg.getY();
    const content = this._escapeHtml(msg.getContent());

    return `<div class="debug-message">
      <span class="debug-message-header">
        <span class="debug-message-step">[Step ${step}]</span>
        <span class="debug-message-entity">${entityType} @ ${entityId.substring(0, 8)}</span>
        <span class="debug-message-location">(${x}, ${y})</span>
      </span>
      <span class="debug-message-content">${content}</span>
    </div>`;
  }

  /**
   * Escapes HTML special characters in a string.
   *
   * @param {string} text - The text to escape.
   * @returns {string} The escaped text.
   */
  _escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }
}


export {DebugPresenter, ResultsPresenter};
