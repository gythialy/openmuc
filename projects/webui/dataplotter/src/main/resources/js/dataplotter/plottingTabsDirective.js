(function(){

	var injectParams = [];

	var PlottingTabsDirective = function() {
		return {
			restrict: 'E',
			templateUrl: 'dataplotter/html/plottingTabs.html'
		};
	};

	PlottingTabsDirective.$inject = injectParams;

	angular.module('openmuc.dataplotter').directive('plottingTabs', PlottingTabsDirective);

})();
