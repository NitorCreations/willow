var env = require('../env');
var fs = require('fs');
env.init();
var name = "graph-is-added-to-custom-radiator";
var waitTimeout = 10000;

casper.test.begin('User adds graph to new radiator', 6, function(test) {

  var newRadiatorName = "new-test-radiator" + Math.floor((Math.random() * 1000) + 1);

  var fieldSelector = {};
  fieldSelector[env.customRadiatorDialog.newNameField] = newRadiatorName;

  //FIXME sami-airaksinen: how I clear localstorage?
  casper.evaluate(function() {
    localStorage.clear();
  }, {});

  casper.start(env.root + "/#metric=mem&timescale=10800", function() {
    test.assertExists(env.memLink);
  });

  env.waitForAndClick(env.toCustomRadiatorLink, name, waitTimeout);

  casper.waitUntilVisible(env.customRadiatorDialog.modal, function() {
    this.sendKeys(env.customRadiatorDialog.newNameField, newRadiatorName);
  }, env.screencapFailure(name), waitTimeout);

  casper.thenClick(env.customRadiatorDialog.createNewButton, function() {
    test.assertNotVisible(env.customRadiatorDialog.modal);
  });

  casper.waitForPopup(env.root + "/radiator.html#name=" + newRadiatorName, function() {
    test.assertEquals(this.popups.length, 1);
  });

  casper.withPopup(env.root + "/radiator.html#name=" + newRadiatorName + "&timescale=10800", function() {
    test.assertUrlMatch(env.root + "/radiator.html#name=" + newRadiatorName + "&timescale=10800",
      "creating new radiator should open new tab");
    test.assertTitleMatch(/^Willow - MEM for/);
    test.assertExists('div[data-module=horizon-graph] > div[data-metric=mem]',
      "mem horizon graph should be present in the radiator now");
  });

  casper.then(function() {
    env.writeCoverage(this, name);
  });

  casper.on('error', function() {
    this.capture("failed-on-error-screenshot-" + name + ".png")
  });

  casper.run(function() { test.done(); });
});
