(function () {

    var injectParams = ['$scope', '$state', '$translate', '$interval', 'notify', 'DriversService'];

    var DriversController = function ($scope, $state, $translate, $interval, notify, DriversService) {

        $translate('DRIVER_DELETED_SUCCESSFULLY').then(text => $scope.driverOKText = text);

        $translate('DELETE_CONFIRM_MESSAGE').then(confirmMessage => $scope.confirmMessage = confirmMessage);

        $scope.drivers = [];

        DriversService.getDrivers().then(function (drivers) {
            $scope.drivers = drivers;
        });

        $scope.interval = $interval(() => {
            DriversService.getDrivers().then(function (drivers) {
                updateDrivers = drivers;
            });
            if (typeof updateDrivers != "undefined"){
                $scope.drivers = updateDrivers;
            }
        }, 5000);

        $scope.deleteDriver = function (id) {
            if (!confirm($scope.confirmMessage + " " + id + "?")) {
                return;
            }

            DriversService.destroy(id).then(function (data) {
                notify({message: $scope.driverOKText, position: "right", classes: "alert-success"});

                DriversService.getDrivers().then(function (drivers) {
                    $scope.drivers = drivers;
                });
            });

        };

        $scope.$on('$destroy', () => $interval.cancel($scope.interval));

    };

    DriversController.$inject = injectParams;

    angular.module('openmuc.drivers').controller('DriversController', DriversController);

}());
