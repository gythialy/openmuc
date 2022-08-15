(function(){

	var injectParams = [];

	var DriversFormDirective = function() {
		return {
			restrict: 'E',
			templateUrl: 'channelconfigurator/html/drivers/form.html'
		};
	};

	DriversFormDirective.$inject = injectParams;

	angular.module('openmuc.drivers').directive('driversForm', DriversFormDirective);

})();
