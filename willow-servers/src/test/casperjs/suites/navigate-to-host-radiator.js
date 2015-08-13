var env = require('../env');
var fs = require('fs');
env.init();
var name = "navigate-to-radiator";
var waitTimeout = 2000;

casper.test.begin('navigate to host page', 2, function(test) {

  casper.start(env.root + "/#metric=cpu&timescale=10800", function() {
    test.assertExists(env.cpuLink);
  });

  casper.waitUntilVisible(env.hostLink, function() {
    this.click(env.hostLink);
  }, env.screencapFailure, waitTimeout);

  casper.waitForPopup(/radiator\.html/, function() {
    //FIXME should assert something actual popup count..?
  }, env.screencapFailure, waitTimeout);

  casper.withPopup(/radiator\.html/, function() {
    //FIXME should assert something actual element presence here
  });

  casper.waitUntilVisible(env.heapDiv, function() {
    env.writeCoverage(this, name);
  }, env.screencapFailure, waitTimeout);

  casper.run(function() { test.done(); });
});
