Box.Application.addService('utils', function(application) {
  'use strict';
  var $ = application.getGlobal('jQuery');

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
    // check if object contains a property
    contains: function(where, what) {
      for (var key in where) {
        return (key === what);
      }
    },
    setConfigurationElement: function(container, config) {
      container.html("<script type='text/x-config'>" + JSON.stringify(config) + "</script>");
    },
    /*
     * Replaces special characters with HTML entities
     *
     * @param {String} str
     * @returns {String} str with special chars replaced to HTML entities
     *
     */
    htmlEncode: function(str) {
      var el = document.createElement("div");
      el.innerText = el.textContent = str;
      str = el.innerHTML;
      return str;
    },

    // generates a new guid
    guid: function() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,
        function(c) {
          var r = Math.random() * 16 | 0,
            v = c == 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
        }).toUpperCase();
    },

    getStep: function(timescale, widthInPx) {
      widthInPx = widthInPx || $(window).width();
      return parseInt(timescale * 1000 / widthInPx);
    },

    appendPopupGraphIcon: function(parentElement, host) {
      return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon popup-" + host, true)
        .attr("data-type", "to-popup")
        .append("use").attr("xlink:href", "#shape-external-link");
    },

    appendDraggableHandleIcon: function(parentElement) {
    return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon drag-handle", true)
        .attr("data-type", "graph-drag")
        .append("use").attr("xlink:href", "#shape-move");
    },

    appendShareRadiatorIcon: function(parentElement, host) {
      return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon share-" + host, true)
        .attr("data-type", "to-radiator")
        .append("use").attr("xlink:href", "#shape-to-radiator");
    },

    appendTerminalIcon: function(parentElement, host) {
      return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon shape-terminal terminal-" + host, true)
        .attr("data-type", "start-terminal")
        .attr("data-host", host)
        .append("use").attr("xlink:href", "#shape-terminal");
    },

    appendRemovalButton: function(parentElement, parentModuleId) {
      return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon remove", true)
        .attr("data-type", "close")
        .attr("data-graph-module-id", parentModuleId)
        .append("use").attr("xlink:href", "#shape-close2");
    },

    appendHostRadiatorLink: function(parentElement, title, host) {
      var link = parentElement.append("div").classed("host-link", true);
      link.append("span").text(title);
      link.append("a")
        .attr("href", "radiator.html#host=" + host)
        .attr("data-host", host)
        .attr("data-type", "host-radiator").text(host);
      return link;
    },

    getDetailsSteps: function(moduleElement, timescale) {
      var steps = timescale;
      var detailsStep = 1;
      var i = 0;
      while (steps > ($(moduleElement[0]).width() / 2)) {
        detailsStep = scales[i].step;
        legend = scales[i].legend;
        steps = parseInt((detailsStop - detailsStart) / detailsStep);
        i++;
      }
    }
  };
});
