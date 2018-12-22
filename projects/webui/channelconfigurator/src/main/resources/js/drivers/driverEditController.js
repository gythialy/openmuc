(function(){

	var injectParams = ['$scope', '$stateParams', '$state', '$translate', 'notify', 'DriversService'];

	var DriverEditController = function($scope, $stateParams, $state, $translate, notify, DriversService) {

		$translate('DRIVER_UPDATED_SUCCESSFULLY').then(function(text) {
			$scope.driverOKText = text;
		});

		$translate('DRIVER_UPDATED_ERROR').then(function(text) {
			$scope.driverErrorText = text;
		});

		$scope.driver = DriversService.getDriver($stateParams.id);
		$scope.driverId = $stateParams.id;

		$scope.saveDriver = function() {
			if ($scope.driverForm.$valid) {
				DriversService.update($scope.driver).then(function(resp) {
					notify({message: $scope.driverOKText, position: "right", classes: "alert-success"});
					return $state.go('channelconfigurator.drivers.index');
				}, function(error) {
					notify({message: $scope.driverErrorText, position: "right", classes: "alert-warning"});
					return $state.go('channelconfigurator.drivers.index');
				});
			} else {
				$scope.driverForm.submitted = true;
			}
		}

	};

	DriverEditController.$inject = injectParams;

	angular.module('openmuc.drivers').controller('DriverEditController', DriverEditController);

})();
