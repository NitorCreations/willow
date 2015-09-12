var env = require('../env');

var name = "graph-to-existing-custom-radiator";
var existingRadiatorName = "existing-test-radiator" + Math.floor((Math.random() * 1000) + 1);

casper.test.begin('User adds graph to existing radiator', function(test) {

  env.init();

  env.clearLocalStorage();

  casper.thenOpen(env.root, function() {
    test.assertExists(env.cpuLink, "common link 'cpu' visible (sanity check)");
  });

  env.waitForAndClick(env.toCustomRadiatorLink, name, env.defaultTimeOut);

  casper.waitUntilVisible(env.customRadiatorDialog.modal, function() {
    this.sendKeys(env.customRadiatorDialog.newNameField, existingRadiatorName);
  }, env.screencapFailure(name), env.defaultTimeOut);

  casper.thenClick(env.customRadiatorDialog.createNewButton, function() {
    test.assertNotVisible(env.customRadiatorDialog.modal, "hides dialog after click");
  });

  casper.waitForPopup(env.root + "/radiator.html#name=" + existingRadiatorName, function() {
    test.assertEquals(this.popups.length, 1, "only one radiator is opened");
  });

  casper.thenClick(env.hostLink, function() {});

  casper.waitForPopup(env.root + "/radiator.html#host=");

  casper.withPopup(env.root + "/radiator.html#host=", function() {
    env.waitForAndClick(env.graph("heap-graph").addToRadiator, name, env.defaultTimeOut);

    casper.waitUntilVisible(env.customRadiatorDialog.modal, function() {
      test.assertExists(env.customRadiatorDialog.existingRadiatorLink(existingRadiatorName),
          "radiator exists in the available radiator list");
    });

    casper.thenClick(env.customRadiatorDialog.existingRadiatorLink(existingRadiatorName), function() {
      test.assertNotVisible(env.customRadiatorDialog.modal, "hides dialog after click");
    });
  });

  casper.waitForPopup(env.root + "/radiator.html#name=" + existingRadiatorName, function() {
    test.assertEquals(this.popups.length, 2, "only one radiator is opened (plus host radiator)");
  });

  //TODO sami-airaksinen 8/28/15 : reload doesn't work properly with popups
  casper.thenOpen(env.root + "/radiator.html#name=" + existingRadiatorName + "&timescale=10800", function() {
    test.assertUrlMatch(env.root + "/radiator.html#name=" + existingRadiatorName + "&timescale=10800",
      "creating new radiator should open new tab");
    test.assertTitleMatch(new RegExp("^Willow - "+ existingRadiatorName));
    test.assertExists('div[data-module=heap-graph]',
      "filesystem graph should be present in the radiator now");
    test.assertExists('div[data-module=horizon-graph] > div[data-metric=cpu]',
      "cpu horizon graph should be present in the radiator now");
    assertGraphOrdering("horizon-graph", "heap-graph");
  });

/*
  Ordering graphs by dragging can't be tested due to limitation in the phatomjs api (1.9).
  see: http://phantomjs.org/api/webpage/method/send-event.html

  casper.then(function() {
    mouse.down(env.graph("horizon-graph").reOrderGraph);
    this.mouse.move(env.graph("heap-graph").reOrderGraph);
  });

  casper.then(function() {
    this.mouse.up(env.graph("heap-graph").reOrderGraph);
  });

  casper.then(function() {
    assertGraphOrdering("heap-graph", "horizon-graph");
  });


  casper.thenOpen(env.root + "/radiator.html#name=" + existingRadiatorName + "&timescale=28800", function() {
    assertGraphOrdering("heap-graph", "horizon-graph");
  });
*/

  casper.thenClick(env.graph("heap-graph").removeFromRadiator, function() {
    test.assertVisible(env.graph("horizon-graph").module, "horizon graph is present");
    test.assertNotVisible(env.graph("heap-graph").module, "heap graph is removed");
  });

  casper.thenOpen(env.root + "/radiator.html#name=" + existingRadiatorName + "&timescale=43200", function() {
    test.assertVisible(env.graph("horizon-graph").module, "horizon graph is present");
    test.assertNotVisible(env.graph("heap-graph").module, "heap graph is not present");
  });

  casper.then(function() {
    env.writeCoverage(this, name);
  });

  casper.run(function() {
    casper.mainPage.close();
    test.done();
  });

  function assertGraphOrdering(graph1, graph2) {
    test.assertVisible(env.graph(graph1).module, "graph " + graph1 + " is present");
    test.assertVisible(env.graph(graph2).module, "graph " + graph2 + " is present");
    test.assertElementCount('div[data-module*=-graph]',2, "only 2 graphs present");
    test.assertEval(function(graph1, graph2) {
      var graphs = document.querySelectorAll('div[data-module$=-graph]');
      return graphs[0].attributes["data-module"].value === graph1 &&
        graphs[1].attributes["data-module"].value === graph2
    }, "ordering should be expected [" + graph1 + ","+ graph2 +"]", [graph1, graph2]);
  }
});

