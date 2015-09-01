var env = require('../env');
var fs = require('fs');
env.init();
var name = "graph-to-new-custom-radiator";

casper.test.begin('User adds graph to new radiator', 6, function(test) {

  var newRadiatorName = "new-test-radiator" + Math.floor((Math.random() * 100000) + 1);

  var fieldSelector = {};
  fieldSelector[env.customRadiatorDialog.newNameField] = newRadiatorName;

  env.clearLocalStorage();

  casper.start(env.root + "/#metric=mem&timescale=10800", function() {
    test.assertExists(env.memLink);
  });

  env.waitForAndClick(env.toCustomRadiatorLink, name, env.defaultTimeOut);

  casper.waitUntilVisible(env.customRadiatorDialog.modal, function() {
    this.sendKeys(env.customRadiatorDialog.newNameField, newRadiatorName);
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.thenClick(env.customRadiatorDialog.createNewButton, function() {
    test.assertNotVisible(env.customRadiatorDialog.modal);
  });

  casper.waitForPopup(env.root + "/radiator.html#name=" + newRadiatorName, function() {
    test.assertEquals(this.popups.length, 1);
  });

  casper.withPopup(env.root + "/radiator.html#name=" + newRadiatorName + "&timescale=10800", function() {
    test.assertUrlMatch(env.root + "/radiator.html#name=" + newRadiatorName + "&timescale=10800",
        "creating new radiator should open new tab");
    test.assertTitleMatch(/^Willow - MEM for/); //TODO sami-airaksinen 8/28/15 : fix real issue with title setting
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