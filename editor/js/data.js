/**
 * Logic for running the external data dialog and OPFS file system.
 *
 * @license BSD-3-Clause
 */

const MB_CONVERSION = 1 / (1024 * 1024);
const RECOMMENDED_SERVER_MAX_MB = 500;
const TEXT_TYPES = ["text/csv", "application/json"];


/**
 * Presenter which runs the data files dialog and the OPFS files layer.
 */
class DataFilesPresenter {

  /**
   * Create a new presenter for the data files dialog.
   *
   * @param {string} openButtonId - The ID for the button used to open the dialog.
   * @param {string} dialogId - The ID for the dialog in which the user can manipulate the OPFS file
   *     system.
   */
  constructor(openButtonId, dialogId) {
    const self = this;
    self._fileLayer = new LocalFileLayer();
    self._openButton = document.getElementById(openButtonId);
    self._dialog = document.getElementById(dialogId);

    self._filesPanel = self._dialog.querySelector(".files-panel");
    self._addFileButton = self._dialog.querySelector(".add-file-button");
    self._closeButton = self._dialog.querySelector(".cancel-button");
    self._fileUploadInput = self._dialog.querySelector(".upload-input");
    self._fileUploadCancelButton = self._dialog.querySelector(".add-file-cancel-button");
    self._fileUploadConfirmButton = self._dialog.querySelector(".add-file-confirm-button");
    self._fileUploadIdlePanel = self._dialog.querySelector(".file-upload-idle-panel");
    self._fileUploadActivePanel = self._dialog.querySelector(".file-upload-active-panel");
    self._spaceUtilizationProgressBar = self._dialog.querySelector(".space-utilization-progress");
    self._usedMbLabel = self._dialog.querySelector(".used-mb-display");
    self._totalMbLabel = self._dialog.querySelector(".total-mb-display");

    self._hideFileUpload();
    self._attachListeners();
    self._refreshFilesList();
    self._updateSpaceUtilizationDisplay();
  }

  /**
   * Get the contents of the file system as a JSON string.
   *
   * @returns {Promise<string>} The contents of the file system serialized as JSON.
   */
  async getFilesAsJson() {
    const self = this;
    return self._fileLayer.serialize();
  }

  /**
   * Attach event listeners for running the data files UI.
   */
  _attachListeners() {
    const self = this;

    self._openButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._dialog.showModal();
    });

    self._closeButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._dialog.close();
    });

    self._addFileButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._showFileUpload();
    });

    self._fileUploadCancelButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._hideFileUpload();
    });

    self._fileUploadConfirmButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._uploadFile();
    });
  }

  /**
   * Show the file upload panel and hide the idle panel.
   */
  _showFileUpload() {
    const self = this;
    self._fileUploadIdlePanel.style.display = "none";
    self._fileUploadActivePanel.style.display = "block";
  }

  /**
   * Hide the file upload panel and show the idle panel.
   */
  _hideFileUpload() {
    const self = this;
    self._fileUploadIdlePanel.style.display = "block";
    self._fileUploadActivePanel.style.display = "none";
  }

  /**
   * Add the file in the file upload input to the OPFS file system.
   *
   * Take the current file in the file upload input and add it to the OPFS file system with the
   * same name as the file had in the upload input. If a file by that name already exists, use 
   * confirm to check with the user if the file should be overwritten. The file list will be
   * refreshed after file upload.
   */
  async _uploadFile() {
    const self = this;
    const file = self._fileUploadInput.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (e) => {
      const contents = e.target.result;
      const isBinaryFile = self._getIsBinaryFile(file.type);
      const opfsFile = new OpfsFile(file.name, contents, false, isBinaryFile);
      try {
        await self._fileLayer.putFile(opfsFile);
        await self._refreshFilesList();
        await self._updateSpaceUtilizationDisplay();
        self._hideFileUpload();
      } catch (error) {
        alert("Error uploading file: " + error);
      }
    };
    reader.readAsText(file);
  }

  /**
   * Update the files panel to show the current listing of files in the OPFS file system.
   */
  async _refreshFilesList() {
    const self = this;
    const filesList = await self._fileLayer.listFiles();
    const filesPanelD3 = d3.select(self._filesPanel);

    filesPanelD3.html("");

    const filesGroups = filesPanelD3.selectAll(".file-group")
      .data(filesList)
      .enter()
      .append("div")
      .classed("file-group", true);

    const deleteHolders = filesGroups.append("div").classed("delete-holder", true);

    deleteHolders.append("a")
      .attr("href", "#")
      .text("delete")
      .attr("aria-label", (name) => "Delete " + name)
      .on("click", async (name) => {
        if (confirm(`Are you sure you want to delete ${name}?`)) {
          try {
            await self._removeFile(name);
          } catch (error) {
            console.error("Error deleting file:", error);
            alert("Error deleting file. Please try again.");
          }
        }
      });

    const labelHolders = filesGroups.append("div").classed("label-holder", true);

    labelHolders.text((name) => name);
  }

  /**
   * Request that the file at the given path be removed from the OPFS file sytem.
   *
   * Request that the file at the given path be removed from the OPFS file sytem before refreshing
   * the files list.
   *
   * @param {string} name - The path (name) of the file to be removed.
   */
  async _removeFile(name) {
    const self = this;
    await self._fileLayer.deleteFile(name);
    self._refreshFilesList();
    self._updateSpaceUtilizationDisplay();
  }

  /**
   * Determine if a file should be treated as binary or text based on mime filetype.
   *
   * @param {string} filetype - The mime type associated with the file.
   * @returns {boolean} True if should be treated as binary and false otherwise.
   */
  _getIsBinaryFile(filetype) {
    const self = this;
    return !TEXT_TYPES.includes(filetype);
  }

  /**
   * Update the progress bar and labels showing space utilization.
   */
  async _updateSpaceUtilizationDisplay() {
    const self = this;
    const usedMb = await self._fileLayer.getMbUsed();
    const totalMb = await self._fileLayer.getAvailableSpace();
    const percentUsed = (usedMb / totalMb) * 100;

    self._spaceUtilizationProgressBar.value = percentUsed;
    self._usedMbLabel.innerHTML = Math.round(usedMb);
    self._totalMbLabel.innerHTML = Math.round(totalMb);
  }

}


