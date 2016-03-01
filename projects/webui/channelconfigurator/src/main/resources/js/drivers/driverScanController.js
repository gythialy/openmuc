(function(){

	var injectParams = ['$scope', '$state', '$alert', '$stateParams', '$translate', 'DriversService', 'DevicesService'];
	
	var DriverScanController = function($scope, $state, $alert, $stateParams, $translate, DriversService, DevicesService) {

		$translate('DRIVER_SCAN_DEVICE_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.deviceOKText = text;
		});
		
		$translate('DRIVER_SCAN_DEVICE_CREATED_ERROR').then(function(text) {
			$scope.deviceErrorText = text;
		});
		
		$translate('DRIVER_SCAN_NOT_SUPPORTED').then(function(text) {
			$scope.deviceWarningrText = text;
		});
		
		$scope.driver = DriversService.getDriver($stateParams.id);
		$scope.devices = [];
		$scope.selectedDevices = [];
		$scope.settings = "";

		$scope.scanDriver = function() {
			$scope.scanDriverForm.submitted = true;
			DriversService.scan($scope.driver, $scope.settings).then(function(response) {
				$scope.devices = [];
    			$.each(response.devices, function(i, device) {
    				$scope.devices.push({configs: device});
    			});
				
				$scope.scanDriverForm.submitted = false;
			}, function(error) {
				$alert({content: $scope.deviceWarningrText, type: 'warning'});
				return $state.go('channelconfigurator.drivers.index');
			});
		};
		
		$scope.addDevices = function() {
			$.each($scope.selectedDevices, function(i, d) {
				var device = {driver: $scope.driver.id, configs: d.configs};
				DevicesService.create(device).then(function(response){
					$alert({content: $scope.deviceOKText, type: 'success'});
				}, function(error) {
					$alert({content: $scope.deviceErrorText, type: 'warning'});
				});
			});

			return $state.go('channelconfigurator.devices.index');
		};
		
	};
	
	DriverScanController.$inject = injectParams;

	angular.module('openmuc.drivers').controller('DriverScanController', DriverScanController);

})();