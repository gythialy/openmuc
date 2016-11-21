(function(){

	var injectParams = ['$scope', '$state', '$alert', '$stateParams', '$translate', 'DevicesService', 'ChannelsService'];

	var DeviceScanController = function($scope, $state, $alert, $stateParams, $translate, DevicesService, ChannelsService) {

		$translate('DEVICE_SCAN_CHANNEL_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.deviceOKText = text;
		});

		$translate('DEVICE_SCAN_CHANNEL_CREATED_ERROR').then(function(text) {
			$scope.deviceErrorText = text;
		});

		$translate('DEVICE_SCAN_NOT_SUPPORTED').then(function(text) {
			$scope.deviceWarningrText = text;
		});



		$scope.device = DevicesService.getDevice($stateParams.deviceId);
		$scope.channels = [];
		$scope.selectedChannels = [];

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
				$alert({content: $scope.deviceWarningrText, type: 'warning'});
				return $state.go('channelconfigurator.devices.index');
			});
//		};

		$scope.addChannels = function() {
			$.each($scope.selectedChannels, function(i, d) {
				var channel = {device: $scope.device.id, configs: d.configs};
				ChannelsService.create(channel).then(function(response){
					$alert({content: $scope.deviceOKText, type: 'success'});
				}, function(error) {
					$alert({content: $scope.deviceErrorText, type: 'warning'});
				});
			});

			return $state.go('channelconfigurator.channels.index');
		};

		$scope.checkAll = function() {
			var elements = document.getElementsByName('checkboxes');

			if ($scope.master) {
				angular.forEach(elements, function(value, key) {
					value.checked = true;
					$scope.selectedChannels[key] = $scope.channels[key];
				});
			}
			else {
				angular.forEach(elements, function(value, key) {
					value.checked = false;
				});
				$scope.selectedChannels.length = 0;
			}
		};

	};

	DeviceScanController.$inject = injectParams;

	angular.module('openmuc.devices').controller('DeviceScanController', DeviceScanController);

})();