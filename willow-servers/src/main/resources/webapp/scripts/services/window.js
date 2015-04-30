Box.Application.addService('window', function(application) {
  'use strict';
  // I'll add utilities here for communicating between browser windows
  // and keeping track of open windows
  // For example you need to be able to add a graph to an open radiator
  return {
    open: function(url, target) {
      window.open(url, target);
    }
  };
});