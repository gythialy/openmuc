(function(){

	var injectParams = ['$scope', '$stateParams', '$state', '$translate', 'notify', 'DevicesService', 'DriversService'];

	var DeviceEditController = function($scope, $stateParams, $state, $translate, notify, DevicesService, DriversService) {

		$scope.driverInfo = {};
		var deviceWarningrText;

        $translate('DRIVER_INFO_FAILED').then(text => deviceWarningrText = text);
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
					notify({message: $scope.deviceOKText, position: "right", classes: "alert-success"});
					return $state.go('channelconfigurator.devices.index');
				}, function(error) {
					notify({message: $scope.deviceErrorText, position: "right", classes: "alert-warning"});
					return $state.go('channelconfigurator.devices.index');
				});
			} else {
				$scope.deviceForm.submitted = true;
			}
		};

		$scope.getDriverInfo = function() {
            if ($stateParams.driverId) {
                DriversService.getInfos($stateParams.driverId).then((driverInfo) => $scope.driverInfo = driverInfo, e => {
					notify({message: deviceWarningrText, position: "right", classes: "alert-warning"});
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
