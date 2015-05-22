describe("Tests for configuration store:", function() {
  //TODO resolve issue with mocking local storage in tests
  var service, sandbox, contextFake, mockStorage;

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
    localStorage.clear();
    contextFake = new Box.TestServiceProvider();
    service = Box.Application.getServiceForTest('configuration-store', contextFake);
    sandbox = sinon.sandbox.create();
    sandbox.mock(localStorage).expects('getItem').withExactArgs(radiatorConfigKey)
        .returns(JSON.stringify(existingConfigurations));
  });

  it('reads existing radiator configurations from proper place', function() {
    service.customRadiators.listAvailableRadiators();
  });

  it('reads existing radiator configuration', function() {
    service.customRadiators.readConfiguration("rad1");
  });

  /*
   //FIXME figure out why this expectation does not work
  it('append graph configuration in existing radiator config', function() {
    var expectedConfiguration = existingConfigurations.rad1.push(testGraphConfig);
    sandbox.mock(localStorage).expects('setItem')
        .withExactArgs(radiatorConfigKey, JSON.stringify(expectedConfiguration));
    service.customRadiators.appendConfiguration("rad1", testGraphConfig);
  });

/*
  //FIXME figure out why this expectation does not work
  it('append graph configuration with new radiator key', function() {
    var expectedConfiguration = existingConfigurations;
    expectedConfiguration["new_rad"] = [testGraphConfig];
    sandbox.mock(localStorage).expects('setItem')
        .withExactArgs(radiatorConfigKey, JSON.stringify(expectedConfiguration));
    service.customRadiators.appendConfiguration("new-rad", testGraphConfig);
  });

  //FIXME figure out why this expectation does not work
  it('store graph configuration', function() {
    var expectedConfiguration = existingConfigurations;
    expectedConfiguration.rad2 = [testGraphConfig];
    sandbox.mock(localStorage).expects('setItem')
        .withExactArgs(radiatorConfigKey, JSON.stringify(expectedConfiguration));
    service.customRadiators.storeConfiguration("rad2", [testGraphConfig]);
  });
*/

  afterEach(function() {
    localStorage.clear();
    sandbox.verifyAndRestore();
  });
});