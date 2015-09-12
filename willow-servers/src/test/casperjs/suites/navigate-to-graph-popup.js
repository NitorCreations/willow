var env = require('../env');

var name = "navigate-to-graph-popup";

var horizonGraph = env.graph('horizon-graph');

casper.test.begin('navigate to graph popup', 7, function(test) {

  env.init();

  casper.thenOpen(env.root, function() {
    test.assertExists(env.cpuLink, "common navigation is initialized");
    test.assertVisible(horizonGraph.openToPopup, "to popup link is visible");
    test.assertTitleMatch(/^Willow monitoring - Main/);
  });

  env.waitForAndClick(horizonGraph.openToPopup, name, env.defaultTimeOut);

  casper.waitForPopup(env.root + "/radiator.html#name=", function() {
    test.assertEquals(this.popups.length, 1, "should open a popup with single graph");
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.withPopup(env.root + "/radiator.html#name=", function() {
    test.assertTitleMatch(/^Willow - CPU for/, "title is specific to single graph");
    test.assertElementCount("div[data-module=horizon-graph]", 1, "single horizon graph visible");
    test.assertElementCount("svg.graph", 0, "no other graphs present");
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.then(function() {
    env.writeCoverage(this, name);
  });

  casper.run(function() {
    casper.mainPage.close();
    test.done();
  });
});
