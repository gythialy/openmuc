(function(){

	var injectParams = ['$scope', '$location', '$alert', '$translate', 'DevicesService'];

	var ChannelsAccessToolController = function($scope, $location, $alert, $translate, DevicesService) {

		$translate('SELECT_AT_LEAST_ONE_DEVICE').then(function(text) {
			$scope.selectOneDevice = text;
		});

		$scope.devices = [];

		DevicesService.getAllDevices().then(function(devices){
			return $scope.devices = devices;
		});

		$scope.checkedDevices = {};

		$scope.accessChannels = function() {
			if (jQuery.isEmptyObject($scope.checkedDevices)) {
				$alert({content: $scope.selectOneDevice, type: 'warning'});
			} else {
				$location.path('/channelaccesstool/access').search($scope.checkedDevices);
			}
		};

        $scope.checkAll = function () {
            var elements = document.getElementsByName('checkedDevices');
            if ($scope.checkAllDevices) {
                 angular.forEach(elements, function(value, key) {
                     value.checked = true;
                     $scope.checkedDevices[$scope.devices[key].id] = 'on';
                 });
            }
            else {
                angular.forEach(elements, function(value, key) {
                    value.checked = false;
                    $scope.checkedDevices[$scope.devices[key].id] = 'off';
                });
            }
        }

	};

	ChannelsAccessToolController.$inject = injectParams;

	angular.module('openmuc.channelaccesstool').controller('ChannelsAccessToolController', ChannelsAccessToolController);

})();