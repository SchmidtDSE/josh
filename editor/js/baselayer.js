/**
 * Logic to run the basemap dialog and basemap image retrieval logic.
 *
 * @license BSD-3-Clause
 */

const MAX_IMAGE_WIDTH = 1280;
const MAX_IMAGE_HEIGHT = 1280;
const MAPBOX_BASE_URL = "https://api.mapbox.com/styles/v1";


/**
 * Create a new basemap dialog presenter which runs the open button and dialog itself.
 * 
 * Create a presenter which can be used to request basemaps from Mapbox through the static images
 * API (https://api.mapbox.com/styles/v1).
 */
class BasemapDialogPresenter {

  /**
   * Create a new presenter.
   *
   * @param {Element} openButton - Element which, when clicked, should open the dialog.
   * @param {Element} dialog - The actual basemap dialog element.
   * @param {function} imageUrlCallback - Function to be called with a single parameter which is the
   *     string if a basemap image should be used (in this case it will be the URL at which that
   *     image may be retrieved) or null if no basemap should be used.
   */
  constructor(openButton, dialog, imageUrlCallback) {
    const self = this;
    
    self._openButton = openButton;
    self._dialog = dialog;

    self._settingsAvailablePanel = self._dialog.querySelector("#map-settings-available");
    self._settingsUnavailablePanel = self._dialog.querySelector("#map-settings-unavailable");
    
    self._basemapLayerSelect = self._dialog.querySelector("#basemap-layer-select");
    self._mapboxUsernameInput = self._dialog.querySelector("#mapbox-username-input");
    self._apiKeyInput = self._dialog.querySelector("#mapbox-key-input");
    self._closeButton = self._dialog.querySelector("#close-map-button");
    
    self._metadata = null;
    self._imageUrlCallback = imageUrlCallback;

    self._addEventListeners();
    self._updateVisibility();
  }

  /**
   * Update the metadata to be used in determining the image boundaries.
   *
   * @param {SimulationMetadata} metadata - The metadata from which to get the grid boundaries
   *     to use for setting the bbox in mapbox requests.
   */
  setMetadata(metadata) {
    const self = this;
    
    self._metadata = metadata;
    
    self._updateVisibility();
    self._generateAndSendUrl();
  }

