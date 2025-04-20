class ResultsPresenter {

  constructor(rootId) {
    const self = this;

    self._tabs = new Tabby("[data-tabs]");

    self._root = document.getElementById(rootId);
    self._statusPresenter = new StatusPresenter(self._root.querySelector(".status-tab"));
  }

  onSimStart() {
    const self = this;
    self._statusPresenter.resetProgress();
    self._root.style.display = "block";
  }

  onStep() {
    const self = this;
    self._statusPresenter.incrementProgress();
  }

}


class StatusPresenter {

  constructor(selection) {
    const self = this;
    self._numComplete = 0;
    self._root = selection;
  }

  resetProgress() {
    const self = this;
    self._numComplete = 0;
    self._updateProgress();
    self._root.querySelector(".running-indicator").style.display = "block";
  }

  incrementProgress() {
    const self = this;
    self._numComplete++;
    self._updateProgress();
  }

  _updateProgress() {
    const self = this;
    self._root.querySelector(".completed-count").innerHTML = self._numComplete;
  }

} 


export {ResultsPresenter};
