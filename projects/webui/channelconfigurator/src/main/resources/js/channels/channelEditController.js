(function(){
	
	var injectParams = ['$scope', '$stateParams', '$state', '$alert', '$translate', 'ChannelsService', 'DriversService'];
	
	var ChannelEditController = function($scope, $stateParams, $state, $alert, $translate, ChannelsService, DriversService) {

		$scope.driverInfo = {};

		$translate('CHANNEL_UPDATED_SUCCESSFULLY').then((text) => channelOKText = text);
		$translate('CHANNEL_UPDATED_ERROR').then((text) => channelErrorText = text);

		var channelId = $stateParams.id;
		if (channelId) {
			$scope.channel = ChannelsService.getChannel(channelId);
			$scope.channelId = channelId;
		} else {
			$scope.channel = [];
		}
		
		$scope.saveChannel = function() {
			if ($scope.channelForm.$valid) {
				ChannelsService.update($scope.channel).then(function(resp){
					$alert({content: channelOKText, type: 'success'});
					return $state.go('channelconfigurator.channels.index');
				}, function(error) {
					$alert({content: channelErrorText, type: 'warning'});
					return $state.go('channelconfigurator.channels.index');
				});
			} else {
				$scope.channelForm.submitted = true;
			}
		};

		$scope.getDriverInfo = function() {
			var channelId = $stateParams.id;

			if (channelId) {
                ChannelsService.getChannelDriverId(channelId).then(function(driverId) {
					if (!driverId || driverId.length === 0) {
						return;
					}
					DriversService.getInfos(driverId).then((driverInfo) => $scope.driverInfo = driverInfo);
				});
			}

			if(Object.keys($scope.driverInfo).length == 0) {
				$scope.driverInfo.channelAddressSyntax = 'N.A.';
			}
		};
		
	};

	ChannelEditController.$inject = injectParams;

	angular.module('openmuc.channels').controller('ChannelEditController', ChannelEditController);

})();