(function () {

    var injectParams = ['$scope', '$state', '$stateParams', '$translate', 'notify', 'DriversService', 'DriverDataService'];

    var DriverInfosController = function ($scope, $state, $stateParams, $translate, notify, DriversService) {


        var deviceWarningrText;
        $translate('DRIVER_INFO_FAILED').then(text => deviceWarningrText = text);

        $scope.driver = DriversService.getDriver($stateParams.id);
        $scope.driver.infos = {};

        $scope.showInfos = function () {
            $scope.infosDriverForm.submitted = true;

            DriversService.getInfos($scope.driver.id).then(response => $scope.driver.infos = response,
                e => {
                    notify({message: deviceWarningrText, position: "right", classes: "alert-warning"});
                    return $state.go('channelconfigurator.drivers.index');
                });
        };

    };

    DriverInfosController.$inject = injectParams;

    angular.module('openmuc.drivers').controller('DriverInfosController', DriverInfosController);

})();
