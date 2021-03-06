describe("Tests for window service", function() {
  var service, sandbox, contextFake;

  beforeEach(function() {
    d3 = { time: { 'format': function() {} } };
    contextFake = new Box.TestServiceProvider({
      'd3': d3
    });
    service = Box.Application.getServiceForTest('window', contextFake);
    sandbox = sinon.sandbox.create();
  });

  afterEach(function() {
    window.open.restore();
    sandbox.verifyAndRestore();
  });

  it('open terminal service opens new tab', function() {
    sandbox.mock(window).expects("open").withExactArgs("shell.html?user=pasi&host=test-host", "_blank");
    service.openTerminalToHost("pasi", "test-host");
  });

  it('open alerts opens alert window', function() {
    sandbox.mock(window).expects("open").withExactArgs("alerts.html", "index-alerts");
    service.openAlerts();
  });

  it('open radiator opens existing radiator tab', function() {
    sandbox.mock(window).expects("open").withExactArgs("radiator.html#host=test-host", "hostradiator-test-host");
    service.openRadiatorForHost("test-host");
  });
});