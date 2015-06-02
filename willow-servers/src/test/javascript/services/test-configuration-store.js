describe("Tests for configuration store:", function() {
  var service, sandbox, contextFake;

  var radiatorConfigKey = "willow:custom_radiators";

  var testGraphConfig = {
    host: "test-host",
    instaceTag: "test-instances",
    metric: "test-metric"
  };

  var testGraphConfig2 = {
    host: "test-host2",
    instaceTag: "test-instances2",
    metric: "test-metric2"
  };

  var testGraphConfig3 = {
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
    var expectedConfigurationsAfterAppend = {
      rad1: [testGraphConfig2, testGraphConfig],
      rad2: [testGraphConfig2, testGraphConfig3]
    };

    sandbox.stub(window.localStorage, "setItem")
      .withArgs(
        radiatorConfigKey,
        JSON.stringify(expectedConfigurationsAfterAppend)
      );

    service.customRadiators.appendConfiguration("rad1", testGraphConfig);
  });

  it('append graph configuration with new radiator key', function() {
    var expectedConfigurationsAfterAppend = {
      'rad1': [testGraphConfig2],
      'rad2': [testGraphConfig2, testGraphConfig3],
      'new-rad': [testGraphConfig]
    };

    sandbox.stub(window.localStorage, "setItem")
      .withArgs(
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

    sandbox.stub(window.localStorage, "setItem")
      .withArgs(
        radiatorConfigKey,
        JSON.stringify(expectedConfigurationsAfterAppend)
      );

    service.customRadiators.storeConfiguration("rad2", [testGraphConfig]);
  });

  afterEach(function() {
    sandbox.verifyAndRestore();
  });
});