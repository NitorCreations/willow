Box.Application.addModule('radiator-navigation', function(context) {
  'use strict';

  var $, host, windowService;

  return {
    behaviors: [ "navigation-common" ],

    init: function() {
      $ = context.getGlobal("jQuery");
      windowService = context.getService("window");
      host = windowService.getHashVariable("host");
      $(".shape-navterminal").attr("data-host", host);
    }
  };
});