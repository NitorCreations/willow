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
  function customRadiatorKey() {
    return "willow:custom_radiators";
  }

  function readRadiatorConfigurations() {
    return readConfiguration(customRadiatorKey()) || {};
  }

  function readRadiatorConfiguration(radiatorName) {
    var configs = readRadiatorConfigurations();
    return configs[radiatorName];
  }

  function storeRadiatorConfiguration(radiatorName, config) {
    var configs = readRadiatorConfigurations();
    configs[radiatorName] = config;
    storeConfiguration(customRadiatorKey(), configs);
  }

  function removeRadiatorConfiguration(radiatorName) {
    var configs = readRadiatorConfigurations();
    delete configs[radiatorName];
    storeConfiguration(customRadiatorKey(), configs);
  }

  function appendGraphToRadiator(radiatorName, config) {
    var configurations = readRadiatorConfigurations();
    var existingConfig = configurations[radiatorName] || [];
    existingConfig.push(config);
    configurations[radiatorName] = existingConfig;
    storeConfiguration(customRadiatorKey(), configurations);
  }

  function listRadiatorNames() {
    return Object.keys(readRadiatorConfigurations());
  }

  return {
    customRadiators: {
      readConfiguration:      readRadiatorConfiguration,
      storeConfiguration:     storeRadiatorConfiguration,
      appendConfiguration:    appendGraphToRadiator,
      removeConfiguration:    removeRadiatorConfiguration,
      listAvailableRadiators: listRadiatorNames
    },
    readConfiguration: readConfiguration,
    storeConfiguration: storeConfiguration
  };
});