Box.Application.addModule('radiator-navigation', function(context) {
  'use strict';

  var $, host, windowService, session;

  return {
    behaviors: [ "navigation-common" ],

    init: function() {
      $ = context.getGlobal("jQuery");
      session = context.getGlobal("session");
      windowService = context.getService("window");
      host = windowService.getHashVariable("host");
      if (session.isAdmin) {
        $(".shape-navterminal").attr("data-host", host);
      } else {
        $(".shape-navterminal").remove();
      }
    }
  };
});