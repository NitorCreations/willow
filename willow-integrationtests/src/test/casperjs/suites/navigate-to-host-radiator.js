var env = require('../env');
var fs = require('fs');
env.init();
var name = "navigate-to-radiator";

casper.test.begin('navigate to host page', 2, function(test) {
  casper.start(env.root + "/#metric=cpu&timescale=10800", function() {
    test.assertExists(env.cpuLink);
    this.waitUntilVisible(env.hostLink,
      function() {
        this.click(env.hostLink);
        casper.waitForPopup(/radiator\.html/);
        casper.withPopup(/radiator\.html/, function() {
          this.waitUntilVisible(env.heapDiv, function() {
            env.writeCoverage(this, name + "-1");
            test.pass('integrationtest graph visible');
          });
        });
        env.writeCoverage(casper, name + "-2");
      },
      function() {
        this.capture("failed-screenshot-" + name + ".png");
      }, 15000);
  }).run(function() {
    test.done();
  });
});
