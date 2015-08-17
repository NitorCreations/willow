// Test root given on command line as --home
exports.root = casper.cli.options.home

/**********************************************
  Some selectors for links
***********************************************/
exports.cpuLink        = { type: 'xpath', path: '//a[@id="cpu"]' };
exports.memLink        = { type: 'xpath', path: '//a[@id="mem"]' };
exports.netLink        = { type: 'xpath', path: '//a[@id="net"]' };
exports.diskLink       = { type: 'xpath', path: '//a[@id="diskio"]' };
exports.connLink       = { type: 'xpath', path: '//a[@id="tcpinfo"]' };
exports.cpuDiv         = { type: 'xpath', path: '//div[@data-metric="cpu"]'  }
exports.memDiv         = { type: 'xpath', path: '//div[@data-metric="mem"]'  }
exports.netDiv         = { type: 'xpath', path: '//div[@data-metric="net"]'  }
exports.diskDiv        = { type: 'xpath', path: '//div[@data-metric="diskio"]'  }
exports.connDiv        = { type: 'xpath', path: '//div[@data-metric="tcpinfo"]'  }
exports.radiatorNavDiv = { type: 'xpath', path: '//div[@data-module="radiator-controller"]'  }
exports.hostLink       = { type: 'xpath', path: '//a[@data-type="host-radiator"]' };
exports.alertsLink     = 'svg.shape-bell';
exports.heapDiv        = '#mod-heap-graph-1';

exports.toCustomRadiatorLink = "svg[data-type=to-radiator]";
exports.customRadiatorDialog = {
  modal: ".ui-dialog.ui-widget",
  newNameField: "#custom-radiator-list-dialog input[name=radiator-id]",
  createNewButton: "#custom-radiator-list-dialog #create"
};
exports.init = function() {
  casper.options.viewportSize = { width: 1920, height: 1080 };
  casper.start();
  casper.setHttpAuth('admin', 'admin');
  casper.viewport(1920, 1080);
};

exports.writeCoverage = function(cspr, name) {
  var coverage = casper.evaluate(function() {
    return window.__coverage__;
  });
  if (coverage) {
    fs.write("target/js-coverage/test-" + name + ".json",
      JSON.stringify(coverage), 'w');
  }
};

exports.screencapFailure = function(name) {
  return function() {
    this.capture("failed-screenshot-" + name + ".png");
  }
};

exports.assertHorizonGraph = function(elementSelector) {
  casper.test.assertVisible(elementSelector);
};

exports.waitForAndClick = function(selector, name, waitTimeout) {
  casper.waitUntilVisible(selector, function() {
    this.click(selector);
  }, exports.screencapFailure(name), waitTimeout);
};
