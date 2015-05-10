Box.Application.addService('configuration-store', function(application) {
  'use strict';

  function readConfiguration(configId) {
    return JSON.parse(localStorage.getItem(configId));
  }

  function storeConfiguration(configId, config) {
    localStorage.setItem(configId, JSON.stringify(config));
  }

  return {
    readConfiguration: readConfiguration,
    storeConfiguration: storeConfiguration
  };
});