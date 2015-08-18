(function(){

	var injectParams = ['$scope', '$state', '$alert', '$stateParams', 'DevicesService', 'ChannelsService'];
	
	var DeviceScanController = function($scope, $state, $alert, $stateParams, DevicesService, ChannelsService) {
		$scope.device = DevicesService.getDevice($stateParams.id);
		$scope.channels = [];
		$scope.selectedChannels = [];
		
		DevicesService.scan($scope.device, $scope.settings).then(function(response) {
			$.each(response.channels, function(index, channel) {
				$scope.channels.push({configs: channel});
			});
			
			//$scope.scanDriverForm.submitted = false;
		}, function(error) {
		});
		
		$scope.addChannels = function() {
			
		};
		
	};
	
	DeviceScanController.$inject = injectParams;

	angular.module('openmuc.devices').controller('DeviceScanController', DeviceScanController);

})();