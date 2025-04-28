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


export {ScrubPresenter};
