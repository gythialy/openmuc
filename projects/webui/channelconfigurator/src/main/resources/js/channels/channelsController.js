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

			$.each($scope.drivers, function(i, driver) {
				DevicesService.getDevices(driver).then(function(devices){
					$scope.drivers[i]['devices'] = devices;
					
					$.each(devices, function(j, device) {
						ChannelsService.getChannels(device).then(function(channels){
							$scope.drivers[i]['devices'][j]['channels'] = channels;
						});
					});
					
				});
			});
		});

		$scope.deleteChannel = function(channelId) {
			if (confirm($scope.confirmMessage + " " + channelId + "?") == true) {
				ChannelsService.destroy(channelId).then(function(data) {
					$.each($scope.drivers, function(i, driver) {
						$.each(driver['devices'], function(j, device) {
							ChannelsService.getChannels(device).then(function(channels){
								$scope.drivers[i]['devices'][j]['channels'] = channels;
							});
						});
					});

					$alert({content: $scope.channelOKText, type: 'success'});
					return $state.go('channelconfigurator.channels.index');
				});
			}
		};
		
	};

	ChannelsController.$inject = injectParams;

	angular.module('openmuc.channels').controller('ChannelsController', ChannelsController);
	
})();