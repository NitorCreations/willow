Box.Application.addService('window', function(application) {
  'use strict';
  var intercom = application.getGlobal("Intercom").getInstance();
  var localStorage = application.getGlobal("localStorage");
  var name  = application.getGlobal("name");
  var hashChangeCallbacks = [];
  // Add utilities here for communicating between browser windows
  // and keeping track of open windows, .i.e. routing component

  function _open(url, target) {
    window.open(url, target);
  }

  function openRadiator(host) {
    _open("radiator.html#host=" + host, "hostradiator-" + host);
  }

  function openTerminal(user, host) {
    _open("shell.html?user=" + user + "&host=" + host, "_blank");
  }

  function openAlerts() {
    _open("alerts.html", "index-alerts");
  }
  
  function sendToRadiator(graphConfig, radiatorname) {
    var graphSpec = '{"type":"horizon","host":"' + graphConfig.host + '","metric":"' + graphConfig.metric + '"}';
    var windowArr = localStorage.willowWindows ? JSON.parse(localStorage.willowWindows) : [];
    if (windowArr.indexOf("radiator-" + radiatorname) == -1) {
      _open("radiator.html#graph=" + encodeURIComponent(graphSpec) + "&name=" + radiatorname, "radiator-" + radiatorname);
    } else {
      intercom.emit("radiator-" + radiatorname + "-addgraph", graphSpec);
    }
  }

  return {
    openRadiatorForHost: openRadiator,
    sendGraphToRadiator: sendToRadiator,
    openTerminalToHost: openTerminal,
    openAlerts: openAlerts,
    getQueryVariable: function(variable) {
      return this.getUrlVariable(window.location.search, variable);
    },
    getHashVariable: function(variable) {
      return this.getUrlVariable(window.location.hash, variable);
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
    variableStateInHash: function(variable, value) {
      var hash = window.location.hash ? window.location.hash.substring(1) : "";
      hash = this.addOrReplaceUrlVariable(hash, variable, value);
      window.location.hash = "#" + hash;
      var i=0;
      for (i=0;i<hashChangeCallbacks.length;i++) {
        hashChangeCallbacks[i](hash);
      }
    },
    onHashChange: function(callback) {
      hashChangeCallbacks.push(callback);
      return callback;
    }
  };
});