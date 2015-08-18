(function(){
	
	var injectParams = ['$scope', '$stateParams', '$state', '$alert', '$translate', 'DriversService'];
	
	var DriverEditController = function($scope, $stateParams, $state, $alert, $translate, DriversService) {

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
					$alert({content: $scope.driverOKText, type: 'success'});
					return $state.go('channelconfigurator.drivers.index');
				}, function(error) {
					$alert({content: $scope.driverErrorText, type: 'warning'});
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