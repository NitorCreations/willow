Box.Application.addService('window', function(application) {
  'use strict';
  var intercom = application.getGlobal("Intercom").getInstance();
  var localStorage = application.getGlobal("localStorage"); //application.getService("configuration-store");
  var name  = application.getGlobal("name");
  var hashChangeCallbacks = [];
  var utils = application.getService("utils");
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

  function graphSpecificationString(graphConfig) {
    var config = graphConfig;
    config.type = graphConfig.type || "horizon";
    return JSON.stringify(config);
  }

  function sendToRadiator(radiatorName, graphConfig) {
    var graphSpec = graphSpecificationString(graphConfig);
    var openWindows = localStorage.willowWindows ? JSON.parse(localStorage.willowWindows) : [];
    if (openWindows.indexOf("radiator-" + radiatorName) == -1) {
      _open("radiator.html#graph=" + encodeURIComponent(graphSpec) + "&name=" + radiatorName, "radiator-" + radiatorName);
    } else {
      intercom.emit("radiator-" + radiatorName + "-addgraph", graphSpec);
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
    },
    setTitle: function(newTitle) {
      document.title = "Willow - " + utils.htmlEncode(newTitle);
    },

    /*
     * Creates a popup
     *
     * @param {Object} opts
     * @returns {Object} reference to created window
     *
     */
    popup: function(opts) {
      opts        = opts || {};

      opts.width  = opts.width  || window.innerWidth * 0.75;
      opts.height = opts.height || 200;
      opts.posX   = opts.posX   || window.screenX + (window.innerWidth / 2 - opts.width / 2);
      opts.posY   = opts.posY   || window.screenY + (window.innerHeight / 2 - opts.height / 2) + 100;
      opts.name   = opts.name   || 'TODO: this name.';
      opts.url    = opts.url    || '/';

      return window.open(
        opts.url,
        opts.name,
        'height=' + opts.height +
        ',width=' + opts.width +
        ',screenX=' + opts.posX +
        ',screenY=' + opts.posY
      );
    }
  };
});
