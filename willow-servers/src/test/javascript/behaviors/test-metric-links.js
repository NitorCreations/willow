describe("Tests for metric-links behavior", function() {
  var behavior, sandbox, contextFake;

  beforeEach(function() {
    utilsSvc = {
      getHashVariable: function() {},
      variableStateInHash: function() {}
    };
    jquery = {
      attr: function() {}
    };
    contextFake = new Box.TestServiceProvider({
      'utils': utilsSvc,
      'jQuery': jquery
    });
    contextFake.getElement = function() { return $('<div data-module="navigation-index"></div>')[0]; };
    sandbox = sinon.sandbox.create();
    behavior = Box.Application.getBehaviorForTest('metric-links', contextFake);
    behavior.init();
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