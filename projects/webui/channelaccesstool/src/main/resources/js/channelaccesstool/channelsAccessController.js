(function(){
	
	var injectParams = ['$scope', '$location', '$alert', '$translate', '$interval', 'DevicesService', 'ChannelsService', 'ChannelDataService'];
	
	var ChannelsAccessController = function($scope, $location, $alert, $translate, $interval, DevicesService, ChannelsService, ChannelDataService) {
		
		$translate('CHANNEL_VALUE_UPDATED_SUCCESSFULLY').then(function(text) {
			$scope.channelWriteValueOKText = text;
		});
		
		$translate('CHANNEL_VALUE_UPDATED_ERROR').then(function(text) {
			$scope.channelWriteValueErrorText = text;
		});
		
		$scope.checkedDevices = [];
		
		$scope.interval = "";
		
		DevicesService.getAllDevicesIds().then(function(devices){
			
			$.each(devices, function(index, deviceId) {
				if ($location.search()[deviceId] == "on") {
					$scope.checkedDevices.push({id: deviceId});
				}
			});

			$.each($scope.checkedDevices, function(index, device) {

				ChannelsService.getChannels(device).then(function(channels){
					device['channels'] = channels;
					
					// add new value for write form
					$.each(device['channels'], function(j, channel) {
						channel['newValue'] = "";
					});
				});
			});
									
			$scope.interval = $interval(function(){
				$.each($scope.checkedDevices, function(i, device) {
					DevicesService.getDeviceRecords(device).then(function(response){
						var records = response.data.records;
						$.each(device['channels'], function(j, channel) {
							channel['records'] = records[j].record; // TODO: check IDs to see if is it the right channel
						});
					});
				});
			}, 1000);	
		});

		$scope.writeValue = function() {
			$.each($scope.checkedDevices, function(i, device) {
				$.each(device['channels'], function(j, channel) {
					if (channel.newValue) {
						ChannelsService.writeValue(channel.id, channel.data.valueType, channel.newValue).then(function(resp) {
							$alert({content: $scope.channelWriteValueOKText, type: 'success'});
						}, function(error) {
							$alert({content: $scope.channelWriteValueErrorText, type: 'warning'});
						});
						
						channel.newValue = "";						
					}
				});
			});			
		};
		
	    $scope.$on('$destroy', function () { 
	    	$interval.cancel($scope.interval); 
	    });

	};

	ChannelsAccessController.$inject = injectParams;

	angular.module('openmuc.channelaccesstool').controller('ChannelsAccessController', ChannelsAccessController);

})();