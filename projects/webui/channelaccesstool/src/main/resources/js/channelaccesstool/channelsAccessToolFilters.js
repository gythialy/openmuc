(function(){
	
	var injectParams = [];
		
	var intArrayToHexArray = function() {
	    return function(input) {
	    	if ($.isArray(input)) {
	    		var values = [];
	    		$.each(input, function(i, value) {
	    			values.push(value.toString(16).toUpperCase());
	    		});
	    		return values;
	    	} else {
	    		return input;
	    	}
	    };
	};

	intArrayToHexArray.$inject = injectParams;

	angular.module('openmuc.channelaccesstool').filter('intArrayToHexArray', intArrayToHexArray);

})();