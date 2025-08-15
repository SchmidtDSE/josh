/**
 * Logic for running the config dialog.
 *
 * @license BSD-3-Clause
 */

import {LocalFileLayer, OpfsFile} from "data";

const DEFAULT_CONFIG_CONTENT = "testVariable = 5 m";
const CONFIG_FILENAME = "editor.jshc";

/**
 * Presenter which runs the config dialog and manages the editor.jshc file.
 */
class ConfigDialogPresenter {

  /**
   * Create a new presenter for the config dialog.
   *
   * @param {string} openButtonId - The ID for the button used to open the dialog.
   * @param {string} dialogId - The ID for the dialog in which the user can edit the config file.
   */
  constructor(openButtonId, dialogId) {
    const self = this;
    self._fileLayer = new LocalFileLayer();
    self._openButton = document.getElementById(openButtonId);
    self._dialog = document.getElementById(dialogId);

    self._textarea = self._dialog.querySelector(".config-textarea");
    self._saveButton = self._dialog.querySelector(".config-save-button");
    self._cancelButton = self._dialog.querySelector(".config-cancel-button");

    self._originalContent = "";
    self._defaultContent = DEFAULT_CONFIG_CONTENT;

    self._attachListeners();
  }

  /**
   * Attach event listeners for running the config dialog UI.
   */
  _attachListeners() {
    const self = this;

    self._openButton.addEventListener("click", async (event) => {
      event.preventDefault();
      await self._openDialog();
    });

    self._cancelButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._cancelChanges();
    });

    self._saveButton.addEventListener("click", async (event) => {
      event.preventDefault();
      await self._saveChanges();
    });
  }

  /**
   * Open the config dialog and load the current editor.jshc content.
   */
  async _openDialog() {
    const self = this;
    try {
      const file = await self._fileLayer.getFile(CONFIG_FILENAME);
      if (file.getIsSaved()) {
        self._originalContent = file.getContents();
        self._textarea.value = self._originalContent;
      } else {
        // File doesn't exist, use default content
        self._originalContent = self._defaultContent;
        self._textarea.value = self._defaultContent;
      }
      self._dialog.showModal();
    } catch (error) {
      console.error("Error loading config file:", error);
      // If error loading, start with default content
      self._originalContent = self._defaultContent;
      self._textarea.value = self._defaultContent;
      self._dialog.showModal();
    }
  }

  /**
   * Cancel changes and close the dialog, reverting to original content.
   */
  _cancelChanges() {
    const self = this;
    self._textarea.value = self._originalContent;
    self._dialog.close();
  }

  /**
   * Save changes to editor.jshc and close the dialog.
   */
  async _saveChanges() {
    const self = this;
    try {
      const content = self._textarea.value;
      const opfsFile = new OpfsFile(CONFIG_FILENAME, content, false, false);
      await self._fileLayer.putFile(opfsFile);
      self._originalContent = content;
      self._dialog.close();
    } catch (error) {
      console.error("Error saving config file:", error);
      alert("Error saving config file: " + error.message);
    }
  }
}

export {ConfigDialogPresenter};
