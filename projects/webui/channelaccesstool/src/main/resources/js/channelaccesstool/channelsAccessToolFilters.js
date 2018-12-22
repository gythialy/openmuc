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

	var dropValueDecimals = function() {
	    return function(value) {
			return String(value)
				.split('.')
				.map(function (d, i) { return i ? d.substr(0, 3) : d; })
				.join('.');
		};
	};

	intArrayToHexArray.$inject = injectParams;
	dropValueDecimals.$inject = injectParams;

	angular.module('openmuc.channelaccesstool').filter('intArrayToHexArray', intArrayToHexArray).filter('dropValueDecimals', dropValueDecimals);

})();
