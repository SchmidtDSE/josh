/**
 * Editor-only control for sizing the grid visualization.
 *
 * This lives beside the editor rather than in the shared `viz` module because it is bound to the
 * editor's own markup: it reaches for `#map-width-input`, `#map-height-input`, and
 * `#update-map-button`, none of which exist on the other sites that draw the same heatmap.
 *
 * @license BSD-3-Clause
 */

import {MapDimensions} from "viz";


/**
 * Presenter for custom map sizing controls.
 *
 * Manages the details element that allows users to configure the width and height
 * of the grid visualization and triggers re-rendering with new dimensions.
 */
class MapConfigPresenter {

  /**
   * Create a new map configuration presenter.
   *
   * @param {Element} selection - The details element containing the configuration controls.
   * @param {function} callback - Function to call when map dimensions should be updated.
   *     Called with a MapDimensions object.
   */
  constructor(selection, callback) {
    const self = this;
    self._root = selection;
    self._callback = callback;
    self._widthInput = selection.querySelector("#map-width-input");
    self._heightInput = selection.querySelector("#map-height-input");
    self._updateButton = selection.querySelector("#update-map-button");

    self._addEventListeners();
  }

  /**
   * Set the default dimensions in the input fields.
   *
   * @param {MapDimensions} dimensions - Map dimensions object with width and height in pixels.
   */
  setDefaultDimensions(dimensions) {
    const self = this;
    self._widthInput.value = dimensions.getWidth();
    self._heightInput.value = dimensions.getHeight();
  }

  /**
   * Get the current dimensions from the input fields.
   *
   * @returns {MapDimensions} Map dimensions object with width and height in pixels.
   */
  getDimensions() {
    const self = this;
    const width = parseInt(self._widthInput.value) || 800;
    const height = parseInt(self._heightInput.value) || 600;
    return new MapDimensions(width, height);
  }

  /**
   * Add event listeners to the update button.
   */
  _addEventListeners() {
    const self = this;

    self._updateButton.addEventListener("click", (event) => {
      event.preventDefault();
      const dimensions = self.getDimensions();
      self._callback(dimensions);
    });
  }
}


export {MapConfigPresenter};
