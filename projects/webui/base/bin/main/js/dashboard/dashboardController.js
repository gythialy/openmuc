(function(){

	var injectParams = ['$scope', '$state', 'notify', '$translate', 'AvailableAppsService', 'AuthService'];

	var DashboardController = function($scope, $state, notify, $translate, AvailableAppsService, AuthService) {

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
			angular.forEach(response, function(index, value){
				appsAliases.push(value.alias);
			});
		}, function(error) {
			if (error.status == 401) {
				AuthService.logout();
				notify({message: $scope.sessionExpiredText, position: "right", classes: "alert-warning"});
				$state.go('home');
			} else {
				notify({message: $scope.loadingAppDependencesErrorText, position: "right", classes: "alert-warning"});
			}
		});

	};

	DashboardController.$inject = injectParams;

	angular.module('openmuc.dashboard').controller('DashboardController', DashboardController);

})();
