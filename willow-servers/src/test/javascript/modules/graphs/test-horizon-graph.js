describe("Tests for horizon-graph module", function() {
  var module, sandbox, contextFake, windowSvc;

  beforeEach(function() {
    sandbox = sinon.sandbox.create();
    windowSvc = {
      openRadiatorForHost: function(host) {},
      sendGraphToRadiator: function(host) {},
      openTerminalToHost: function(user, host) {},
      getHashVariable: function() {},
      variableStateInHash: function() {},
      setHash: function() {}
    };
    cubism = {
      context: function() { return this; },
      step: function() { return this; },
      size: function() { return this; },
      start: function() { return this; },
      on: function() { return this; },
      horizon: function() { return this; }
    };
    d3 = {
      json: function() { return this; },
      selectAll: function() { return this; },
      call: function() { return this; },
      select: function() { return this; },
      attr: function() { return this; },
      resize: function() { return this; },
      remove: function() { return this; }
    };
    jquery = {
      attr: function() {}
    };
    mockChart = {
      metric: "cpu",
      host: "test-host",
      instanceTag: 'test-instance',
      stop: 10,
      step: 1
    };
    store = {
      readConfiguration: function() { return mockChart; },
      storeConfiguration: function() {}
    };
    cubismGraphs = {
      createMetrics: function() {},
      resetCubismContext: function() {},
      onFocus: function() {},
      removeHorizonGraph: function() {}
    };
    contextFake = new Box.TestServiceProvider({
      'window': windowSvc,
      'cubism-graphs' : cubismGraphs,
      'configuration-store': store,
      'cubism': cubism,
      'd3': d3,
      'jQuery': jquery
    });
    contextFake.getConfig = sandbox.stub().returns({ 'chart': mockChart });
    module = Box.Application.getModuleForTest('horizon-graph', contextFake);
    module.init();
  });

  afterEach(function() {
    localStorage.clear();
    module.destroy();
    sandbox.verifyAndRestore();
  });

  it('Clicking an element with "start-terminal" type should open the shell window', function() {
    sandbox.mock(windowSvc).expects('openTerminalToHost').once().withExactArgs("@admin", "test");
    var target = $(' <svg data-type="start-terminal" data-host="test"></svg>')[0];
    var event = $.Event('click', {
      target: target
    });
    module.onclick(event, target, 'start-terminal');
  });

  it('Clicking an element with "to-radiator" type should broadcast message with graph configuration data as payload', function() {
    sandbox.mock(contextFake).expects('broadcast').once().withExactArgs("open-radiator-list", {
      metric: "cpu",
      host: "test-host",
      instanceTag: 'test-instance',
      stop: 10,
      step: 1
    });
    var target = $(' <svg data-type="to-radiator" data-host="test"></svg>')[0];
    var event = $.Event('click', {
      target: target
    });
    module.onclick(event, target, 'to-radiator');
  });

  it('Clicking an element with "host-radiator"" type should open the radiator window', function() {
    sandbox.mock(windowSvc).expects('openRadiatorForHost').once().withExactArgs("test");
    var target = $(' <a data-type="host-radiator" data-host="test">test</a>')[0];
    var event = $.Event('click', {
      target: target
    });
    module.onclick(event, target, 'host-radiator');
  });

  it('Receiving a "metric-changed" message should result calling setMetric', function() {
    sandbox.mock(module).expects('setMetric').once().withExactArgs("cpu");
    module.onmessage('metric-changed', 'cpu');
  });

  it('Receiving a "reload-graph-configuration" message should reset graph', function() {
    sandbox.mock(module).expects('resetGraph').once();
    module.onmessage('reload-graph-configuration');
  });
});