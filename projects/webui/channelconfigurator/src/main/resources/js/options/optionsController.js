(function(){

	var injectParams = ['$scope', '$http', 'notify'];

	var OptionsController = function($scope, $http, notify) {
		$scope.options = [];
	};

	OptionsController.$inject = injectParams;

	angular.module('openmuc.options').controller('OptionsController', OptionsController);

})();
