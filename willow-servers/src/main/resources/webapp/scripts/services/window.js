Box.Application.addService('window', function(application) {
  'use strict';
  // Add utilities here for communicating between browser windows
  // and keeping track of open windows, .i.e. routing component

  function _open(url, target) {
    window.open(url, target);
  }

  function openRadiator(host) {
    _open("radiator.html?host=" + host, "radiator-" + host);
  }

  function openTerminal(user, host) {
    _open("shell.html?user=" + user + "&host=" + host, "_blank");
  }

  function openAlerts() {
    _open("alerts.html", "index-alerts");
  }

  return {
    openRadiatorForHost: openRadiator,
    openTerminalToHost: openTerminal,
    openAlerts: openAlerts,
    setHash: function(hash) { //FIXME define deeplinking contract
      window.location.hash = "#" + hash;
    }
  };
});