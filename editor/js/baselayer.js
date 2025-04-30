/**
 * Logic to run the basemap dialog and basemap image retrieval logic.
 *
 * @license BSD-3-Clause
 */

const MAX_IMAGE_WIDTH = 1280;
const MAX_IMAGE_HEIGHT = 1280;


/**
 * Create a new basemap dialog presenter which runs the open button and dialog itself.
 * 
 * Create a presenter which can be used to request basemaps from Mapbox through the static images
 * API (https://api.mapbox.com/styles/v1).
 */
class BasemapDialogPresenter {

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
  }

  setMetadata(metadata) {
    const self = this;
    self._metadata = metadata;
    self._updateVisibility();
    self._generateAndSendUrl();
  }

  _addEventListeners() {
    const self = this;
    
    self._openButton.addEventListener('click', (e) => {
      e.preventDefault();
      self._dialog.showModal();
    });

    self._closeButton.addEventListener('click', (e) => {
      e.preventDefault();
      self._dialog.close();
      self._generateAndSendUrl();
    });

    self._basemapLayerSelect.addEventListener('change', () => {
      self._updateVisibility();
      self._generateAndSendUrl();
    });
  }

  _generateAndSendUrl() {
    const self = this;
    self._imageUrlCallback(self._generateUrl());
  }

  _generateUrl() {
    const self = this;
    
    if (!self._metadata || !self._metadata.hasDegrees() || !self._userSelectedBasemap()) {
      return '';
    }

    const dimensions = self._scaleDimensions();
    if (!dimensions) {
      return '';
    }

    const username = self._mapboxUsernameInput.value;
    const apiKey = self._apiKeyInput.value;
    const style = self._basemapLayerSelect.value;
    const width = dimensions.getImageWidth();
    const height = dimensions.getImageHeight();
    
    const bbox = [
      self._metadata.getMinLongitude(),
      self._metadata.getMinLatitude(),
      self._metadata.getMaxLongitude(),
      self._metadata.getMaxLatitude()
    ].join(',');

    return `https://api.mapbox.com/styles/v1/${username}/${style}/static/[${bbox}]/${width}x${height}?access_token=${apiKey}&attribution=true`;
  }

  _updateVisibility() {
    const self = this;
    
    const hasMetadata = self._metadata !== null;
    const hasDegrees = hasMetadata && self._metadata.hasDegrees();
    
    if (!hasMetadata || !hasDegrees) {
      self._settingsAvailablePanel.style.display = 'none';
      self._settingsUnavailablePanel.style.display = 'block';
      return;
    }

    self._settingsAvailablePanel.style.display = 'block';
    self._settingsUnavailablePanel.style.display = 'none';

    const credentialsPanel = self._dialog.querySelector("#mapbox-credentials");
    credentialsPanel.style.display = self._userSelectedBasemap() ? 'block' : 'none';
  }

  _scaleDimensions() {
    const self = this;
    
    if (!self._metadata) {
      return null;
    }

    const gridWidth = self._metadata.getEndX() - self._metadata.getStartX();
    const gridHeight = self._metadata.getEndY() - self._metadata.getStartY();
    const aspectRatio = gridWidth / gridHeight;

    let imageWidth, imageHeight;
    if (aspectRatio >= 1) {
      imageWidth = MAX_IMAGE_WIDTH;
      imageHeight = Math.round(MAX_IMAGE_WIDTH / aspectRatio);
    } else {
      imageHeight = MAX_IMAGE_HEIGHT;
      imageWidth = Math.round(MAX_IMAGE_HEIGHT * aspectRatio);
    }

    return new ImageDimensions(imageWidth, imageHeight);
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
   * button and, upon close, also generate and send a mapbox URL. Events' defaults prevented.
   */
  _addEventListeners() {
    const self = this;
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
   * option. Attribution always included.
   * 
   * @returns {string} URL with authentication information included where this image can be found.
   */
  _generateUrl() {
    const self = this;
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
  }
  
}


/**
 * Container for image dimensions used in Mapbox Static Images API requests.
 */
class ImageDimensions {

  /**
   * Creates a new image dimensions container.
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
   * Gets the width of the image in pixels.
   * 
   * @returns {number} The image width in pixels.
   */
  getImageWidth() {
    const self = this;
    return self._imageWidth;
  }

  /**
   * Gets the height of the image in pixels.
   * 
   * @returns {number} The image height in pixels.
   */
  getImageHeight() {
    const self = this;
    return self._imageHeight;
  }
  
}
