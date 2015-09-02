var env = require('../env');
var fs = require('fs');
env.init();
var name = "navigate-to-alerts";
var waitTimeout = 10000;

casper.test.begin('navigate to host page', 5, function(test) {

  casper.start(env.root, function() {
    test.assertExists(env.cpuLink, "common navigation is initialized");
    test.assertVisible(env.alertsLink);
  });

  env.waitForAndClick(env.alertsLink, name, waitTimeout);

  casper.waitForPopup(env.root + "/alerts.html", function() {
    test.assertEquals(this.popups.length, 1);
  }, env.screencapFailure(name), waitTimeout);

  casper.withPopup(env.root + "/alerts.html", function() {
    casper.waitUntilVisible("tr", function() {
      test.assertVisible("thead");
      test.assertVisible("tr");
    }, env.screencapFailure(name), waitTimeout);
  });

  casper.then(function() {
    env.writeCoverage(this, name);
  });

  casper.on('error', function() {
    this.capture("failed-on-error-screenshot-" + name + ".png")
  });

  casper.run(function() { test.done(); });
});
