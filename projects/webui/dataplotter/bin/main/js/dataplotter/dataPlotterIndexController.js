(function () {

    var injectParams = ['$scope', '$q', 'DataPlotterService'];

    var DataPlotterIndexController = function ($scope, $q, DataPlotterService) {

        DataPlotterService.getConfigFile().then(function (response) {
            $scope.confFile = response;
        });

        $scope.getPlotter = function (name) {
            var res = $scope.confFile.find((plotter) => plotter.name === name);
            return res ? res : '';
        };

    };

    DataPlotterIndexController.$inject = injectParams;

    angular.module('openmuc.dataplotter').controller('DataPlotterIndexController', DataPlotterIndexController);

})();
