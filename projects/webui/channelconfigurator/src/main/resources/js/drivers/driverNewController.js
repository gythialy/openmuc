(function(){
	
	var injectParams = ['$scope', '$state', '$alert', '$translate', 'DriversService'];
	
	var DriverNewController = function($scope, $state, $alert, $translate, DriversService) {

		$translate('DRIVER_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.driverOKText = text;
		});
		
		$translate('DRIVER_CREATED_ERROR').then(function(text) {
			$scope.driverErrorText = text;
		});
		
		$scope.driver = {configs: {
							samplingTimeout: 0,
							connectRetryInterval: 60000,
							disabled: false,
							}
						};

		$scope.saveDriver = function() {
			if ($scope.driverForm.$valid) {
				DriversService.create($scope.driver).then(function(resp) {
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

	DriverNewController.$inject = injectParams;

	angular.module('openmuc.drivers').controller('DriverNewController', DriverNewController);

})();