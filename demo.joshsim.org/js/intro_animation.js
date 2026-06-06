/**
 * Logic to run landing page.
 *
 * @license BSD-3-Clause
 */


/**
 * Animate a tiled background into an svg.
 *
 * Fills the svg with a grid of squares (~50 columns wide) sized to the svg's parent element. Each
 * square starts white at 0 opacity, then transitions (random 0–5s delay, 1s duration) to a random
 * opacity between 0% and 40%, giving a subtle shimmering tile texture behind the content.
 *
 * @param {string} selector - CSS selector for the svg to fill. No-op if it is not present.
 */
function tileBackground(selector) {
  const svg = d3.select(selector);
  if (svg.empty()) {
    return;
  }

  const container = svg.node().parentNode;
  const width = container.offsetWidth;
  const height = container.offsetHeight;
  if (!width || !height) {
    return;
  }

  svg.attr("width", width)
     .attr("height", height);

  const squareSize = width / 50;
  const numRows = Math.ceil(height / squareSize);
  const numCols = Math.ceil(width / squareSize);

  const squares = [];
  for (let row = 0; row < numRows; row++) {
    for (let col = 0; col < numCols; col++) {
      squares.push({
        x: col * squareSize,
        y: row * squareSize,
        size: squareSize
      });
    }
  }

  svg.selectAll("rect")
     .data(squares)
     .enter()
     .append("rect")
     .attr("x", d => d.x)
     .attr("y", d => d.y)
     .attr("width", d => d.size * 0.9)
     .attr("height", d => d.size * 0.9)
     .attr("fill", "#ffffff")
     .attr("opacity", 0)
     .transition()
     .delay(() => Math.random() * 5000)
     .duration(1000)
     .attr("opacity", () => Math.random() * 0.4);
}


/**
 * Animate the header tile background (kept for backwards compatibility).
 */
function runIntroAnimation() {
  tileBackground(".header-intro");
}


/**
 * Run page introductory commands and setup.
 *
 * Tiles the header banner and, where present, the demo call-out box. Selectors that match nothing
 * are skipped, so this is safe on pages that have only one (or neither).
 */
function main() {
  setTimeout(() => {
    tileBackground(".header-intro");
    tileBackground(".demo-callout-intro");
  }, 500);
}


main();
