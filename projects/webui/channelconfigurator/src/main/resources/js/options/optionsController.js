(function(){

	var injectParams = ['$scope', '$http', '$alert'];
	
	var OptionsController = function($scope, $http, $alert) {
		$scope.options = [];
	};

	OptionsController.$inject = injectParams;

	angular.module('openmuc.options').controller('OptionsController', OptionsController);
	
})();