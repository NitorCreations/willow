Box.Application.addModule('navigation-index', function(context) {
  'use strict';
  return {
    behaviors: [ 'metric-links', "navigation-common" ]
  };
});