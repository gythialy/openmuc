(function(){

	var injectParams = ['$scope', '$http', '$alert', '$state', '$translate', 'DriversService', 'DevicesService', 'ChannelsService'];
	
	var ChannelsController = function($scope, $http, $alert, $state, $translate, DriversService, DevicesService, ChannelsService) {

		$translate('CHANNEL_DELETED_SUCCESSFULLY').then(function(text) {
			$scope.channelOKText = text;
		});

        $translate('DELETE_CONFIRM_MESSAGE').then(function(confirmMessage) {
            $scope.confirmMessage = confirmMessage;
        });

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

				$alert({content: $scope.channelOKText, type: 'success'});
				return $state.go('channelconfigurator.channels.index');
			});
		};

	};

	ChannelsController.$inject = injectParams;

	angular.module('openmuc.channels').controller('ChannelsController', ChannelsController);
	
})();