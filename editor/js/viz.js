/**
 * Logic to run the visualization components in the data display.
 *
 * @license BSD-3-Clause
 */

const SCRUB_RIGHT_PAD = 60;
const SCRUB_BOTTOM_PAD = 20;


/**
 * Presenter which runs the scrub bar chart visualization.
 */
class ScrubPresenter {

  /**
   * Create a new presenter for the scrub visualization.
   *
   * @param {Element} selection - Selection over the entire scrub presenter component which includes
   *     the SVG element where the graphic should actually display.
   * @param {function} callback - Function to call when a different timestep is selected.
   */
  constructor(selection, callback) {
    const self = this;
    self._dataset = null;
    self._root = selection;
    self._callback = callback;
    self._timestepSelected = null;
    self._svg = self._root.querySelector("#scrub-viz");
    self._svgSelection = d3.select(self._svg);
  }

  /**
   * Render the scrub visualization.
   *
   * Render the scrub visualization such that there is one bar per timestep in ascending order. The
   * graphic extents are minimum of 0 and the smallest value seen in the dataset's per-step values.
   *
   * @param {SummarizedResult} summarized - The dataset to visualize.
   */
  render(summarized) {
    const self = this;
    self._dataset = summarized;

    self._svgSelection.html("");

    const svgBounds = self._svg.getBoundingClientRect();
    const width = svgBounds.width - SCRUB_RIGHT_PAD;
    const height = svgBounds.height - SCRUB_BOTTOM_PAD;

    const minTimestep = summarized.getMinTimestep();
    const maxTimestep = summarized.getMaxTimestep();
    const numSteps = maxTimestep - minTimestep + 1;
    const timesteps = Array.from({length: numSteps}, (_, i) => minTimestep + i);

    const values = [];
    for (let step = minTimestep; step <= maxTimestep; step++) {
      values.push(summarized.getTimestepValue(step));
    }
    const valuesNoNull = values.filter((x) => x !== null);
    const minValue = Math.min(0, Math.min(...valuesNoNull));
    const maxValue = Math.max(...valuesNoNull);

    const xScale = d3.scaleBand()
      .domain(timesteps)
      .range([10, width - 5])
      .padding(0);

    const yScale = d3.scaleLinear()
      .domain([minValue, maxValue])
      .range([height - 7, 13]);

    const createBody = () => {
      const groups = self._svgSelection.selectAll("g")
        .data(timesteps)
        .enter()
        .append("g")
        .classed("scrub-group", true)
        .attr("transform", (d) => `translate(${xScale(d)},0)`);

      groups.append("rect")
        .attr("class", "display-bar")
        .attr("x", 1)
        .attr("y", (d) => yScale(summarized.getTimestepValue(d)))
        .attr("width", xScale.bandwidth() - 2)
        .attr("height", (d) => height - yScale(summarized.getTimestepValue(d)));

      groups.append("rect")
        .attr("class", "pointer-target")
        .attr("x", 0)
        .attr("y", 0)
        .attr("width", xScale.bandwidth())
        .attr("height", height)

      groups.append("text")
        .attr("x", xScale.bandwidth() / 2)
        .attr("y", height + 3)
        .text((d) => d)
        .classed("horizontal-embed-tick", true)

      groups.on("click", (_, d) => self._onStepSelect(d, xScale, yScale));

      self._svgSelection.selectAll(".selected-value-display")
        .data(timesteps)
        .enter()
        .append("text")
        .attr("x", (d) => xScale(d) + xScale.bandwidth() / 2)
        .attr("y", (d) => yScale(summarized.getTimestepValue(d)) - 1)
        .text((d) => summarized.getTimestepValue(d).toFixed(2))
        .classed("selected-value-display", true);
    };

    const addVerticalAxis = () => {
      self._svgSelection.append("text")
        .attr("x", width + 10)
        .attr("y", 13)
        .text(maxValue.toFixed(2))
        .classed("vertical-tick", true);

      self._svgSelection.append("text")
        .attr("x", width + 10)
        .attr("y", height - 3)
        .text(minValue.toFixed(2))
        .classed("vertical-tick", true);
    };

    const addHorizontalAxis = () => {
      self._svgSelection.append("text")
        .attr("x", xScale(minTimestep) + xScale.bandwidth() / 2)
        .attr("y", height + 3)
        .text(minTimestep)
        .classed("horizontal-tick", true);

      self._svgSelection.append("text")
        .attr("x", xScale(maxTimestep) + xScale.bandwidth() / 2)
        .attr("y", height + 3)
        .text(maxTimestep)
        .classed("horizontal-tick", true);
    };

    addVerticalAxis();
    addHorizontalAxis();
    createBody();

    const noTimestep = self._timestepSelected === null;
    const timestepTooSmall = self._timestepSelected < minTimestep;
    const timestepTooLarge = self._timestepSelected > maxTimestep;
    const needTimestepUpdate = noTimestep || timestepTooSmall || timestepTooLarge;
    if (needTimestepUpdate) {
      self._onStepSelect(maxTimestep, xScale, yScale);
    }
  }


