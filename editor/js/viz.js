/**
 * Logic to run the visualization components in the data display.
 *
 * @license BSD-3-Clause
 */

const SCRUB_RIGHT_PAD = 50;
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
   */
  constructor(selection) {
    const self = this;
    self._root = selection;
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
    
    self._svgSelection.html("");
    
    const svgBounds = self._svg.getBoundingClientRect();
    const width = svgBounds.width - SCRUB_RIGHT_PAD;
    const height = svgBounds.height - SCRUB_BOTTOM_PAD;
    
    const minTimestep = summarized.getMinTimestep();
    const maxTimestep = summarized.getMaxTimestep();
    const numSteps = maxTimestep - minTimestep + 1;
    
    const groupWidth = width / numSteps;
    
    // Find min/max values for scaling
    const values = [];
    for (let step = minTimestep; step <= maxTimestep; step++) {
      values.push(summarized.getTimestepValue(step));
    }
    const minValue = Math.min(...values);
    const maxValue = Math.max(...values);
    
    const createBody = () => {
      const timesteps = Array.from({length: numSteps}, (_, i) => minTimestep + i);
      
      const xScale = d3.scaleBand()
        .domain(timesteps)
        .range([0, width])
        .padding(0);
      
      const groups = self._svgSelection.selectAll("g")
        .data(timesteps)
        .enter()
        .append("g")
        .attr("transform", d => `translate(${xScale(d)},0)`);
      
      groups.append("rect")
        .attr("class", "display-bar")
        .attr("x", 1)
        .attr("y", d => {
          const value = summarized.getTimestepValue(d);
          const barHeight = (value - minValue) / (maxValue - minValue) * height;
          return height - barHeight;
        })
        .attr("width", xScale.bandwidth() - 2)
        .attr("height", d => {
          const value = summarized.getTimestepValue(d);
          return (value - minValue) / (maxValue - minValue) * height;
        });
      
      groups.append("rect")
        .attr("class", "pointer-target")
        .attr("x", 0)
        .attr("y", 0)
        .attr("width", xScale.bandwidth())
        .attr("height", height)
        .style("opacity", 0)
        .on("click", (_, d) => self._onStepSelect(d));
    };
    
    const addVerticalAxis = () => {
      self._svgSelection.append("text")
        .attr("x", width + 5)
        .attr("y", 15)
        .text(maxValue.toFixed(2));
        
      self._svgSelection.append("text")
        .attr("x", width + 5)
        .attr("y", height - 5)
       .text(minValue.toFixed(2));
    };
    
    const addHorizontalAxis = () => {
      self._svgSelection.append("text")
        .attr("x", 5)
        .attr("y", height + 15)
        .text(minTimestep);
        
      self._svgSelection.append("text")
        .attr("x", width - 20)
        .attr("y", height + 15)
        .text(maxTimestep);
    };

    createBody();
    addVerticalAxis();
    addHorizontalAxis();

    const noTimestep = self._timestepSelected === null;
    const timestepTooSmall = self._timestepSelected < minTimestep;
    const timestepTooLarge = self._timestepSelected > maxTimestep;
    const needTimestepUpdate = noTimestep || timestepTooSmall || timestepTooLarge;
    if (needTimestepUpdate) {
      self._onStepSelect(maxTimestep);
    }
  }

  _onStepSelect(timestep) {
    
  }

}
