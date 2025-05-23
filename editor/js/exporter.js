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
    self._locationOption = self._dialog.querySelector("#location-option");
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

    self._locationOption.style.display = metadata.hasDegrees() ? "block" : "none";
    
    self._downloadLink.style.display = "inline-block";
    
    self._updateDownloadDataUri();
  }

  /**
   * Attach listeners for opening and closing the export dialog box.
   */
  _attachListeners() {
    const self = this;
    
    self._exportButton.addEventListener("click", (event) => {
        event.preventDefault();
        self._dialog.showModal();
    });

    self._cancelLink.addEventListener("click", (event) => {
        event.preventDefault();
        self._dialog.close();
    });

    self._seriesSelect.addEventListener("change", () => {
        self._updateDownloadDataUri();
    });

    self._stepsSelect.addEventListener("change", () => {
        self._updateDownloadDataUri();
    });

    self._convertLocationToDegreesCheck.addEventListener("change", () => {
        self._updateDownloadDataUri();
    });
  }

  /**
   * Update the data URI in the download link.
   */
  _updateDownloadDataUri() {
    const self = this;
    const exportCommand = self._buildExportCommand();
    const dataUri = buildExportUri(self._metadata, self._dataset, exportCommand);
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
    self._seriesNames = seriesName;
    self._finalOnly = finalOnly;
    self._convertLocationToDegrees = convertLocationToDegrees;
  }

  /**
   * Get the name of the series to be exported.
   * 
   * @returns {string} Name of the series (simulation, patches, or entities).
   */
  getSeriesName() {
    const self = this;
    return self._seriesNames;
  }

  /**
   * Check if only the final timestep should be exported.
   * 
   * @returns {boolean} True if only final timestep should be exported, false for all timesteps.
   */
  isFinalOnly() {
    const self = this;
    return self._finalOnly;
  }

  /**
   * Check if locations should be converted to degrees.
   * 
   * @returns {boolean} True if locations should be converted to degrees.
   */
  shouldConvertLocationToDegrees() {
    const self = this;
    return self._convertLocationToDegrees;
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
  const convertToDegrees = command.shouldConvertLocationToDegrees();
  const rows = [];

  if (dataset.length == 0) {
    return "data:text/csv;charset=utf-8,";
  }

  const seriesName = command.getSeriesName();
  
  // Add header row
  const attributes = dataset[0].getVariables(seriesName);

  const attributesSorted = Array.from(attributes).sort();
  rows.push([...attributesSorted, "replicate"].join(","));

  // Add data rows
  dataset.forEach((replicate, replicateNum) => {
    const results = replicate.getSeries(seriesName);

    const addToRows = (resultsTarget) => {
      resultsTarget.forEach((result) => {
        rows.push(getCsvRow(result, attributesSorted, replicateNum, metadata, convertToDegrees));
      });
    };

    if (command.isFinalOnly()) {
      const lastStep = Math.max(...results.map((x) => x.getValue("step")));
      const lastStepResults = results.filter((x) => x.getValue("step") == lastStep);
      addToRows(lastStepResults);
    } else {
      addToRows(results);
    }
  });

  const addToRows = (resultsTarget) => {
    resultsTarget.forEach((result) => {
      rows.push(getCsvRow(result, attributesSorted, replicateNum, metadata, convertToDegrees));
    });
  }

  const csvContent = rows.join("\n");
  const filename = `${seriesName}.csv`;
  return `data:text/csv;charset=utf-8;filename=${filename},${encodeURIComponent(csvContent)}`;
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
 * @param {SimulationMetadata} metadata - Metadata containing the simulation details required for
 *     degree conversions.
 * @param {boolean} convertToDegrees - Flag indicating whether or not location coordinates should
 *     be converted to degrees.
 * @returns {string} CSV serialization of this datum with the replicate number included after all
 *     attributesSorted. Does not include a newline.
 */
function getCsvRow(datum, attributesSorted, replicateNumber, metadata, convertToDegrees) {
  const convertToDegreesActive = convertToDegrees && metadata.hasDegrees();
  const values = attributesSorted.map((attr) => {
    if (!datum.hasValue(attr)) {
      return "";
    }
    let value = datum.getValue(attr);

    const isPosition = attr === "position.x" || attr === "position.y";
    if (convertToDegreesActive && isPosition) {
      const x = datum.getValue("position.x");
      const y = datum.getValue("position.y");
      const coords = getPositionInDegrees(metadata, x, y);
      value = attr === "position.x" ? coords.getLongitude() : coords.getLatitude();
    }
    
    if (typeof value === "number") {
      return value.toString();
    } else {
      return value;
    }
  });
  
  values.push(replicateNumber.toString());
  return values.join(",");
}


/**
 * Convert from an x and y position in grid-space coordinates to degrees by using the minimum and
 * maximum latitude and longitude coordinates where grid-space coordinates may be meters or may be
 * number of patches. If the later, the first patch center point will be at 0.5, 0.5.
 *
 * @param {SimulationMetadata} metadata - Information about the simulation required to construct a
 *     grid.
 * @param {number} xInCount - The patch-space horizontal coordinate to be converted.
 * @param {number} yInCount - The patch-space vertical coordinate to be converted.
 * returns {EarthCoordinate} The position converted to Earth-space coordinates.
 */
function getPositionInDegrees(metadata, xInCount, yInCount) {
  if (!metadata.hasDegrees()) {
    throw "Cannot convert as metadata does not specify degrees."
  }
  
  const minLon = metadata.getMinLongitude();
  const maxLon = metadata.getMaxLongitude();
  const minLat = metadata.getMinLatitude();
  const maxLat = metadata.getMaxLatitude();

  const startX = metadata.getStartX();
  const endX = metadata.getEndX();
  const startY = metadata.getStartY();
  const endY = metadata.getEndY();

  const gridWidth = endX - startX;
  const gridHeight = endY - startY;
  
  const lonRange = maxLon - minLon;
  const latRange = maxLat - minLat;

  const percentX = (xInCount - startX) / gridWidth;
  const percentY = (yInCount - startY) / gridHeight;
  
  const longitude = minLon + percentX * lonRange;
  const latitude = minLat + percentY * latRange;
  
  return new EarthCoordinate(longitude, latitude);
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
