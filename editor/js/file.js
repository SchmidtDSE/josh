
/**
 * Logic for handling file operations in the code editor.
 * 
 * @license BSD-3-Clause
 */

const CLEAR_CONFIRM_MESSAGE = [
  "This will clear the code you have written in the editor.",
  "Do you want to continue?"
].join(" ");


/**
 * Presenter which handles file operations for the code editor.
 * 
 * Manages loading, saving, and creating new files in the code editor interface.
 * Uses localStorage for persistent storage of code content.
 */
class FilePresenter {

  /**
   * Create a new file presenter.
   *
   * @param {string} rootId The ID of the element containing the file operation buttons.
   * @param {function} onFileOpen Function to call with loaded file content when a file is opened.
   */
  constructor(rootId, onFileOpen) {
    const self = this;
    self._root = document.getElementById(rootId);
    self._onFileOpen = onFileOpen;

    self._setupButtons();
  }

  /**
   * Saves the current code to local storage.
   *
   * @param {string} code The code content to save.
   */
  saveCodeToFile(code) {
    const self = this;
    localStorage.setItem("source", code);
    self._updateSaveButton(code);
  }

  /**
   * Gets the currently saved code from local storage.
   *
   * @returns {string|null} The saved code content or null if none exists.
   */
  getCodeInFile() {
    const self = this;
    return localStorage.getItem("source");
  }

  /**
   * Sets up event listeners for file operation buttons.
   *
   * @private
   */
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

  /**
   * Updates the save button's download link with current code.
   *
   * @private
   * @param {string} code The current code content to encode in the download link.
   */
  _updateSaveButton(code) {
    const self = this;
    const encodedValue = encodeURI("data:text/josh;charset=utf-8," + code);
    const saveButton = self._root.querySelector("#save-file-button");
    saveButton.href = encodedValue;
  }

  /**
   * Handles the new file button click event.
   *
   * @private
   */
  _onNewFileClick() {
    const self = this;
    if (confirm(CLEAR_CONFIRM_MESSAGE)) {
      self._onFileOpen("");
    }
  }

  /**
   * Shows the load file dialog.
   * 
   * @private
   */
  _showLoadDialog() {
    const self = this;
    self._root.querySelector("#load-file-dialog").showModal();
  }
  
}


export {FilePresenter};
