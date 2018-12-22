(function () {

    var injectParams = ['$scope', '$location', '$translate', 'notify', 'DevicesService', 'DeviceDataService'];

    var ChannelsAccessToolController = function ($scope, $location, $translate, notify, DevicesService, DeviceDataService) {

        $translate('SELECT_AT_LEAST_ONE_DEVICE').then(text => selectOneDevice = text);

        $scope.devices = [];

        DevicesService.getAllDevices().then(function (devices) {
            $scope.devices = devices
            $scope.devices.forEach((device) => {
                DeviceDataService.getDeviceConfigs(device).then(function (d) {
                    device['configs'] = d;
                });
            }); 
        });

        $scope.checkedDevices = {};

        $scope.accessChannels = () => {
            if (Object.values($scope.checkedDevices).filter(v => v).length === 0) {
                notify({message: selectOneDevice, position: "right", classes: "alert-warning"})
            } else {
                $location.path('/channelaccesstool/access').search($scope.checkedDevices);
            }
        };

		$scope.check = function(value) {
            if (value !== undefined) {
                return $scope.devices.forEach(dev => $scope.checkedDevices[dev.id] = value);
            } else {
                return $scope.devices.filter(dev => $scope.checkedDevices[dev.id] ).length === $scope.devices.length;
            }
        };
    };

    ChannelsAccessToolController.$inject = injectParams;

    angular.module('openmuc.channelaccesstool').controller('ChannelsAccessToolController', ChannelsAccessToolController);

})();
