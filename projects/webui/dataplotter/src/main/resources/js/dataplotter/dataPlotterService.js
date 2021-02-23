(function () {

    var injectParams = ['$http', 'SETTINGS', 'RestServerAuthService'];

    var DataPlotterService = function ($http, SETTINGS, RestServerAuthService) {

        this.getConfigFile = function () {
            var url = SETTINGS.DATAPLOTTER_CONFIG_URL;

            var req = {
                method: 'GET',
                dataType: 'json',
                url: url,
                headers: {
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(r => r.data.plotters);
        };

    };

    DataPlotterService.$inject = injectParams;

    angular.module('openmuc.dataplotter').service('DataPlotterService', DataPlotterService);

})();
