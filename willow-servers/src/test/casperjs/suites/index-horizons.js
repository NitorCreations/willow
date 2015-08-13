var env = require('../env');
var fs = require('fs');
env.init();
var name = "index-horizons";
var waitTimeout = 2000;

casper.test.begin('index page metrics navigation links changes graphs', 5, function(test) {

  casper.start(env.root + "/#metric=cpu&timescale=10800", function() {
    test.assertExists(env.cpuLink, "cpu link present");
    test.assertExists(env.memLink, "mem link present");
    test.assertExists(env.netLink, "net link present");
    test.assertExists(env.diskLink, "disk link present");
    test.assertExists(env.connLink, "conn link present");
  });

  casper.waitUntilVisible(env.hostLink, function() {
    this.click(env.memLink);
  }, env.screencapFailure(name), waitTimeout);

  casper.waitUntilVisible(env.memDiv, function() {
    assertHorizonGraph(env.memDiv);
    this.click(env.netLink);
  }, env.screencapFailure(name), waitTimeout);

  casper.waitUntilVisible(env.netDiv, function() {
    assertHorizonGraph(env.netDiv);
    this.click(env.diskLink);
  }, env.screencapFailure(name), waitTimeout);

  casper.waitUntilVisible(env.diskDiv, function() {
    assertHorizonGraph(env.diskDiv);
    this.click(env.connLink);
  }, env.screencapFailure(name), waitTimeout);

  casper.waitUntilVisible(env.connDiv, function() {
    assertHorizonGraph(env.connDiv);
  }, env.screencapFailure(name), waitTimeout);

  casper.then(function() {
    env.writeCoverage(this, name);
  });

  casper.on('error', function() {
    this.capture("failed-on-error-screenshot-" + name + ".png")
  });

  casper.run(function() { test.done(); });

  function assertHorizonGraph(elementSelector) {
    //FIXME how to merge selector object of {type, path}?
  }
});
