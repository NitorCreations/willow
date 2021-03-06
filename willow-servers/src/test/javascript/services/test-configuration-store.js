describe("Tests for configuration store:", function() {
  var service, sandbox, contextFake;

  var radiatorConfigKey = "willow:custom_radiators";

  var testGraphConfig = {
    host: "test-host",
    instaceTag: "test-instances",
    metric: "test-metric"
  };

  var testGraphConfig2 = {
    id: 1,
    host: "test-host2",
    instaceTag: "test-instances2",
    metric: "test-metric2"
  };

  var testGraphConfig3 = {
    id:3,
    host: "test-host3",
    instaceTag: "test-instances3",
    metric: "test-metric3"
  };

  var existingConfigurations = {
    rad1: [testGraphConfig2],
    rad2: [testGraphConfig2, testGraphConfig3]
  };

  beforeEach(function() {
    contextFake = new Box.TestServiceProvider();
    service = Box.Application.getServiceForTest('configuration-store', contextFake);
    sandbox = sinon.sandbox.create();
    var localStorageGetItemStubbed = sandbox.stub(window.localStorage, "getItem");
    localStorageGetItemStubbed.withArgs(radiatorConfigKey).returns(JSON.stringify(existingConfigurations));
  });

  it('lists available radiator names', function() {
    expect(service.customRadiators.listAvailableRadiators()).toEqual(['rad1','rad2']);
  });

  it('returns existing radiator configuration', function() {
    expect(service.customRadiators.readConfiguration("rad1")).toEqual([testGraphConfig2]);
  });

  it('append graph configuration in existing radiator config', function() {
    var insertedGraphConfig = clone(testGraphConfig);
    insertedGraphConfig.id = 2;
    var expectedConfigurationsAfterAppend = {
      rad1: [testGraphConfig2, insertedGraphConfig],
      rad2: [testGraphConfig2, testGraphConfig3]
    };
    sandbox.mock(localStorage).expects('setItem')
      .withExactArgs(
      radiatorConfigKey,
      JSON.stringify(expectedConfigurationsAfterAppend)
    );
    service.customRadiators.appendConfiguration("rad1", testGraphConfig);
  });

  it('append graph configuration with new radiator key', function() {
    var insertedGraphConfig = clone(testGraphConfig);
    insertedGraphConfig.id = 1;
    var expectedConfigurationsAfterAppend = {
      'rad1': [testGraphConfig2],
      'rad2': [testGraphConfig2, testGraphConfig3],
      'new-rad': [insertedGraphConfig]
    };
    sandbox.mock(localStorage).expects('setItem')
        .withExactArgs(
      radiatorConfigKey,
      JSON.stringify(expectedConfigurationsAfterAppend)
    );
    service.customRadiators.appendConfiguration("new-rad", testGraphConfig);
  });

  it('overwrite radiator configuration', function() {
    var expectedConfigurationsAfterAppend = {
      'rad1': [testGraphConfig2],
      'rad2': [testGraphConfig]
    };
    sandbox.mock(localStorage).expects('setItem')
        .withExactArgs(
      radiatorConfigKey,
      JSON.stringify(expectedConfigurationsAfterAppend)
    );
    service.customRadiators.storeConfiguration("rad2", [testGraphConfig]);
  });

  it('removes radiator configuration', function() {
    var expectedConfigurationsAfterAppend = {
      'rad1': [testGraphConfig2]
    };
    sandbox.mock(localStorage).expects('setItem')
      .withExactArgs(
      radiatorConfigKey,
      JSON.stringify(expectedConfigurationsAfterAppend)
    );
    service.customRadiators.removeConfiguration("rad2");
  });

  it('removes graph configuration from radiator', function() {
    var expectedConfigurationsAfterAppend = {
      'rad1': [testGraphConfig2],
      'rad2': [testGraphConfig2]
    };
    sandbox.mock(localStorage).expects('setItem')
      .withExactArgs(
      radiatorConfigKey,
      JSON.stringify(expectedConfigurationsAfterAppend)
    );
    service.customRadiators.removeRadiatorConfig("rad2", testGraphConfig3);
  });

  afterEach(function() {
    sandbox.verifyAndRestore();
  });

  function clone(object) {
    return JSON.parse(JSON.stringify(object));
  }
});