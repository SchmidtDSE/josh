
/**
 * Animate the header.
 *
 * Animate the svg in the header such that there is a grid of squares created with 1000 squares
 * wide. The size of the square depends on the size of the header. Each square is given a color of
 * white but an opacity of 0. Then, in a transition with a random delay between 0 to 2 seconds with
 * a duration of 1 second, the opacity is set randomly between 0% and 20%.
 */
function runIntroAnimation() {
  const svg = d3.select('.header-intro');
  const header = document.querySelector('header');
  const width = header.offsetWidth;
  const height = header.offsetHeight;
  
  svg.attr('width', width)
     .attr('height', height);

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

  svg.selectAll('rect')
     .data(squares)
     .enter()
     .append('rect')
     .attr('x', d => d.x)
     .attr('y', d => d.y)
     .attr('width', d => d.size * 0.9)
     .attr('height', d => d.size * 0.9)
     .attr('fill', '#ffffff')
     .attr('opacity', 0)
     .transition()
     .delay(() => Math.random() * 2000)
     .duration(1000)
     .attr('opacity', () => Math.random() * 0.2);
}

/**
 * Run page introductory commands and setup
 */
function main() {
  setTimeout(() => runIntroAnimation(), 500);
}
