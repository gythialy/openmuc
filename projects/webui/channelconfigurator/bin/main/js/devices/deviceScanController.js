(function(){

	var injectParams = ['$scope', '$state', '$stateParams', '$translate', 'notify', 'DevicesService', 'ChannelsService'];

	var DeviceScanController = function($scope, $state, $stateParams, $translate, notify, DevicesService, ChannelsService) {

		$translate('DEVICE_SCAN_CHANNEL_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.deviceOKText = text;
		});

		$translate('DEVICE_SCAN_CHANNEL_CREATED_ERROR').then(function(text) {
			$scope.deviceErrorText = text;
		});

		$translate('DEVICE_SCAN_NOT_SUPPORTED').then(function(text) {
			$scope.deviceWarningrText = text;
		});

		$translate('DEVICE_SCAN_CHANNELS_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.devicesOKText = text;
		});

		$translate('DEVICE_SCAN_CHANNELS_CREATED_ERROR').then(function(text) {
			$scope.devicesErrorText = text;
		});

		$scope.device = DevicesService.getDevice($stateParams.deviceId);
		$scope.channels = [];
		$scope.selectedChannels = [];
		var addChannelsError;

//		$scope.scanDevice = function() {
//			$scope.scanDeviceForm.submitted = true;

			DevicesService.scan($scope.device, $scope.settings).then(function(response) {
				$scope.channels = [];
				var i = 0;
				$.each(response.channels, function(index, channel) {
					channel.id  = $scope.device.id + '_channel_' + i;
					$scope.channels.push({configs: channel});
					++i;
				});

//				$scope.scanDeviceForm.submitted = false;
			}, function(error) {
				notify({message: $scope.deviceWarningrText, position: "right", classes: "alert-warning"});
				return $state.go('channelconfigurator.devices.index');
			});
//		};

		$scope.addChannels = function() {
			angular.forEach($scope.selectedChannels, function(d, i) {
				var channel = {device: $scope.device.id, configs: d.configs};
				ChannelsService.create(channel).then(function(error) {
					addChannelsError = true;
				});
			});
			if ($scope.selectedChannels.length > 1){
				if (addChannelsError === true){
					notify({message: $scope.devicesErrorText, position: "right", classes: "alert-warning"});
				}
				else {
					notify({message: $scope.devicesOKText, position: "right", classes: "alert-success"});
				}
			}
			else{
				if (addChannelsError === true){
					notify({message: $scope.deviceErrorText, position: "right", classes: "alert-warning"});
				}
				else {
					notify({message: $scope.deviceOKText, position: "right", classes: "alert-success"});
				}
			}
			return $state.go('channelconfigurator.channels.index');
		};

		$scope.checkAll = function() {
			var elements = document.getElementsByName('checkboxes');

			if ($scope.master) {
				angular.forEach(elements, function(value, key) {
					value.checked = false;
				});
				$scope.selectedChannels.length = 0;
			}
			else {
				angular.forEach(elements, function(value, key) {
					value.checked = true;
					$scope.selectedChannels[key] = $scope.channels[key];
				});
			}
		};

	};

	DeviceScanController.$inject = injectParams;

	angular.module('openmuc.devices').controller('DeviceScanController', DeviceScanController);

})();
