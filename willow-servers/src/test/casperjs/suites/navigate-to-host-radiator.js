var env = require('../env');
var fs = require('fs');
env.init();
var name = "navigate-to-radiator";
var waitTimeout = 10000;

casper.test.begin('navigate to host page', 2, function(test) {

  casper.start(env.root + "/#metric=cpu&timescale=10800", function() {
    test.assertExists(env.cpuLink, "common navigation is initialized");
  });

  env.waitForAndClick(env.hostLink, name, waitTimeout);

  casper.waitForPopup(env.root + "/radiator.html", function() {
  }, env.screencapFailure(name), waitTimeout);

  casper.withPopup(env.root + "/radiator.html", function() {
    casper.waitUntilVisible(env.connDiv, function() {
      env.assertHorizonGraph(env.connDiv);
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
