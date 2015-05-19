Box.Application.addService('configuration-store', function(application) {
  'use strict';

  var localStorage = application.getGlobal("localStorage");

  function readConfiguration(configId) {
    return JSON.parse(localStorage.getItem(configId));
  }

  function storeConfiguration(configId, config) {
    localStorage.setItem(configId, JSON.stringify(config));
  }

  //FIXME each sub config area to own scope (custom, index, host)
  function customRadiatorKey(radiatorId) {
    return "willow:custom_radiators:" + radiatorId;
  }

  function readRadiatorConfiguration(radiatorName) {
    return readConfiguration(customRadiatorKey(radiatorName));
  }

  function storeRadiatorConfiguration(radiatorName, config) {
    return storeConfiguration(customRadiatorKey(radiatorName), config);
  }

  function appendGraphToRadiator(radiatorName, config) {
    var existingConfig = readRadiatorConfiguration(radiatorName) || [];
    existingConfig.push(config);
    storeRadiatorConfiguration(radiatorName, existingConfig);
  }

  return {
    customRadiators: {
      readConfiguration: readRadiatorConfiguration,
      storeConfiguration: storeRadiatorConfiguration,
      appendConfig: appendGraphToRadiator
    },
    readConfiguration: readConfiguration,
    storeConfiguration: storeConfiguration
  };
});