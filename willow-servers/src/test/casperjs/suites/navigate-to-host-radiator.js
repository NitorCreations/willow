var env = require('../env');
var fs = require('fs');
env.init();
var name = "navigate-to-radiator";
var waitTimeout = 2000;

casper.test.begin('navigate to host page', 1, function(test) {

  casper.start(env.root + "/#metric=cpu&timescale=10800", function() {
    test.assertExists(env.cpuLink, "common navigation is initialized");
  });

  //FIXME resolve issue with filesystem-graph.js
  // TypeError: 'undefined' is not a function (evaluating 'nv.utils.optionsFunc.bind(chart)')

  //http://localhost:5120/scripts/lib/nv.d3.js:11422
  //http://localhost:5120/scripts/lib/nv.d3.js:5128
  //http://localhost:5120/scripts/lib/nv.d3.js:5402
  //http://localhost:5120/scripts/modules/graphs/heap-graph.js:28
  //http://localhost:5120/scripts/lib/nv.d3.js:65

  //casper.waitUntilVisible(env.hostLink, function() {
  //  this.click(env.hostLink);
  //}, env.screencapFailure, waitTimeout);
  //
  //casper.waitForPopup(/radiator\.html/, function() {
  //  //FIXME should assert something actual popup count..?
  //}, env.screencapFailure, waitTimeout);
  //
  //casper.withPopup(/radiator\.html/, function() {
  //  this.capture('debugging-in-popup.png');
  //  //FIXME should assert something actual element presence here
  //});
  //
  //casper.waitUntilVisible(env.heapDiv, function() {
  //  env.writeCoverage(this, name);
  //}, env.screencapFailure, waitTimeout);

  casper.run(function() { test.done(); });
});
