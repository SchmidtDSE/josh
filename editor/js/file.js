const CLEAR_CONFIRM_MESSAGE = [
  "This will clear the code you have written in the editor.",
  "Do you want to continue?"
].join(" ");


class FilePresenter {

  constructor(rootId, onFileOpen) {
    const self = this;
    self._root = document.getElementById(rootId);
    self._onFileOpen = onFileOpen;

    self._setupButtons();
  }

  saveCodeToFile(code) {
    const self = this;
    localStorage.setItem("source", code);
    self._updateSaveButton(code);
  }

  getCodeInFile() {
    const self = this;
    return localStorage.getItem("source");
  }

  _setupButtons() {
    const self = this;
    
    self._root.querySelector("#new-file-button").addEventListener("click", (event) => {      
      event.preventDefault();
      self._onNewFileClick();
    });

    self._root.querySelector("#load-file-button").addEventListener("click", (event) => {      
      event.preventDefault();
      self._showLoadDialog();
    });

    const loadFileDialog = self._root.querySelector("#load-file-dialog");

    loadFileDialog.querySelector(".cancel-button").addEventListener("click", (event) => {
      event.preventDefault();
      loadFileDialog.close();
    });

    loadFileDialog.querySelector(".load-button").addEventListener("click", (event) => {
      event.preventDefault();

      const file = loadFileDialog.querySelector(".upload-file").files[0];
      if (file) {
        const reader = new FileReader();
        reader.readAsText(file, "UTF-8");
        reader.onload = (event) => {
          const newCode = event.target.result;
          onFileOpen(newCode);
          self.saveCodeToFile(newCode);
          loadFileDialog.close();
        };
      }
    });
  }

  _updateSaveButton(code) {
    const self = this;
    const encodedValue = encodeURI("data:text/josh;charset=utf-8," + code);
    const saveButton = self._root.querySelector("#save-file-button");
    saveButton.href = encodedValue;
  }

  _onNewFileClick() {
    const self = this;
    if (confirm(CLEAR_CONFIRM_MESSAGE)) {
      self._onFileOpen("");
    }
  }

  _showLoadDialog() {
    const self = this;
    self._root.querySelector("#load-file-dialog").showModal();
  }
  
}


export {FilePresenter};
