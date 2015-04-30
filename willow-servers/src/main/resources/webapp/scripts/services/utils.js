Box.Application.addService('utils', function(application) {
  'use strict';
  return {
    getQueryVariable: function(variable) {
       return this.getUrlVariable(window.location.search.substring(1));
    },
    getUrlVariable: function(urlsegment, variable) {
      if (!urlsegment) return(false);
      var vars = urlsegment.split("&");
      for (var i=0;i<vars.length;i++) {
        var pair = vars[i].split("=");
        if(decodeURIComponent(pair[0]) == variable){return decodeURIComponent(pair[1]);}
      }
      return(false);
    },
    addOrReplaceUrlVariable: function(urlsegment, variable, value) {
      var vars = urlsegment.split("&");
      var ret= "";
      var found = false;
      for (var i=0;i<vars.length;i++) {
        var pair = vars[i].split("=");
        if(decodeURIComponent(pair[0]) === variable){
          ret += "&" + pair[0] + "=" + encodeURIComponent(value);
          found = true;
        } else if (pair.length == 2) {
          ret += "&" + vars[i];
        }
      }
      if (!found) {
        ret += "&" + encodeURIComponent(variable) + "=" + encodeURIComponent(value);
      }
      return ret.substring(1);

    }
  };
});