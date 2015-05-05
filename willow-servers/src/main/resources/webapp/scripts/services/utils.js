Box.Application.addService('utils', function(application) {
  'use strict';
  return {
    debouncer: function(func , timeout) {
      var timeoutID , tmOut = timeout || 200;
      return function () {
        var scope = this , args = arguments;
        clearTimeout( timeoutID );
        timeoutID = setTimeout( function () {
          func.apply( scope , Array.prototype.slice.call( args ) );
        } , tmOut );
      };
    },
    getQueryVariable: function(variable) {
      return this.getUrlVariable(window.location.search);
    },
    getHashVariable: function(variable) {
      return this.getUrlVariable(window.location.hash);
    },
    getUrlVariable: function(urlsegment, variable) {
      if (!urlsegment) return(false);
      urlsegment = urlsegment.substring(1);
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
    },
    variableStateInHash: function(variable, value, callback) {
      var hash = window.location.hash ? window.location.hash.substring(1) : "";
      hash = this.addOrReplaceUrlVariable(hash, variable, value);
      window.location.hash = "#" + hash;
      callback(hash);
    }

  };
});