(function(){

    var injectParams = ['$scope', '$state', '$alert', '$stateParams', '$translate','DriversService','DriverDataService'];

    var DriverInfosController = function($scope, $state, $alert, $stateParams ,$translate, DriversService) {

        $scope.driver = DriversService.getDriver($stateParams.id);
        $scope.driver.infos = {};

        $scope.showInfos = function() {
            $scope.infosDriverForm.submitted = true;

            DriversService.getInfos($scope.driver.id).then(function(response) {
                $scope.driver.infos = response;
            }, function(error) {
                $alert({content: $scope.deviceWarningrText, type: 'warning'});
                return $state.go('channelconfigurator.drivers.infos');
            });
        };

    };

    DriverInfosController.$inject = injectParams;

    angular.module('openmuc.drivers').controller('DriverInfosController', DriverInfosController);

})();