  /**
   * Indicate that a timestep was selected.
   *
   * Indicate that a timestep was selected, selecting and highlighting the specified timestep in the
   * scrub visualization.
   *
   * @param {number} timestep - The specific timestep to select and highlight.
   * @param {d3.ScaleBand} xScale - D3 scale function that maps timesteps to x-axis positions.
   * @param {d3.ScaleLinear} yScale - D3 scale function that maps values to y-axis positions.
   */
  _onStepSelect(timestep, xScale, yScale) {
    const self = this;

    self._svgSelection.selectAll('.scrub-group')
      .classed('active', (d) => d === timestep);

    self._svgSelection.selectAll('.selected-value-display')
      .classed('active', (d) => d === timestep);

    const value = self._dataset.getTimestepValue(timestep);
    self._svgSelection.select(".selected-tick")
      .attr("y", yScale(value))
      .text(value.toFixed(2));

    self._callback(timestep);
  }

}


/**
 * Heatmap visualization showing the results spatially, one timestep at a time.
 * 
 * Visualization showing a grid with one square per patch and which shows one timestep at any given
 * moment. This is a heatmap with at least one pixel of padding between each cell rendered into an
 * SVG element whose width in pixels defines the horizontal size of each square and the vertical
 * size is set to be the same as the horizontal size with the SVG height dynamically updated to fit
 * the entire graphic. Hovering over a square adds the active class and the value at that cell is
 * displayed in the information panel. Uses d3.scaleSequential on d3.interpolateBlues.
 */
class GridPresenter {

  /**
   * Create a new heatmap visualization.
   *
   * @param {Element} selection - The containing element in which the SVG and info display is found.
   */
  constructor(selection) {
    const self = this;
    self._root = selection;
    self._infoSelection = selection.querySelector("#grid-viz-info");
    self._svgSelection = selection.querySelector("#grid-viz");
    self._d3SvgSelection = d3.select(self._svgSelection);
    self._customDimensions = null;

    const resultsSelection = document.getElementById("results-area");
    resultsSelection.addEventListener("scroll", () => {
      const outerRect = resultsSelection.getBoundingClientRect();
      const rect = self._svgSelection.getBoundingClientRect();
      const difference = rect.top - outerRect.top;
      if (difference < 35) {
        self._infoSelection.classList.add("fixed");
        self._infoSelection.style.top = (difference * -1 + 37) + "px";
      } else {
        self._infoSelection.classList.remove("fixed");
      }
    });
  }

  /**
   * Set custom dimensions for the visualization.
   *
   * @param {MapDimensions} dimensions - Map dimensions object with width and height in pixels.
   */
  setCustomDimensions(dimensions) {
    const self = this;
    self._customDimensions = dimensions;
  }

  /**
   * Calculate the default dimensions for the visualization.
   *
   * @param {SimulationMetadata} metadata - Metadata about the grid dimensions.
   * @returns {MapDimensions} Map dimensions object with default width and height in pixels.
   */
  calculateDefaultDimensions(metadata) {
    const self = this;
    const gridWidth = metadata.getEndX() - metadata.getStartX() + 1;
    const gridHeight = metadata.getEndY() - metadata.getStartY() + 1;
    const patchPixels = self._getPatchPixels(gridWidth, gridHeight);
    const defaultWidth = (patchPixels + 1) * gridWidth;
    const defaultHeight = (patchPixels + 1) * gridHeight;
    
    return new MapDimensions(defaultWidth, defaultHeight);
  }

