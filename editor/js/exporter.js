/**
 * Logic to run the export dialog with data URIs providing the download itself.
 *
 * @license BSD-3-Clause
 */


/**
 * Presenter which runs the export button and dialog.
 *
 * Presenter which maintains a dialog with settings about how a CSV export should be prepared with
 * dynamic control visibility depending on if the simulation metadata has earth coordinates
 * associated. This will generate a data URI with the download and put it in the download link
 * styled as a button.
 */
class ExportPresenter {

  /**
   * Create a new export presenter.
   *
   * @param {Element} exportButton - The link which should open the export dialog.
   * @param {Element} dialog - Selection over the export dialog itself.
   */
  constructor(exportButton, dialog) {
    const self = this;
    self._exportButton = exportButton;
    self._dialog = dialog;
    self._seriesSelect = self._dialog.querySelector("#download-series-select");
    self._stepsSelect = self._dialog.querySelector("#timeseries-select");
    self._convertLocationToDegreesCheck = self._dialog.querySelector("#geo-export-check");
    self._downloadLink = self._dialog.querySelector("#download-uri-button");
    self._cancelLink = self._dialog.querySelector(".cancel-button");

    self._metadata = null;
    self._dataset = null;
    self._downloadLink.style.display = "none";

    self._attachListeners();
  }

  /**
   * Set the dataset from which results should be exported within this dialog.
   *
   * @param {SimulationMetadata} metadata - Metadata about the simulation executed and for which a
   *     dataset is provided.
   * @param {Array<SimulationResult>} dataset - The results of each replicate where all replicates
   *     should be exported.
   */
  setDataset(metadata, dataset) {
    const self = this;
    self._metadata = metadata;
    self._dataset = dataset;
    self._downloadLink.style.display = "inline-block";
    self._updateDownloadDataUri();
  }

  /**
   * Attach listeners for opening and closing the export dialog box.
   */
  _attachListeners() {
    const self = this;
    
    self._exportButton.addEventListener('click', function(event) {
        event.preventDefault();
        self._dialog.showModal();
    });

    self._cancelLink.addEventListener('click', function(event) {
        event.preventDefault();
        self._dialog.close();
    });
  }

  /**
   * Update the data URI in the download link.
   */
  _updateDownloadDataUri() {
    const self = this;
    const exportCommand = self._buildExportCommand();
    const dataUri = buildExportUri(command, self._dataset);
    self._downloadLink.href = dataUri;
  }

  /**
   * Build an ExportCommand from the current state of the inputs to this dialog.
   *
   * @returns {ExportCommand}
   */
  _buildExportCommand() {
    const self = this;
    return new ExportCommand(
      self._seriesSelect.value,
      self._stepsSelect.value === "final",
      self._convertLocationToDegreesCheck.checked ? true : false
    );
  }
  
}


/**
 * Record describing how an export data URI should be generated.
 */
class ExportCommand {

  /**
   * Create a new configuration record of how an export should be generated.
   *
   * @param {string} seriesName - Name of the series to be converted to a CSV (simulation, patches,
   *     or entities).
   * @param {boolean} finalOnly - Flag indicating if only the last timestep should be converted to a
   *     CSV. True if only the last timestep should be converted. False otherwise.
   * @param {boolean} convertLocationToDegrees - Flag indicating if position.x and position.y
   *     attributes on the OutputDatum objects shoudl be converted from patch index to latitude and
   *     longitude degrees.
   */
  constructor(seriesName, finalOnly, convertLocationToDegrees) {
    const self = this;
    self._seriesNames = seriesNames;
    self._finalOnly = finalOnly;
    self._convertLocationToDegrees = convertLocationToDegrees;
  }
  
}


/**
 * Generate a data URI string containing a CSV export.
 *
 * @param {SimulationMetadata} metadata - Metadata about the simulation executed and for which a
   *     dataset is provided.
   * @param {Array<SimulationResult>} dataset - The results of each replicate where all replicates
   *     should be serialized to a URI string.
 * @param {ExportCommand} command - Information about how the export data URI should be generated.
 * @returns {string} Data URI string containing the entire export.
 */
function buildExportUri(metadata, dataset, command) {
  
}


/**
 * Convert a single data point to a single CSV row.
 *
 * Convert a single data point to a single CSV row where all attribute values do not have a quote
 * character or a newline within them. Numbers will not have quotes in the returned string while
 * strings will have quotes around them. If an attribute is not found, an empty value will be
 * written in the string without quotes.
 *
 * @param {OutputDatum} datum - The data point to be converted to a CSV string.
 * @param {Array<string>} attributesSorted - Sorted array of attribute names indicating the order in
 *     which those attributes should appear in the result.
 * @param {number} replicateNumber - An integer uniquely indicating the replicate from which this
 *     datum was taken.
 * @returns {string} CSV serialization of this datum with the replicate number included after all
 *     attributesSorted. Does not include a newline.
 */
function getCsvRow(datum, attributesSorted, replicateNumber) {
  
}


/**
 * Convert from an x and y position in grid-space coordinates to degrees by using the minimum and
 * maximum latitude and longitude coordinates where grid-space coordinates may be meters or may be
 * number of patches. If the later, the first patch center point will be at 0.5, 0.5.
 *
 * @param {SimulationMetadata} metadata - Information about the simulation required to construct a
 *     grid.
 * @param {number} xInCount - The patch-space horizontal coordinate to be converted.
 * @param {number} xInCount - The patch-space vertical coordinate to be converted.
 * returns {EarthCoordinate} The position converted to Earth-space coordinates.
 */
function getPositionInDegrees(metadata, xInCount, yInCount) {
  
}


/**
 * Record representing a coordinate in Earth-space with latitude and longitude.
 */
class EarthCoordinate {

  /**
   * Create a new record of a coordinate in Earth-space.
   *
   * @param {number} longitude - The longitude of the point represented by this record.
   * @param {number} latitude - The latitude of the point represented by this record.
   */
  constructor(longitude, latitude) {
    const self = this;
    self._longitude = longitude;
    self._latitude = latitude;
  }
  
  /**
   * Get the longitude of this position.
   *
   * @returns {number} The longitude of the point represented by this record.
   */
  getLongitude() {
    const self = this;
    return self._longitude;
  }

  /**
   * Get the latitude of this position.
   *
   * @returns {number} The latitude of the point represented by this record.
   */
  getLatitude() {
    const self = this;
    return self._latitude;
  }
  
}


export {ExportPresenter};
