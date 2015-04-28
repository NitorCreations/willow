Box.Application.addService('utils', function(application) {
  'use strict';
  return {
    getQueryVariable: function(variable) {
       return this.getUrlVariable(window.location.search.substring(1));
    },
    getUrlVariable: function(urlsegment, variable) {
      var vars = urlsegment.split("&");
      for (var i=0;i<vars.length;i++) {
        var pair = vars[i].split("=");
        if(pair[0] == variable){return pair[1];}
      }
      return(false);
    }
  };
});