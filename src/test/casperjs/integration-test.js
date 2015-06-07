var root = casper.cli.options.home;
casper.start(root);
casper.setHttpAuth('admin', 'admin');
var hostLink = { type: 'xpath', path: '//a[@data-host="integrationtest"]' };
var heapDiv = '#mod-heap-graph-1';
casper.viewport(1920, 1080);
casper.test.begin('integrationtest cpu graph is shown', 2, function(test) {
  casper.start(root + "/#metric=cpu&timescale=10800", function() {
    test.assertExists({ type: 'xpath', path: '//a[@id="cpu"]' });
    this.waitUntilVisible(hostLink,
      function() {
        this.click(hostLink);
        casper.waitForPopup(/radiator\.html/);
        casper.withPopup(/radiator\.html/, function() {
          this.waitUntilVisible(heapDiv, function() {
            this.viewport(1920, 1080);
            this.capture("screenshot.png");
            test.pass('integrationtest graph visible');
          });
        });
      },
      function() {
        //this.capture("screenshot.png");
      }, 15000);
  }).run(function() {
    test.done();
  });
});