  /**
   * Render the grid visualization.
   *
   * Render the grid visualization such that there is one square per patch with the color set
   * according to value at the current timestep.
   *
   * @param {SimulationMetadata} metadata - Metadata about the grid dimensions as read from the
   *     simulation definition.
   * @param {SummarizedResult} summarized - The dataset to visualize.
   * @param {number} timestep - The timestep to visualize.
   * @param {?string} basemapUrl - The URL at which the basemap PNG can be found.
   */
  render(metadata, summarized, timestep, basemapUrl) {
    const self = this;

    self._showIdleMessage(timestep);

    self._d3SvgSelection.html("");

    const baselayerGroup = self._d3SvgSelection.append("g").classed("base-layer", true);
    const glyphsGroup = self._d3SvgSelection.append("g")
      .classed("glyphs-layer", true)
      .classed("overlay", basemapUrl !== null);

    const svgBounds = self._svgSelection.getBoundingClientRect();
    const gridWidth = metadata.getEndX() - metadata.getStartX() + 1;
    const gridHeight = metadata.getEndY() - metadata.getStartY() + 1;

    const patchSize = metadata.getPatchSize();
    const patchSizeHalf = patchSize / 2;

    const patchPixels = self._getPatchPixels(gridWidth, gridHeight);

    const totalWidth = (patchPixels + 1) * gridWidth;
    const totalHeight = (patchPixels + 1) * gridHeight;

    const cells = [];
    const endXPad = metadata.getEndX() - patchSizeHalf;
    const endYPad = metadata.getEndY() - patchSizeHalf;
    for (let x = metadata.getStartX(); x < endXPad; x += patchSize) {
      for (let y = metadata.getStartY(); y < endYPad; y += patchSize) {
        const value = summarized.getGridValue(timestep, x + patchSizeHalf, y + patchSizeHalf);
        if (value !== null) {
          cells.push({x: x, y: y, value: value});
        }
      }
    }

    const values = cells.map(d => d.value);
    const colorScale = d3.scaleSequential()
      .domain([Math.min(...values), Math.max(...values)])
      .interpolator(d3.interpolateBlues);

    const xScale = d3.scaleLinear()
      .domain([metadata.getStartX(), metadata.getEndX()])
      .range([0, totalWidth]);

    const yScale = d3.scaleLinear()
      .domain([metadata.getStartY(), metadata.getEndY()])
      .range([0, totalHeight]);

    const grid = glyphsGroup.selectAll("g")
      .data(cells)
      .enter()
      .append("g")
      .attr("transform", (d) => `translate(${xScale(d.x)}, ${yScale(d.y)})`)
      .classed("grid-patch", true);

    grid.append("rect")
      .attr("width", patchPixels)
      .attr("height", patchPixels)
      .attr("fill", (d) => colorScale(d.value))
      .on("mouseover", (event, d) => self._showInfoMessage(d.x, d.y, timestep, d.value))
      .on("mouseout", (event, d) => self._showIdleMessage(timestep))
      .classed("grid-patch-foreground", true);

    self._d3SvgSelection
      .style("width", totalWidth + "px")
      .style("height", totalHeight + "px");

    self._updateLegend(colorScale);

    if (basemapUrl !== null) {
      self._addBaselayer(basemapUrl, baselayerGroup, totalWidth, totalHeight);
    }
  }

  /**
   * Update the color legend with the current scale values.
   *
   * @param {d3.ScaleSequential} colorScale - The color scale to use for the legend
   */
  _updateLegend(colorScale) {
    const domain = colorScale.domain();
    const step = (domain[1] - domain[0]) / 4;
    const legendValues = [
      domain[0],
      domain[0] + step,
      domain[0] + 2 * step,
      domain[0] + 3 * step,
      domain[1]
    ];

    const legend = d3.select("#grid-legend");
    legend.select(".color .lowest").style("background-color", colorScale(legendValues[0]));
    legend.select(".color .low").style("background-color", colorScale(legendValues[1]));
    legend.select(".color .high").style("background-color", colorScale(legendValues[3]));
    legend.select(".color .highest").style("background-color", colorScale(legendValues[4]));

    legend.select(".label .lowest").text(legendValues[0].toFixed(2));
    legend.select(".label .low").text(legendValues[1].toFixed(2));
    legend.select(".label .high").text(legendValues[3].toFixed(2));
    legend.select(".label .highest").text(legendValues[4].toFixed(2));
  }

