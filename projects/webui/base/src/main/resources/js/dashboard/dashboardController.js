(function(){

	var injectParams = ['$scope', '$state', '$alert', '$translate', 'AvailableAppsService', 'AuthService'];

	var DashboardController = function($scope, $state, $alert, $translate, AvailableAppsService, AuthService) {

		$translate('LOADING_APP_DEPENDENCES_ERROR').then(function(text) {
			$scope.loadingAppDependencesErrorText = text;
		});
		
		$translate('SESSION_EXPIRED').then(function(text) {
			$scope.sessionExpiredText = text;
		});
		
		var appsAliases = [];
		$scope.availableApps = [];

		AvailableAppsService.getAll().then(function(response){
			$scope.availableApps = response;

			$.each(response, function(index, value) {
				appsAliases.push(value.alias);
			});
		}, function(error) {
			if (error.status == 401) {
				AuthService.logout();				
				$alert({content: $scope.sessionExpiredText, type: 'warning'});
				$state.go('home');				
			} else {
				$alert({content: $scope.loadingAppDependencesErrorText, type: 'warning'});
			}
		});

	};

	DashboardController.$inject = injectParams;

	angular.module('openmuc.dashboard').controller('DashboardController', DashboardController);

})();
