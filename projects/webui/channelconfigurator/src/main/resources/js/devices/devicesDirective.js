(function(){

	var injectParams = [];

	var DevicesFormDirective = function() {
		return {
			restrict: 'E',
			templateUrl: 'channelconfigurator/html/devices/form.html'
		};
	};

	DevicesFormDirective.$inject = injectParams;

	angular.module('openmuc.devices').directive('devicesForm', DevicesFormDirective);

})();
