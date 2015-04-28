Box.Application.addModule('navigation-index', function(context) {
  'use strict';
  var windowSvc;

  return {
    behaviors: [ 'metric-links', "navigation-common" ],
  };
});