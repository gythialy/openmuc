(function(){
	
	var injectParams = ['$scope', '$stateParams', '$state', '$alert', '$translate', 'DevicesService', 'DriversService'];
	
	var DeviceEditController = function($scope, $stateParams, $state, $alert, $translate, DevicesService, DriversService) {

		$scope.driverInfo = {};

		$translate('DEVICE_UPDATED_SUCCESSFULLY').then(function(text) {
			$scope.deviceOKText = text;
		});

		$translate('DEVICE_UPDATED_ERROR').then(function(text) {
			$scope.deviceErrorText = text;
		});
		
		if ($stateParams.deviceId) {
			$scope.device = DevicesService.getDevice($stateParams.deviceId);
			$scope.deviceId = $stateParams.deviceId;
		} else {
			$scope.device = [];
		}

		$scope.saveDevice = function() {
			if ($scope.deviceForm.$valid) {
				DevicesService.update($scope.device).then(function(resp){
					$alert({content: $scope.deviceOKText, type: 'success'});
					return $state.go('channelconfigurator.devices.index');
				}, function(error) {
					$alert({content: $scope.deviceErrorText, type: 'warning'});
					return $state.go('channelconfigurator.devices.index');
				});
			} else {
				$scope.deviceForm.submitted = true;
			}
		};

		$scope.getDriverInfo = function() {
            if ($stateParams.driverId) {
                DriversService.getInfos($stateParams.driverId).then(function(driverInfo) {
                    $scope.driverInfo = driverInfo;
                });
            }
            if(Object.keys($scope.driverInfo).length == 0) {
                $scope.driverInfo["settingsSyntax"] = "N.A.";
                $scope.driverInfo["deviceAddressSyntax"] = "N.A.";
            }
		};

	};

	DeviceEditController.$inject = injectParams;

	angular.module('openmuc.devices').controller('DeviceEditController', DeviceEditController);

})();