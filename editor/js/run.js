class RunPanelPresenter {

  constructor(rootId, getSimulations, onRun) {
    const self = this;
    self._root = document.getElementById(rootId);
    self._getSimulations = getSimulations;
    self._onRun = onRun;

    self._availablePanel = self._root.querySelector("#available-panel");
    self._runLocalDialog = self._root.querySelector("#run-local-dialog");

    self._setupDialog();
  }

  showButtons() {
    const self = this;
    self._availablePanel.style.display = "block";
  }

  hideButtons() {
    const self = this;
    self._availablePanel.style.display = "none";
  }

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


class RunRequest {

  constructor(simName, replicates) {
    const self = this;
    self._simName = simName;
    self._replicates = replicates;
  }

  getSimName() {
    const self = this;
    return self._simName;
  }

  getReplicates() {
    const self = this;
    return self._replicates;
  }

}



export {RunPanelPresenter};
