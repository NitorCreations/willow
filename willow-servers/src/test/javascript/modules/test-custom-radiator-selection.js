describe("Tests for custom radiator ", function() {
  var module, sandbox, windowService, store,
    contextFake, testCharConfig;

  var testedModuleId = 'custom-radiator-selection';

  //TODO needs better mocking for jquery, with stubbing and mocking via sinon...
  var jQueryApi = {
    dialog: function() {},
    val: function() { return "test-new-radiator-name"; }
  };

  beforeEach(function() {
    d3 = {
      text: function() { return this; },
      append: function() { return this; },
      enter: function() { return this; },
      data: function() { return this; },
      selectAll: function() { return this; },
      select: function() { return this; },
      attr: function() { return this; },
      classed: function() { return this; },
      remove: function() { return this; }
    };
    jQuery = function() {
      return jQueryApi;
    };
    sandbox = sinon.sandbox.create();
    windowService = {
      sendGraphToRadiator: function() {}
    };
    testCharConfig = {
      metric: "cpu",
      host: "test-host",
      instanceTag: 'test-instance'
    };
    store = {
      customRadiators: {
        appendConfig: function() {},
        listAvailableRadiators: function() { return ["existing-radiator"]; }
      }
    };
    contextFake = new Box.TestServiceProvider({
      'window': windowService,
      'configuration-store': store
    });
    module = Box.Application.getModuleForTest(testedModuleId, contextFake);
    module.init();
  });

  it('broadcasted message renders dialog with latest radiators available', function() {
    sandbox.mock(store.customRadiators).expects('listAvailableRadiators').once();
    sandbox.mock(jQueryApi).expects('dialog').once();

    //TODO figure out how mock chaining api... not so trivial with sinon mocks after all...
    // something like this
    // var mockd3 = sandbox.mock(d3);
    // mockd3.expects('attr').once().withExactArgs("data-type", "append-to-radiator");
    // mockd3.expects('attr').once().withExactArgs("data-radiator-id", "existing-radiator");
    module.onmessage("open-radiator-list", testCharConfig);
  });

  it('creating a new radiator stores the configuration with user given radiator name', function() {
    sandbox.mock(store.customRadiators).expects('appendConfig').withExactArgs("test-new-radiator-name", testCharConfig).once();
    sandbox.mock(windowService).expects('sendGraphToRadiator').withExactArgs("test-new-radiator-name", testCharConfig).once();
    sandbox.mock(jQueryApi).expects('dialog').withExactArgs("destroy").once();
    module.setGraphConfig(testCharConfig);
    module.onclick({}, {}, "create-new-radiator");
  });

  it('selecting existing radiator stores the configuration with selected radiator name', function() {
    sandbox.mock(store.customRadiators).expects('appendConfig').withExactArgs("existing-radiator", testCharConfig).once();
    sandbox.mock(windowService).expects('sendGraphToRadiator').withExactArgs("existing-radiator", testCharConfig).once();
    sandbox.mock(jQueryApi).expects('dialog').withExactArgs("destroy").once();
    module.setGraphConfig(testCharConfig);
    module.onclick({}, {getAttribute: sandbox.stub().returns("existing-radiator") }, "append-to-radiator");
  });

  it('dialog is closed only if new radiator is created or existing is selected from the list', function() {
    sandbox.mock(jQueryApi).expects('dialog').withExactArgs("destroy").never();
    module.onclick({}, {}, "foo-bar-element-type");
  });

  afterEach(function() {
    module.destroy();
    sandbox.verifyAndRestore();
  });

});