  /**
   * Add the image at basemapUrl to baselayerGroup such that it stretches to the entire space.
   *
   * Add the image at basemapUrl to baselayerGroup such that it stretches to the entire space such
   * that the image starts at 0, 0, and stretches to totalWidth and totalHeight.
   *
   * @param {string} basemapUrl - The URL at which the basemap PNG can be found.
   * @param {d3.select} baselayerGroup - D3 selection at which the image should be added.
   * @param {number} totalWidth - The width to which the image should be stretched in pixels.
   * @param {number} totalHeight - The height to which the image should be stretched in pixels.
   */
  _addBaselayer(basemapUrl, baselayerGroup, totalWidth, totalHeight) {
    baselayerGroup.append("image")
      .attr("xlink:href", basemapUrl)
      .attr("x", 0)
      .attr("y", 0)
      .attr("width", totalWidth)
      .attr("height", totalHeight)
      .classed("basemap-layer-image", true);
  }

  /**
   * Determine the pixel size for each grid patch based on the grid width.
   *
   * This function returns a pixel size used for each square patch in the grid
   * visualization, with smaller sizes for larger grids to fit the visualization area.
   * If custom dimensions are set, it calculates the optimal patch size to fit within
   * those dimensions.
   *
   * @param {number} gridWidth - The total number of grid patches along the x-axis.
   * @param {number} gridHeight - The total number of grid patches along the y-axis.
   * @returns {number} - The pixel size for each patch.
   */
  _getPatchPixels(gridWidth, gridHeight) {
    const self = this;

    if (self._customDimensions) {
      const availableWidth = self._customDimensions.getWidth() - gridWidth;
      const availableHeight = self._customDimensions.getHeight() - gridHeight;
      
      const maxPatchWidth = Math.floor(availableWidth / gridWidth);
      const maxPatchHeight = Math.floor(availableHeight / gridHeight);
      
      return Math.max(1, Math.min(maxPatchWidth, maxPatchHeight));
    }

    const largestAxis = Math.max(gridWidth, gridHeight);

    if (largestAxis <= 20) {
      return 25;
    } else if (largestAxis <= 50) {
      return 20;
    } else if (largestAxis <= 100) {
      return 15;
    } else if (largestAxis <= 200) {
      return 10;
    } else if (largestAxis <= 500) {
      return 5;
    } else {
      return 3;
    }
  }

  /**
   * Show a message which should be displayed when the user is not hovering over any boxes.
   *
   * @param {number} timestep - The timestep for which a value is being displayed.
   */
  _showIdleMessage(timestep) {
    const self = this;
    self._infoSelection.innerHTML = `Showing results at time of ${timestep}.`;
  }

  /**
   * Show information about a single patch.
   *
   * @param {}
   */
  _showInfoMessage(x, y, timestep, value) {
    const self = this;
    const rounded = value.toFixed(2);
    self._infoSelection.innerHTML = `Patch ${x}, ${y} has value ${rounded} at time ${timestep}.`;
  }

}


/**
 * Class representing the dimensions of a map visualization in pixels.
 */
class MapDimensions {

  /**
   * Creates a new map dimensions object.
   * 
   * @param {number} width - The width of the map in pixels.
   * @param {number} height - The height of the map in pixels.
   */
  constructor(width, height) {
    const self = this;
    self._width = width;
    self._height = height;
  }

  /**
   * Gets the width of the map.
   * 
   * @returns {number} The width in pixels.
   */
  getWidth() {
    const self = this;
    return self._width;
  }

  /**
   * Gets the height of the map.
   * 
   * @returns {number} The height in pixels.
   */
  getHeight() {
    const self = this;
    return self._height;
  }
  
}


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
   * @param {number} width - Default width in pixels.
   * @param {number} height - Default height in pixels.
   */
  setDefaultDimensions(width, height) {
    const self = this;
    self._widthInput.value = width;
    self._heightInput.value = height;
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


export {GridPresenter, ScrubPresenter, MapConfigPresenter, MapDimensions};