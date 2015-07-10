(function(){
	
	var injectParams = ['$scope', '$stateParams', '$state', '$alert', '$translate', 'ChannelsService', 'DevicesService'];
	
	var ChannelEditController = function($scope, $stateParams, $state, $alert, $translate, ChannelsService, DevicesService) {

		$translate('CHANNEL_UPDATED_SUCCESSFULLY').then(function(text) {
			$scope.channelOKText = text;
		});
		
		$translate('CHANNEL_UPDATED_ERROR').then(function(text) {
			$scope.channelErrorText = text;
		});
		
		if ($stateParams.id) {
			$scope.channel = ChannelsService.getChannel($stateParams.id);
			$scope.channelId = $stateParams.id;
		} else {
			$scope.channel = [];
		}
		
		$scope.saveChannel = function() {
			if ($scope.channelForm.$valid) {
				ChannelsService.update($scope.channel).then(function(resp){
					$alert({content: $scope.channelOKText, type: 'success'});
					return $state.go('channelconfigurator.channels.index');
				}, function(error) {
					$alert({content: $scope.channelErrorText, type: 'warning'});
					return $state.go('channelconfigurator.channels.index');
				});
			} else {
				$scope.channelForm.submitted = true;
			}
		};
		
	};

	ChannelEditController.$inject = injectParams;

	angular.module('openmuc.channels').controller('ChannelEditController', ChannelEditController);

})();