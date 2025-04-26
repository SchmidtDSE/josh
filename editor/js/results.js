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

    self._tabs = new Tabby("[data-tabs]");

    self._root = document.getElementById(rootId);
    self._statusPresenter = new StatusPresenter(self._root.querySelector(".status-tab"));

    self._secondsOnStart = null;
  }

  /**
   * Indicate the start of a new simulation run.
   */
  onSimStart() {
    const self = this;
    self._statusPresenter.resetProgress();
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

  showError(message) {
    const self = this;
    self._root.querySelector(".error-display").style.display = "block";
    
    const errorMessageHolder = self._root.querySelector(".error-message");
    errorMessageHolder.innerHTML = "";
    
    const textNode = document.createTextNode(message);
    errorMessageHolder.appendChild(textNode);
  }

} 


export {ResultsPresenter};
