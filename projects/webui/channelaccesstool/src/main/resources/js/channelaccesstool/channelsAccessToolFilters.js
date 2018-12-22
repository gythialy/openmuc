(function(){
	
	var injectParams = [];
		
	var intArrayToHexArray = function() {
	    return (input) => {
	    	if (!Array.isArray(input)) {
				return input;
			}

			return '[' + input.map(v => Number(v).toString(16).toUpperCase()).join(', ') + ']'
		};
	};

	intArrayToHexArray.$inject = injectParams;

	angular.module('openmuc.channelaccesstool').filter('intArrayToHexArray', intArrayToHexArray);

})();