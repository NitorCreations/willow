var root = casper.cli.options.home;
casper.options.viewportSize = { width: 1920, height: 1080 };
casper.start(root);
casper.setHttpAuth('admin', 'admin');
casper.viewport(1920, 1080);
var fs = require('fs');

var cpuLink = { type: 'xpath', path: '//a[@id="cpu"]' };
var hostLink = { type: 'xpath', path: '//a[@data-host="integrationtest"]' };
var heapDiv = '#mod-heap-graph-1';
var getCoverage = function() { return window.__coverage__; };
var writeCoverage = function(cspr, covnum) {
  var coverage = cspr.evaluate(getCoverage);
  if (coverage) {
    fs.write("target/js-coverage/test" + covnum + ".json",
      JSON.stringify(coverage), 'w');
  }
}
casper.test.begin('navigate to host page', 2, function(test) {
  casper.start(root + "/#metric=cpu&timescale=10800", function() {
    test.assertExists(cpuLink);
    this.waitUntilVisible(hostLink,
      function() {
        this.click(hostLink);
        casper.waitForPopup(/radiator\.html/);
        casper.withPopup(/radiator\.html/, function() {
          this.waitUntilVisible(heapDiv, function() {
            this.capture("screenshot.png");
            writeCoverage(this, 1);
            test.pass('integrationtest graph visible');
          });
        });
        writeCoverage(casper, 2);
      },
      function() {
        this.capture("failed-screenshot.png");
      }, 15000);
  }).run(function() {
    test.done();
  });
});
