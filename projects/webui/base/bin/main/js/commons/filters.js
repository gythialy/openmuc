(function(){

	var injectParams = [];

	var yesNoIcon = function() {
	    return function(input) {
	        return input ? 'fa fa-check' : 'fa fa-times';
	    };
	};

	yesNoIcon.$inject = injectParams;

	angular.module('openmuc.filters').filter('yesNoIcon', yesNoIcon);

})();
