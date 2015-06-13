var env = require('../env');
var fs = require('fs');
env.init();
var name = "index-horizons";

casper.test.begin('navigate to host page', 6, function(test) {
  casper.start(env.root + "/#metric=cpu&timescale=10800", function() {
    test.assertExists(env.cpuLink);
    test.assertExists(env.memLink);
    test.assertExists(env.netLink);
    test.assertExists(env.diskLink);
    test.assertExists(env.connLink);
    this.waitUntilVisible(env.hostLink,
      function() {
        this.click(env.memLink);
        this.waitUntilVisible(env.memDiv, function() {
          this.click(env.netLink);
          this.waitUntilVisible(env.netDiv, function() {
            this.click(env.diskLink);
            this.waitUntilVisible(env.diskDiv, function() {
              this.click(env.connLink);
              this.waitUntilVisible(env.connDiv, function() {
                env.writeCoverage(casper, name);
                test.pass('integrationtest graph visible');
              });
            });
          });
        });
      },
      function() {
        this.capture("failed-screenshot-" + name + ".png");
      }, 15000);
  }).run(function() {
    test.done();
  });
});
