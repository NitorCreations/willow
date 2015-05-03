describe("Tests for window service", function() {
  var service ,sandbox, contextFake, openSpy;

  beforeEach(function() {
    contextFake = new Box.TestServiceProvider();
    service = Box.Application.getServiceForTest('window', contextFake);
    sandbox = sinon.sandbox.create();
    openSpy = sinon.spy(window, "open");
  });

  afterEach(function() {
    sandbox.verifyAndRestore();
    window.open.restore();
  });

  it('open terminal service opens new tab', function() {
    service.openTerminalToHost("pasi", "test-host");
    sinon.assert.calledWith(openSpy, "shell.html?user=pasi&host=test-host", "_blank")
  });

  it('open alerts opens alert window', function() {
    service.openAlerts();
    sinon.assert.calledWith(openSpy, "alerts.html", "index-alerts")
  });

  it('open radiator opens existing radiator tab', function() {
    service.openRadiatorForHost("test-host");
    sinon.assert.calledWith(openSpy, "radiator.html?host=test-host", "radiator-test-host")
  });
});