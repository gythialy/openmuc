(function(){
	
	var injectParams = ['$scope', '$stateParams', '$state', '$alert', '$translate', 'DevicesService', 'DriversService'];
	
	var DeviceEditController = function($scope, $stateParams, $state, $alert, $translate, DevicesService, DriversService) {
		
		$translate('DEVICE_UPDATED_SUCCESSFULLY').then(function(text) {
			$scope.deviceOKText = text;
		});
		
		$translate('DEVICE_UPDATED_ERROR').then(function(text) {
			$scope.deviceErrorText = text;
		});
		
		if ($stateParams.id) {
			$scope.device = DevicesService.getDevice($stateParams.id);
			$scope.deviceId = $stateParams.id;
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
		
	};

	DeviceEditController.$inject = injectParams;

	angular.module('openmuc.devices').controller('DeviceEditController', DeviceEditController);

})();