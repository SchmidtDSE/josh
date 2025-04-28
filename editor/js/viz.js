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
    self._infoSelection = selection.querySelector("#grid-viz-info");
    self._svgSelection = selection.querySelector("#grid-viz");
    self._d3SvgSelection = d3.select(self._svgSelection);
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
   */
  render(metadata, summarized, timestep) {
    const self = this;
    
    self._showIdleMessage(timestep);

    // TODO
  }

  /**
   * Show a message which should be displayed when the user is not hovering over any boxes.
   *
   * @param {number} timestep - The timestep for which a value is being displayed.
   */
  _showIdleMessage(timestep) {
    const self = this;
    self._infoSelection.innerHTML = `Showing results at ${timestep}.`;
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


export {ScrubPresenter};
