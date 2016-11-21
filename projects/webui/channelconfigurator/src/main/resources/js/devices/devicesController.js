(function(){

	var injectParams = ['$scope', '$http', '$alert', '$state', '$translate', 'DevicesService', 'DriversService'];
	
	var DevicesController = function($scope, $http, $alert, $state, $translate, DevicesService, DriversService) {

		$translate('DEVICE_DELETED_SUCCESSFULLY').then(function(text) {
			$scope.deviceOKText = text;
		});

        $translate('DELETE_CONFIRM_MESSAGE').then(function(confirmMessage) {
            $scope.confirmMessage = confirmMessage;
        });

		$scope.drivers = [];
		
		DriversService.getDrivers().then(function(drivers){
			$scope.drivers = drivers;

			$.each($scope.drivers, function(index, driver) {
				DevicesService.getDevices(driver).then(function(devices){
					$scope.drivers[index]['devices'] = devices;
				});
			});
		});

		$scope.deleteDevice = function(deviceName) {
            if (confirm($scope.confirmMessage + " " + deviceName + "?") == true) {
				DevicesService.destroy(deviceName).then(function(data) {
					$alert({content: $scope.deviceOKText, type: 'success'});
					return $state.go('channelconfigurator.devices.index');
				});

				$.each($scope.drivers, function(index, driver) {
					DevicesService.getDevices(driver).then(function(devices){
						$scope.drivers[index]['devices'] = devices;
					});
				});
			}
		};
		
	};

	DevicesController.$inject = injectParams;

	angular.module('openmuc.devices').controller('DevicesController', DevicesController);
	
})();