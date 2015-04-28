describe("Tests for navigation-common behavior", function() {
  var behavior, sandbox, contextFake, windowSvc;

  beforeEach(function() {
    windowSvc = {
      open: function(url, target) {}
    };
    contextFake = new Box.TestServiceProvider({
      'window-service': windowSvc
    });
    behavior = Box.Application.getBehaviorForTest('navigation-common', contextFake);
    behavior.init();
    sandbox = sinon.sandbox.create();
  });
  afterEach(function() {
      behavior.destroy();
      sandbox.verifyAndRestore();
  });
  it('Clicking an element with "alert" type should open the alert window', function() {
    sandbox.mock(windowSvc).expects('open').once().withExactArgs("alerts.html", "index-alerts");
    var target = $(' <svg data-type="alerts"></svg>')[0];
    var event = $.Event('click', {
      target: target
    });
    behavior.onclick(event, target, 'alerts');
  });
  it('Changeing time scale should send "timescale-changed" message', function() {
    sandbox.mock(contextFake).expects('broadcast').once().withExactArgs("timescale-changed", "1000");
    var target = $('<select id="timescale" class="navcenter smooth" data-type="select-timescale"><option value="1000">3h</option></select')[0];
    target.selectedIndex = 0;
    var event = $.Event('change', {
      target: target
    });
    behavior.onchange(event, target, 'select-timescale');
  });

});
