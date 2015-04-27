/**
 * @fileoverview Navigation pane for system frontpage
 */
Box.Application.addModule('navigation-index', function(context) {
	'use strict';
	var element;

	return {
		init: function() {
			element = context.getElement();
		},

		destroy: function() {
			element = null;
		},
		onclick: function(event, element, elementType) {
			switch (elementType) {
			case 'select-metric':
				metric = getHashVariable(element.getAttribute("href").substring(1), "metric");
				resetGraphs();
				shuffleNavigation();
				break;
			case 'alerts':
				alert("Alerts");
				break;
			};
		},
		onchange: function(event, element, elementType) {
			switch (elementType) {
			  case 'select-timescale':
				alert(element.children[element.selectedIndex].getAttribute("value"));
				break;
			};
		}
	};
});