describe("Tests for test-metric behavior", function() {
  var behavior, sandbox, contextFake;

  beforeEach(function() {
    contextFake = new Box.TestServiceProvider();
    behavior = Box.Application.getBehaviorForTest('metric-links', contextFake);
    behavior.init();
    sandbox = sinon.sandbox.create();
  });

  afterEach(function() {
    behavior.destroy();
    sandbox.verifyAndRestore();
  });

  it('Clicking an element with "select-metric" type should send "metric-changed" message', function() {
    sandbox.mock(contextFake).expects('broadcast').once().withExactArgs("metric-changed", "cpu");
    var target = $('<a id="cpu" href="#metric=cpu" data-metric="cpu" data-type="select-metric">cpu</a>')[0];
    var event = $.Event('click', {
      target: target
    });
    behavior.onclick(event, target, 'select-metric');
  });
});