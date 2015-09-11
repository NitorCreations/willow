var env = require('../env');

var name = "navigate-to-alerts";

casper.test.begin('navigate to alerts page', 5, function(test) {

  env.init();

  casper.thenOpen(env.root, function() {
    test.assertExists(env.cpuLink, "common navigation is initialized");
    test.assertVisible(env.alertsLink);
  });

  env.waitForAndClick(env.alertsLink, name, env.defaultTimeOut);

  casper.waitForPopup(env.root + "/alerts.html", function() {
    test.assertEquals(this.popups.length, 1);
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.withPopup(env.root + "/alerts.html", function() {
    casper.waitUntilVisible("tr", function() {
      test.assertVisible("thead");
      test.assertVisible("tr");
    }, env.screencapFailure(name), env.defaultTimeOut);
  });

  casper.then(function() {
    env.writeCoverage(this, name);
  });

  casper.run(function() {
    casper.mainPage.close();
    test.done();
  });
});
