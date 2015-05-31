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

  function removeConfigurationFromRadiator(radiatorName, config) {
    var configurations = readRadiatorConfigurations();
    var existingConfig = configurations[radiatorName] || [];
    var storedConfig = existingConfig.filter(function(graph) {
      return graph.id !== config.id;
    });
    configurations[radiatorName] = storedConfig;
    storeConfiguration(customRadiatorKey(), configurations);
  }

  function appendGraphToRadiator(radiatorName, config) {
    var configurations = readRadiatorConfigurations();
    var existingConfig = configurations[radiatorName] || [];
    var newConfigId = existingConfig.length + 1;
    config.id = newConfigId;
    existingConfig.push(config);
    configurations[radiatorName] = existingConfig;
    storeConfiguration(customRadiatorKey(), configurations);
    return newConfigId;
  }

  function listRadiatorNames() {
    return Object.keys(readRadiatorConfigurations());
  }

  return {
    customRadiators: {
      readConfiguration: readRadiatorConfiguration,
      storeConfiguration: storeRadiatorConfiguration,
      removeConfiguration: removeRadiatorConfiguration,
      appendConfiguration: appendGraphToRadiator,
      removeRadiatorConfig: removeConfigurationFromRadiator,
      listAvailableRadiators: listRadiatorNames
    },
    readConfiguration: readConfiguration,
    storeConfiguration: storeConfiguration
  };
});