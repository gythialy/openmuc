(function () {

    var injectParams = ['$http', 'SETTINGS', 'RestServerAuthService'];

    var ChannelDataService = function ($http, SETTINGS, RestServerAuthService) {
        var configReq = function (channel, configField) {
            return {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.id + SETTINGS.CONFIGS_URL + (configField ? configField : ''),
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
        };


        this.getChannelConfig = function (channel, config) {
            var configPath;
            if (config !== undefined) {
                configPath = '/' + config;
            }

            return $http(configReq(channel, configPath))
                .then(r = > r.data.configs
        )
            ;
        };

        this.channelHasHistoricValues = function (channel) {
            return $http(configReq(channel, '/loggingInterval'))
                .then(r = > true, e =
        >
            false
        )
            ;
        };

        this.getChannelData = function (channel) {
            return $http(configReq(channel)).then(response = > response.data.configs
        )
            ;
        };

        this.getChannelDataValues = function (channel) {
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.id,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            return $http(req).then(function (response) {
                return response.data.record;
            });
        };
    };

    ChannelDataService.$inject = injectParams;

    angular.module('openmuc.channels').service('ChannelDataService', ChannelDataService);

})();
