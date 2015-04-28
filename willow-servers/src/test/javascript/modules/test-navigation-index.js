describe("Tests for navigation-index module", function() {
  var module, sandbox;

  beforeEach(function() {
      var contextFake = new Box.TestServiceProvider();
      this.module = Box.Application.getModuleForTest('navigation-index', contextFake);
      this.module.init();
      this.sandbox = sinon.sandbox.create();
  });

  afterEach(function() {
      this.module.destroy();
      this.sandbox.verifyAndRestore();
  });

  it('Clicking an element with "select-metric" type should call selectMetric()', function() {
	    this.sandbox.mock(this.module).expects('selectMetric').once().withExactArgs("metric=cpu");
	    var target = $('<a id="cpu" href="#metric=cpu" data-type="select-metric">cpu</a>')[0];
	    var event = $.Event('click', {
	        target: target
	    });
	    this.module.onclick(event, target, 'select-metric');
  });
});
