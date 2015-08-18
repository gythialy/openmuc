(function(){
	
	var injectParams = ['$scope', '$stateParams', '$state', '$alert', '$translate', 'ChannelsService', 'DevicesService'];
	
	var ChannelNewController = function($scope, $stateParams, $state, $alert, $translate, ChannelsService, DevicesService) {

		$translate('CHANNEL_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.channelOKText = text;
		});
		
		$translate('CHANNEL_CREATED_ERROR').then(function(text) {
			$scope.channelErrorText = text;
		});

		if ($stateParams.deviceId) {
			$scope.device = DevicesService.getDevice($stateParams.deviceId);
		} else {
			$scope.device = [];
		}

		$scope.channel = {
			device: $scope.device.id,
			configs: {
				disabled: false
			}
		};

		$scope.saveChannel = function() {
			
			if ($scope.channelForm.$valid) {
				ChannelsService.create($scope.channel).then(function(resp){
					$alert({content: $scope.channelOKText, type: 'success'});
					return $state.go('channelconfigurator.channels.index');
				}, function(error) {
					$alert({content: $scope.channelErrorText + error.statusText, type: 'warning'});
					return $state.go('channelconfigurator.channels.index');
				});
			} else {
				$scope.channelForm.submitted = true;
			}
		};
				
	};

	ChannelNewController.$inject = injectParams;

	angular.module('openmuc.channels').controller('ChannelNewController', ChannelNewController);

})();