var env = require('../env');

var name = "graph-to-existing-custom-radiator";
var existingRadiatorName = "existing-test-radiator" + Math.floor((Math.random() * 1000) + 1);

casper.test.begin('User adds graph to existing radiator', function(test) {

  env.init();

  env.clearLocalStorage();

  casper.thenOpen(env.root, function() {
    test.assertExists(env.cpuLink, "common link 'cpu' visible (sanity check)");
  });

  env.waitForAndClick(env.toCustomRadiatorLink, name, env.defaultTimeOut);

  casper.waitUntilVisible(env.customRadiatorDialog.modal, function() {
    this.sendKeys(env.customRadiatorDialog.newNameField, existingRadiatorName);
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.thenClick(env.customRadiatorDialog.createNewButton, function() {
    test.assertNotVisible(env.customRadiatorDialog.modal, "hides dialog after click");
  });

  casper.waitForPopup(env.root + "/radiator.html#name=" + existingRadiatorName, function() {
    test.assertEquals(this.popups.length, 1, "only one radiator is opened");
  });

  casper.thenClick(env.hostLink, function() {});

  casper.waitForPopup(env.root + "/radiator.html#host=");

  casper.withPopup(env.root + "/radiator.html#host=", function() {
    env.waitForAndClick(env.graph("heap-graph").addToRadiator, name, env.defaultTimeOut);

    casper.waitUntilVisible(env.customRadiatorDialog.modal, function() {
      test.assertExists(env.customRadiatorDialog.existingRadiatorLink(existingRadiatorName),
          "radiator exists in the available radiator list");
    });

    casper.thenClick(env.customRadiatorDialog.existingRadiatorLink(existingRadiatorName), function() {
      test.assertNotVisible(env.customRadiatorDialog.modal, "hides dialog after click");
    });
  });

  casper.waitForPopup(env.root + "/radiator.html#name=" + existingRadiatorName, function() {
    test.assertEquals(this.popups.length, 2, "only one radiator is opened (plus host radiator)");
  });

  //TODO sami-airaksinen 8/28/15 : reload doesn't work properly with popups
  casper.thenOpen(env.root + "/radiator.html#name=" + existingRadiatorName + "&timescale=10800", function() {
    test.assertUrlMatch(env.root + "/radiator.html#name=" + existingRadiatorName + "&timescale=10800",
      "creating new radiator should open new tab");
    test.assertTitleMatch(new RegExp("^Willow - "+ existingRadiatorName));
    test.assertExists('div[data-module=heap-graph]',
      "filesystem graph should be present in the radiator now");
    test.assertExists('div[data-module=horizon-graph] > div[data-metric=cpu]',
      "cpu horizon graph should be present in the radiator now");
  });

  casper.run(function() {
    env.writeCoverage(this, name);
    casper.mainPage.close();
    test.done();
  });
});