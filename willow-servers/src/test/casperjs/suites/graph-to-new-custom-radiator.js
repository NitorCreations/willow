//TODO some of it should go into includes and some of to the pre
var env = require('../env');

var name = "graph-to-new-custom-radiator";
var newRadiatorName = "new-test-radiator" + Math.floor((Math.random() * 100000) + 1);
var fieldSelector = {};
fieldSelector[env.customRadiatorDialog.newNameField] = newRadiatorName;

casper.test.begin('User adds graph to new radiator', 6, function(test) {

  env.init();

  env.clearLocalStorage();

  casper.thenOpen(env.root, function() {
    test.assertExists(env.memLink, "common link 'mem' visible (sanity check)");
  });

  env.waitForAndClick(env.memLink, name, env.defaultTimeOut);

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

  casper.run(function() {
    casper.mainPage.close();
    test.done();
  });
});