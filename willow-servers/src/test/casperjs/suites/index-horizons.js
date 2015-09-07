var env = require('../env');

var name = "index-horizons";

casper.test.begin('index page metrics navigation links changes graphs', 8, function(test) {

  env.init();

  casper.thenOpen(env.root, function() {
    test.assertExists(env.cpuLink, "cpu link present");
    test.assertExists(env.memLink, "mem link present");
    test.assertExists(env.netLink, "net link present");
    test.assertExists(env.diskLink, "disk link present");
    test.assertExists(env.connLink, "conn link present");
  });

  casper.waitUntilVisible(env.hostLink, function(){},
    env.screencapFailure(name), env.defaultTimeOut);

  casper.thenClick(env.memLink);

  casper.waitUntilVisible(env.memDiv, function() {
    env.assertHorizonGraph(env.memDiv);
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.thenClick(env.memLink);

  casper.waitUntilVisible(env.netDiv, function() {
    env.assertHorizonGraph(env.netDiv);
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.thenClick(env.diskLink);

  casper.waitUntilVisible(env.diskDiv, function() {
    env.assertHorizonGraph(env.diskDiv);
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.thenClick(env.connLink);

  casper.waitUntilVisible(env.connDiv, function() {
    env.assertHorizonGraph(env.connDiv);
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.run(function() {
    env.writeCoverage(this, name);
    casper.mainPage.close();
    test.done();
  });
});
