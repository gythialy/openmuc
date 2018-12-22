(function(){

	var injectParams = ['$scope', '$location', '$alert', '$translate', 'DevicesService'];

	var ChannelsAccessToolController = function($scope, $location, $alert, $translate, DevicesService) {

		$translate('SELECT_AT_LEAST_ONE_DEVICE').then(text => selectOneDevice = text);

		$scope.devices = [];

		DevicesService.getAllDevices().then(devices => $scope.devices = devices);

		$scope.checkedDevices = {};

		$scope.accessChannels = () => {
			if (Object.keys($scope.checkedDevices).length === 0) {
				$alert({content: selectOneDevice, type: 'warning'});
			} else {
				$location.path('/channelaccesstool/access').search($scope.checkedDevices);
			}
		};

        $scope.checkAll = (checked) => {
            var elements = document.getElementsByName('checkedDevices');
		 	elements.forEach((value, key) => {
				value.checked = checked;
			    $scope.checkedDevices[$scope.devices[key].id] = checked;
		 	});
        };
	};

	ChannelsAccessToolController.$inject = injectParams;

	angular.module('openmuc.channelaccesstool').controller('ChannelsAccessToolController', ChannelsAccessToolController);

})();