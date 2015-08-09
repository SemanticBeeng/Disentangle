// Set the dimensions of the canvas / graph

function plotIt(filename)
{

    var margin = {top: 30, right: 20, bottom: 30, left: 100},
        width = 600 - margin.left - margin.right,
        height = 270 - margin.top - margin.bottom;

    // Set the ranges
    var x = d3.scale.linear().range([0, width]);
    var y = d3.scale.linear().range([height, 0]);

    // Define the axes
    var xAxis = d3.svg.axis().scale(x)
        .orient("bottom").ticks(5);

    var yAxis = d3.svg.axis().scale(y)
        .orient("left").ticks(5);

    // Define the line
    var valueline = d3.svg.line()
        .x(function(d) { return x(d.nodes); })
        .y(function(d) { return y(d.measured); });

    // Adds the svg canvas
    var svg = d3.select("body")
        .append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
        .append("g")
            .attr("transform",
                  "translate(" + margin.left + "," + margin.top + ")");

    // Get the data
    d3.csv(filename, function(error, data) {

        // Scale the range of the data
        x.domain([0, d3.max(data, function(d) { return Number(d.nodes); })]);
    //    y.domain([0, d3.max(data, function(d) { return Number(d.measured); })]);
        y.domain([0, d3.max(data, function(d) { return Number(d.expected); })]);

        // Add the valueline path.
    //    svg.append("path")
    //        .attr("class", "line")
    //        .attr("d", valueline(data));

        // Add the scatterplot of the measurements
        svg.selectAll("dot")
            .data(data)
          .enter().append("circle")
            .attr("r", 3.5)
            .attr("fill","blue")
            .attr("cx", function(d) { return x(d.nodes); })
            .attr("cy", function(d) { return y(d.measured); });

        // Add the scatterplot of expected values
        svg.selectAll("dot")
            .data(data)
          .enter().append("circle")
            .attr("r", 3.5)
            .attr("fill","red")
            .attr("cx", function(d) { return x(d.nodes); })
            .attr("cy", function(d) { return y(d.expected); });

        // Add the X Axis
        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);

        // Add the Y Axis
        svg.append("g")
            .attr("class", "y axis")
            .call(yAxis);
    });
}

//todo next call from scala.js and get it to a file. (Think it'll be a blank white box?)
function plotToPng() {
/*
  var canvas = d3.select('body').append('canvas').node();
  canvas.width = 100;
  canvas.height = 100;
  var ctx = canvas.getContext('2d');
  ctx.drawImage(img, 0, 0);
*/
  var canvas = d3.select('body').append('canvas').node();
  //var ctx = body.getContext('2d');
  var canvasUrl = canvas.toDataURL("image/png");

  console.log(canvasUrl)

  return canvasUrl

}

function dataToPng(filename) {
    plotIt(filename)
    return plotToPng
}