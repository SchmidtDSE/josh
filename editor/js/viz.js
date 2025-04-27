/**
 * Logic to run the visualization components in the data display.
 *
 * @license BSD-3-Clause
 */


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
    
    // Clear existing elements
    self._svgSelection.selectAll("*").remove();
    
    const svgBounds = self._svg.getBoundingClientRect();
    const width = svgBounds.width - 50;  // 50px right padding
    const height = svgBounds.height - 20; // 20px bottom padding
    
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
    
    // Create groups and bars
    for (let step = minTimestep; step <= maxTimestep; step++) {
      const value = summarized.getTimestepValue(step);
      const index = step - minTimestep;
      const x = index * groupWidth;
      
      const group = self._svgSelection.append("g")
        .attr("transform", `translate(${x},0)`);
      
      const barHeight = (value - minValue) / (maxValue - minValue) * height;
      
      // Display bar
      group.append("rect")
        .attr("class", "display-bar")
        .attr("x", 1)
        .attr("y", height - barHeight)
        .attr("width", groupWidth - 2)
        .attr("height", barHeight);
      
      // Pointer target
      group.append("rect")
        .attr("class", "pointer-target")
        .attr("x", 0)
        .attr("y", 0)
        .attr("width", groupWidth)
        .attr("height", height)
        .style("opacity", 0)
        .on("click", () => self._onStepSelect(step));
    }
    
    // Add min/max value labels
    self._svgSelection.append("text")
      .attr("x", width + 5)
      .attr("y", 15)
      .text(maxValue.toFixed(2));
      
    self._svgSelection.append("text")
      .attr("x", width + 5)
      .attr("y", height - 5)
      .text(minValue.toFixed(2));
    
    // Add timestep labels  
    self._svgSelection.append("text")
      .attr("x", 5)
      .attr("y", height + 15)
      .text(minTimestep);
      
    self._svgSelection.append("text")
      .attr("x", width - 20)
      .attr("y", height + 15)
      .text(maxTimestep);
    
    // Set initial selection if needed
    if (self._timestepSelected === null || 
        self._timestepSelected < minTimestep || 
        self._timestepSelected > maxTimestep) {
      self._onStepSelect(maxTimestep);
    }
  }

  _onStepSelect(timestep) {
    
  }

}
