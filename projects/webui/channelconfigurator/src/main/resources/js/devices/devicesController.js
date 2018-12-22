(function () {

    var injectParams = ['$scope', '$http', '$alert', '$state', '$translate', 'DevicesService', 'DriversService'];

    var DevicesController = function ($scope, $http, $alert, $state, $translate, DevicesService, DriversService) {

        $translate('DEVICE_DELETED_SUCCESSFULLY').then(function (text) {
            $scope.deviceOKText = text;
        });

        $translate('DELETE_CONFIRM_MESSAGE').then(function (confirmMessage) {
            $scope.confirmMessage = confirmMessage;
        });

        $scope.drivers = [];

        DriversService.getDrivers().then(function (drivers) {
            $scope.drivers = drivers;

            resetDrivers();
        });

        $scope.deleteDevice = function (deviceName) {
            if (!confirm($scope.confirmMessage + " " + deviceName + "?")) {
                return;
            }

            DevicesService.destroy(deviceName).then(function (data) {
                $alert({content: $scope.deviceOKText, type: 'success'});
                return $state.go('channelconfigurator.devices.index');
            });

            resetDrivers();
        };

        var resetDrivers = function () {
            $scope.drivers.forEach(driver = > {
                DevicesService.getDevices(driver).then(devices = > driver['devices'] = devices
        )
            ;
        })
            ;
        }
    };

    DevicesController.$inject = injectParams;

    angular.module('openmuc.devices').controller('DevicesController', DevicesController);

})();