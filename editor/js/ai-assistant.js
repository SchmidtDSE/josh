/**
 * Logic for running the AI Assistant dialog.
 *
 * @license BSD-3-Clause
 */

/**
 * Presenter which runs the AI Assistant dialog to provide information about using LLMs with Josh.
 */
class AiAssistantDialogPresenter {

  /**
   * Create a new presenter for the AI Assistant dialog.
   *
   * @param {string} openButtonId - The ID for the button used to open the dialog.
   * @param {string} dialogId - The ID for the dialog that shows AI assistant information.
   */
  constructor(openButtonId, dialogId) {
    const self = this;
    self._openButton = document.getElementById(openButtonId);
    self._dialog = document.getElementById(dialogId);

    self._closeButton = self._dialog.querySelector(".ai-assistant-close-button");

    self._attachListeners();
  }

  /**
   * Attach event listeners for running the AI Assistant dialog UI.
   */
  _attachListeners() {
    const self = this;

    self._openButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._openDialog();
    });

    self._closeButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._closeDialog();
    });

    // Close dialog when clicking outside
    self._dialog.addEventListener("click", (event) => {
      if (event.target === self._dialog) {
        self._closeDialog();
      }
    });

    // Close dialog on Escape key
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && self._dialog.open) {
        self._closeDialog();
      }
    });
  }

  /**
   * Open the AI Assistant dialog.
   */
  _openDialog() {
    const self = this;
    self._dialog.showModal();
  }

  /**
   * Close the AI Assistant dialog.
   */
  _closeDialog() {
    const self = this;
    self._dialog.close();
  }
}

export {AiAssistantDialogPresenter};