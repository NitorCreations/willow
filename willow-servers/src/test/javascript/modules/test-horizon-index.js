describe("Tests for horizon-index module", function() {
  var module, sandbox, contextFake, windowSvc;

  beforeEach(function() {
    windowSvc = {
      openRadiatorForHost: function(host) {},
      openTerminalToHost: function(user, host) {},
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
      remove: function() { return this; }
    };
    jquery = {
      attr: function() {}
    }
    contextFake = new Box.TestServiceProvider({
      'window': windowSvc,
      'cubism': cubism,
      'd3': d3,
      'jQuery': jquery
    });
    module = Box.Application.getModuleForTest('horizon-index', contextFake);
    sandbox = sinon.sandbox.create();
    module.init();
  });

  afterEach(function() {
    module.destroy();
    sandbox.verifyAndRestore();
  });

  it('Clicking an element with "start-terminal" type should open the shell window', function() {
    sandbox.mock(windowSvc).expects('openTerminalToHost').once().withExactArgs("pasi", "test");
    var target = $(' <svg data-type="start-terminal" data-host="test"></svg>')[0];
    var event = $.Event('click', {
      target: target
    });
    module.onclick(event, target, 'start-terminal');
  });

  it('Clicking an element with "to-radiator"" type should open the radiator window', function() {
    sandbox.mock(windowSvc).expects('openRadiatorForHost').once().withExactArgs("test");
    var target = $(' <svg data-type="start-terminal" data-host="test"></svg>')[0];
    var event = $.Event('click', {
      target: target
    });
    module.onclick(event, target, 'to-radiator');
  });

  it('Receiving a "metric-changed" message should result calling setMetric', function() {
    sandbox.mock(module).expects('setMetric').once().withExactArgs("cpu");
    module.onmessage('metric-changed', 'cpu');
  });

  it('Receiving a "timescale-changed" message should result calling setTimescale', function() {
    sandbox.mock(module).expects('setTimescale').once().withExactArgs("1000");
    module.onmessage('timescale-changed', '1000');
  });
});