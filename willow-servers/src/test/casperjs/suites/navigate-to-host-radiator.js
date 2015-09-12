var env = require('../env');

var name = "navigate-to-radiator";
var waitTimeout = 10000;

casper.test.begin('navigate to host page', 4, function(test) {

  env.init();

  casper.thenOpen(env.root, function() {
    test.assertExists(env.cpuLink, "common navigation is initialized");
  });

  env.waitForAndClick(env.memLink, name, waitTimeout);

  env.waitForAndClick(env.hostLink, name, waitTimeout);

  casper.waitForPopup(env.root + "/radiator.html#host=", function() {
    test.assertEquals(this.popups.length, 1);
  }, env.screencapFailure(name), waitTimeout);

  casper.withPopup(env.root + "/radiator.html#host=", function() {
    casper.waitUntilVisible(env.connDiv, function() {
      env.assertHorizonGraph(env.connDiv);
    }, env.screencapFailure(name), waitTimeout);

    casper.evaluate(env.selectTimeScale, 2);

    casper.then(function() {
      test.assertUrlMatch(/timescale=43200/, "timescale parameter has changed to match 12h")
    });
  });

  casper.then(function() {
    env.writeCoverage(this, name);
  });

  casper.run(function() {
    casper.mainPage.close();
    test.done();
  });
});