/**
 * Layer that manages data files in OPFS.
 *
 * Layer that emulates a file system in OPFS as a simple mapping between string paths (names) and
 * string file contents. Binary data may be converted to base64 for persistance. Also provides an
 * option to serialize the entire contents of the file system to a JSON string. Offloads to a worker
 * thread defined in data.worker.js.
 */
class LocalFileLayer {

  /**
   * Create a new file manager for OPFS.
   */
  constructor() {
    const self = this;
    // TODO
  }

  /**
   * Get the contents of a file as a string.
   *
   * @param {string} name - The path (name) of the file to be retrieved.
   * @returns {Promise<OpfsFile>} The file at this filename. Will have unsaved file flag set to
   *     true if does not yet exist.
   */
  async getFile(name) {
    const self = this;
    // TODO
  }

  /**
   * Add a file to the OPFS file system, overwritting if file perviously present.
   *
   * @param {Promise<OpfsFile>} The file to persist, creating a new file if it does not yet exist or
   *     overwritting the prior file of with the same path.
   */
  async putFile(file) {
    const self = this;
    // TODO
  }

  /**
   * Delete the file at the given path.
   *
   * @param {string} name - The path (name) of the file to be deleted.
   * @returns {Promise} Promise which resolves after the file is deleted.
   * @throws Exception thrown if file could not be found.
   */
  async deleteFile(name) {
    const self = this;
    // TODO
  }

  /**
   * Get a list of all files in the OPFS file system.
   *
   * @returns {Promise<Array<string>>} Collection of all paths currently in this OPFS file system.
   */
  async listFiles() {
    const self = this;
    // TODO
  }

  /**
   * Get the total space used in this OPFS file system.
   *
   * Get the total space used in this OPFS file system determined by serializing the contents of
   * this file system and determining its space utilization.
   *
   * @returns {Promise<number>} The size of the OPFS file system in megabytes when serialized.
   */
  async getMbUsed() {
    const self = this;
    // TODO
  }

  /**
   * Get the total available space for OPFS reported by the browser.
   *
   * Get the total available space for OPFS reported by the browser or 500 MB (the guideline
   * recommended maximum for Josh servers), whichever is smallest.
   *
   * @returns {Promise<number>} Promise resolving to the number of megabytes available (minimum of
   *     browser allowance or Josh server recommendation).
   */
  async getAvailableSpace() {
    const estimate = await navigator.storage.estimate();
    const availableMb = (estimate.quota - estimate.usage) * MB_CONVERSION;
    return Math.min(availableMb, RECOMMENDED_SERVER_MAX_MB);
  }

  /**
   * Convert the file system to a JSON string.
   *
   * @returns {Promise<Object>} JSON serializable object of the current OPFS file system contents.
   */
  async serialize() {
    const self = this;
    // TODO
  }

}


/**
 * Structure representing a file for the OPFS file system.
 */
class OpfsFile {

  /**
   * Create a new record of a file which may be in the OPFS file system or which is compatible.
   *
   * @param {string} name - The path (name) of the file represented by this structure.
   * @param {string} contents - The string contents of this file. If the file is binary, this
   *     should be base64 encoded. May be empty string for new file.
   * @param {boolean} saved - Flag indicating if this file has been saved to OPFS. True if this
   *     file has been saved and false if this is not yet persisted (new file or new version of an
   *     existing file).
   * @param {boolean} binary - Flag indicating if this file contains binary data. True if contents
   *     is a base64 encoded string representation of a binary blob. False otherwise.
   */
  constructor(name, contents, saved, binary) {
    const self = this;
    self._name = name;
    self._contents = contents;
    self._saved = saved;
    self._binary = binary;
  }

  /**
   * Get the path (name) of this file.
   *
   * @returns {string} The location at which this file is saved in the local OPFS file system.
   */
  getName() {
    const self = this;
    return self._name;
  }

  /**
   * Get the contents of this file.
   *
   * @returns {string} The contents of this file as a string. If it is a binary file, this is the
   *     base64 encoding.
   */
  getContents() {
    const self = this;
    return self._contents;
  }

  /**
   * Determine if this file is saved or not.
   *
   * @returns {boolean} Flag indicating if this file has been saved to OPFS. True if this file has
   *     been saved and false if this is not yet persisted (new file or new version of an existing
   *     file).
   */
  getIsSaved() {
    const self = this;
    return self._saved;
  }

  /**
   * Determine if this file is a binary file or a string file.
   *
   * @returns {boolean} Flag indicating if this file contains binary data. True if contents is a
   *     base64 encoded string representation of a binary blob. False otherwise.
   */
  getIsBinary() {
    const self = this;
    return self._binary;
  }

}


export {DataFilesPresenter};
