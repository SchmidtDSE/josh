/**
 * Logic for presenters handling simulation results display.
 * 
 * @license BSD-3-Clause
 */


/**
 * Presenter which manages the display of simulation results.
 * Handles tabs and status updates for simulation runs.
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
  }

  /**
   * Indicate the start of a new simulation run.
   */
  onSimStart() {
    const self = this;
    self._statusPresenter.resetProgress();
    self._root.style.display = "block";
  }

  /**
   * Updates progress when a simulation step completes.
   */
  onStep() {
    const self = this;
    self._statusPresenter.incrementProgress();
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
    self._numComplete = 0;
    self._root = selection;
  }

  /**
   * Resets the progress counter and updates display.
   */
  resetProgress() {
    const self = this;
    self._numComplete = 0;
    self._updateProgress();
    self._root.querySelector(".running-indicator").style.display = "block";
  }

  /**
   * Increments the progress counter and updates display.
   */
  incrementProgress() {
    const self = this;
    self._numComplete++;
    self._updateProgress();
  }

  /**
   * Updates the progress display with current count.
   * 
   * @private
   */
  _updateProgress() {
    const self = this;
    self._root.querySelector(".completed-count").innerHTML = self._numComplete;
  }
} 

export {ResultsPresenter};
