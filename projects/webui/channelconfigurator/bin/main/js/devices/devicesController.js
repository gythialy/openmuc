(function () {

    var injectParams = ['$scope', '$http', '$state', '$translate', '$interval', 'notify', 'DevicesService', 'DriversService'];

    var DevicesController = function ($scope, $http, $state, $translate, $interval, notify, DevicesService, DriversService) {

        $translate('DEVICE_DELETED_SUCCESSFULLY').then(function (text) {
            $scope.deviceOKText = text;
        });

        $translate('DELETE_CONFIRM_MESSAGE').then(function (confirmMessage) {
            $scope.confirmMessage = confirmMessage;
        });

        var resetDrivers = function () {
            $scope.drivers.forEach(driver => {
                DevicesService.getDevices(driver).then(devices => driver['devices'] = devices);
            });
        }

        $scope.drivers = [];
        $scope.interval ='';
        $scope.updateDrivers = [];

        DriversService.getDrivers().then(function (drivers) {
            $scope.drivers = drivers;
            resetDrivers();
        });

        DriversService.getDrivers().then(function (drivers) {
            $scope.updateDrivers = drivers;
            resetDrivers();
        });

        $scope.interval = $interval(() => {
            $scope.updateDrivers.forEach(async function(driver) {
                await DevicesService.getDevices(driver).then(devices => driver['devices'] = devices);
            });
            $scope.drivers.forEach(function(driver){
                var updateDriver = $scope.updateDrivers.find(o => o.id === driver.id);
                var length = driver.devices.length;
                var index = 0;
                if (typeof updateDriver.devices != "undefined"){
                    driver.devices.forEach(function(device){
                        if (index < length){
                            if (device.id != updateDriver.devices[index].id){
                                device.id = updateDriver.devices[index].id;
                            }
                            for (var property in device.configs){
                                if(device.configs[property] != updateDriver.devices[index].configs[property]){
                                    device.configs[property] = updateDriver.devices[index].configs[property];
                                }
                            }
                            for (var property in device.data){
                                if(device.data[property] != updateDriver.devices[index].data[property]){
                                    device.data[property] = updateDriver.devices[index].data[property];
                                }
                            }
                            index ++;
                        }
                    });
                }    
            });
        }, 5000);

        $scope.deleteDevice = function (deviceName) {
            if (!confirm($scope.confirmMessage + " " + deviceName + "?")) {
                return;
            }

            DevicesService.destroy(deviceName).then(function (data) {
                notify({message: $scope.deviceOKText, position: "right", classes: "alert-success"});
                return $state.go('channelconfigurator.devices.index');
            });

            resetDrivers();
        };

        $scope.$on('$destroy', () => $interval.cancel($scope.interval));
    };

    DevicesController.$inject = injectParams;

    angular.module('openmuc.devices').controller('DevicesController', DevicesController);

})();
