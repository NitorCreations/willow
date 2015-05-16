Box.Application.addModule('navigation-radiator', function(context) {
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