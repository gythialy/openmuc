(function(){

	var injectParams = ['$scope', '$state', '$translate', 'notify', 'DriversService'];

	var DriverNewController = function($scope, $state, $translate, notify, DriversService) {

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
					notify({message: $scope.drvierOKText, position: "right", classes: "alert-success"});
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

	DriverNewController.$inject = injectParams;

	angular.module('openmuc.drivers').controller('DriverNewController', DriverNewController);

})();
