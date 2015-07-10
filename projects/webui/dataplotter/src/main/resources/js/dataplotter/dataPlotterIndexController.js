(function(){

	var injectParams = ['$scope', '$q', 'DataPlotterService'];
	
	var DataPlotterIndexController = function($scope, $q, DataPlotterService) {
		
		DataPlotterService.getConfigFile().then(function(response) {
			$scope.confFile = response;
		});
		
		$scope.getPlotter = function(name) {
			var res = "";
			$.each($scope.confFile, function(i, plotter) {
				if (plotter.name == name) {
					res = plotter;
				}
			});
			return res;
		};

	};

	DataPlotterIndexController.$inject = injectParams;

	angular.module('openmuc.dataplotter').controller('DataPlotterIndexController', DataPlotterIndexController);
	
})();