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
    }
  };
});
