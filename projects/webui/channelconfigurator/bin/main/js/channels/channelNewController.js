(function(){

	var injectParams = ['$scope', '$stateParams', '$state', '$translate', 'notify', 'ChannelsService', 'DevicesService'];

	var ChannelNewController = function($scope, $stateParams, $state, $translate, notify, ChannelsService, DevicesService) {

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

		$scope.compareChannels = function(field) {
			return true;
		};

		$scope.saveChannel = function() {

			if ($scope.channelForm.$valid) {
				ChannelsService.create($scope.channel).then(function(resp){
					notify({message: $scope.channelOKText, position: "right", classes: "alert-success"});
					return $state.go('channelconfigurator.channels.index');
				}, function(error) {
					notify({message: $scope.channelErrorText + error.statusText, position: "right", classes: "alert-warning"});
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