  /**
   * Add event listeners to the open and close buttons.
   *
   * Add click listeners that open the dialog on open button and both close the dialog on the close
   * button and, upon close, also generate and send a mapbox URL. Events' defaults prevented. Also
   * adds a callback for internal visibility for basemap.
   */
  _addEventListeners() {
    const self = this;
    
    self._openButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._dialog.showModal();
    });

    self._closeButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._dialog.close();
      self._generateAndSendUrl();
    });

    self._basemapLayerSelect.addEventListener("click", (event) => {
      self._updateVisibility();
    });
  }

  /**
   * Generate a new basemap URL and send via image URL callback.
   */
  _generateAndSendUrl() {
    const self = this;
    self._imageUrlCallback(self._generateUrl());
  }

  /**
   * Generate a URL that retrieves a static map.
   *
   * Generate a URL that retrieves a static map by specifying the low and high latitude and
   * longitude through bbox, automatically calculating the zoom.  This will be generated using the
   * current style specification, username, and API key found in the dialog as well as the most
   * recently supplied metadata. If the high density check is selected, will request with the "@2x"
   * option. Attribution always included. Uses png format.
   * 
   * @returns {!string} URL with authentication information included where this image can be found
   *     or null if the metadata has not been provided or does not have Earth-space coordinates
   *     available.
   */
  _generateUrl() {
    const self = this;
    
    if (!self._metadata || !self._metadata.hasDegrees()) {
      return null;
    }

    if (!self._userSelectedBasemap()) {
      return null;
    }

    const apiKey = self._apiKeyInput.value;
    if (!apiKey) {
      alert("No basemap added: Please specify a Mapbox API key.");
      return null;
    }

    const dimensions = self._scaleDimensions();
    if (!dimensions) {
      return null;
    }

    const style = self._basemapLayerSelect.value;
    const width = dimensions.getImageWidth();
    const height = dimensions.getImageHeight();
    const bbox = [
      self._metadata.getMinLongitude(),
      self._metadata.getMinLatitude(), 
      self._metadata.getMaxLongitude(),
      self._metadata.getMaxLatitude()
    ].join(',');

    // Construct the final URL with authentication
    return [
      MAPBOX_BASE_URL,
      "mapbox",
      style,
      "static",
      `[${bbox}]`,
      `${width}x${height}@2x`
    ].join("/") + `?access_token=${apiKey}&attribution=true`;
  }

  /**
   * Update if the settings available or settings not available panel is shown.
   *
   * If no metadata have been provided or the last provided metadata does not have Earth-space
   * coordinates, show the settings unavailable panel. Otherwise, show the settings available panel
   * and then hide mapbox credentials selection unless the user has selected a basemap in which case
   * it should be shown.
   */
  _updateVisibility() {
    const self = this;
    
    const hasMetadata = self._metadata !== null && self._metadata.hasDegrees();
    
    self._settingsAvailablePanel.style.display = hasMetadata ? "block" : "none";
    self._settingsUnavailablePanel.style.display = hasMetadata ? "none" : "block";
    
    const credentialsPanel = self._dialog.querySelector("#mapbox-credentials");
    credentialsPanel.style.display = self._userSelectedBasemap() ? "block" : "none";
  }

  /**
   * Determine if the user has indicated a basemap layer.
   *
   * @returns {boolean} True if a basemap layer should be shown and requested from Mapbox and false
   *     if the grid should be displayed without a basemap.
   */
  _userSelectedBasemap() {
    const self = this;
    return self._basemapLayerSelect.value !== "none";
  }

  /**
   * Get the image dimensions that shoudl be requested from Mapbox.
   *
   * Get the desired dimension of the image using the most recently provided metadata or null if
   * no metadata yet provided. The image will be of either the maximum allowed width or height with
   * the dimension scalled accordingly so a grid 120 patches wide and 60 patches tall will have an
   * image 1280 pixels wide and 640 pixels tall.
   *
   * @returns {?ImageDimensions} Record describing the desired image dimensions.
   */
  _scaleDimensions() {
    const self = this;

    if (!self._metadata) {
      return null;
    }

    const width = self._metadata.getEndX() - self._metadata.getStartX();
    const height = self._metadata.getEndY() - self._metadata.getStartY();

    const widthLargerThanHeight = width > height;
    const aspectRatio = width / height;
    
    if (widthLargerThanHeight) {
      const imageWidth = MAX_IMAGE_WIDTH;
      const imageHeight = Math.round(MAX_IMAGE_HEIGHT / aspectRatio);
      return new ImageDimensions(imageWidth, imageHeight);
    } else {
      const imageHeight = MAX_IMAGE_HEIGHT;
      const imageWidth = Math.round(MAX_IMAGE_WIDTH * aspectRatio);
      return new ImageDimensions(imageWidth, imageHeight);
    }
  }
  
}


/**
 * Class representing the dimensions of an image in pixels.
 */
class ImageDimensions {

  /**
   * Creates a new image dimensions object.
   * 
   * @param {number} imageWidth - The width of the image in pixels.
   * @param {number} imageHeight - The height of the image in pixels.
   */
  constructor(imageWidth, imageHeight) {
    const self = this;
    self._imageWidth = imageWidth;
    self._imageHeight = imageHeight;
  }

  /**
   * Gets the width of the image.
   * 
   * @returns {number} The width in pixels.
   */
  getImageWidth() {
    const self = this;
    return self._imageWidth;
  }

  /**
   * Gets the height of the image.
   * 
   * @returns {number} The height in pixels.
   */
  getImageHeight() {
    const self = this;
    return self._imageHeight;
  }
  
}


export {BasemapDialogPresenter};
