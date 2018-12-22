(function(){

	var injectParams = ['$scope', '$rootScope', '$http', '$state', '$translate', 'notify', 'DriversService', 'DevicesService', 'ChannelsService'];

	var ChannelsController = function($scope, $rootScope, $http, $state, $translate, notify, DriversService, DevicesService, ChannelsService) {

		$translate('CHANNEL_DELETED_SUCCESSFULLY').then(function(text) {
			$scope.channelOKText = text;
		});

		$translate('CHANNELS_DELETED_SUCCESSFULLY').then(function(text) {
			$scope.channelsOKText = text;
		});

        $translate('DELETE_CONFIRM_MESSAGE').then(function(confirmMessage) {
            $scope.confirmMessage = confirmMessage;
		});

		$translate('DELETE_SELECTED_CONFIRM_MESSAGE').then(function(confirmSelectedMessage) {
            $scope.confirmSelectedMessage = confirmSelectedMessage;
		});
		
		var master = 'false';
		$rootScope.selectedChannels = [];

		$scope.drivers = [];
		DriversService.getDrivers().then(function(drivers){
			$scope.drivers = drivers;

			$scope.drivers.forEach((driver) => {
				DevicesService.getDevices(driver).then((devices) => {
					driver.devices = devices;

					devices.forEach((device) => {
						ChannelsService.getChannels(device).then((channels) => device.channels = channels);
					});

				});
			});
		});

		$scope.deleteChannel = function(channelId) {
			if (!confirm($scope.confirmMessage + ' ' + channelId + '?')) {
				return;
			}

			ChannelsService.destroy(channelId).then((data) => {
				$scope.drivers.forEach((driver) => {
					driver.devices.forEach((device) => {
						ChannelsService.getChannels(device).then((channels) => {
							device.channels = channels;
						});
					});
				});

				notify({message: $scope.channelOKText, position: "right", classes: "alert-success"});
				return $state.go('channelconfigurator.channels.index',{}, {reload: true});
			});
		};

		$scope.deleteSelectedChannels = function() {
			if (!confirm($scope.confirmSelectedMessage)) {
				return;
			}

			angular.forEach($rootScope.selectedChannels, function(value, key) {
				ChannelsService.destroy(value.id).then;
			});

			notify({message: $scope.channelsOKText, position: "right", classes: "alert-success"});
			return $state.go('channelconfigurator.channels.index',{}, {reload: true});
		};

        $scope.checkAll = function(driverId, deviceId) {
			$rootScope.selectedChannels = [];
			var uncheck = document.getElementsByClassName("check");
			var nameAll = deviceId + ' checkboxesAll';
			if (document.getElementById(nameAll).checked == true && master == 'true'){
				master = 'false';
			}
			angular.forEach(uncheck, function(value, key) {
				value.checked = false;
			});
			document.getElementById(nameAll).checked = true;
			driver = $scope.drivers.find(o => o.id === driverId);
			device = driver.devices.find(o => o.id === deviceId);
			var name = device.id + ' checkboxes';
			var elements = document.getElementsByName(name);
			var elementsAll = document.getElementsByName('checkboxesAll');
			if (master == 'true') {
				angular.forEach(elements, function(value, key) {
					value.checked = false;
				});
				$rootScope.selectedChannels.length = 0;
				document.getElementById(nameAll).checked = false;
				master = 'false';
			}
			else {
				angular.forEach(elements, function(value, key) {
					value.checked = true;
					$rootScope.selectedChannels[key] = device.channels[key];
				});
				angular.forEach(elementsAll, function(value, key) {
				});
				master = 'true';
			}
		};

	};

	ChannelsController.$inject = injectParams;

	angular.module('openmuc.channels').controller('ChannelsController', ChannelsController);

})();
