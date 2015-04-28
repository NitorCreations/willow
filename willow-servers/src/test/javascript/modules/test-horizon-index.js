describe("Tests for horizon-index module", function() {
  var module, sandbox, contextFake, windowSvc;

  beforeEach(function() {
    windowSvc = {
      open: function(url, target) {}
    };
    contextFake = new Box.TestServiceProvider({
      'window-service': windowSvc
    });
    module = Box.Application.getModuleForTest('horizon-index', contextFake);
    module.init();
    sandbox = sinon.sandbox.create();
  });

  afterEach(function() {
    module.destroy();
    sandbox.verifyAndRestore();
  });
  it('Clicking an element with "start-terminal" type should open the shell window', function() {
    sandbox.mock(windowSvc).expects('open').once().withExactArgs("shell.html?user=pasi&host=test", "_blank");
    var target = $(' <svg data-type="start-terminal" data-host="test"></svg>')[0];
    var event = $.Event('click', {
      target: target
    });
    module.onclick(event, target, 'start-terminal');
  });
  it('Clicking an element with "to-radiator"" type should open the radiator window', function() {
    sandbox.mock(windowSvc).expects('open').once().withExactArgs("radiator.html?host=test", "radiator-test");
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