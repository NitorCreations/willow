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
    }
  };
});