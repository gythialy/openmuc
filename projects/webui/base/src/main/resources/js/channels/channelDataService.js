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

        this.getChannelConfig = async function (channel, config) {
            var configPath;
            var implemented;
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.id + SETTINGS.CONFIGS_URL,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };

            const response = await $http(req);
            implemented = response.data.configs.hasOwnProperty(config);
            if (implemented === true){
                if (config !== undefined) {
                    configPath = '/' + config;
                }
                var configResponse = await $http(configReq(channel, configPath));
                return configResponse.data.configs;
            }
            else {
                console.warn("No field " + config + " defined in " + channel.id);
                return false;
            }
        };

        this.channelHasHistoricValues = async function (channel) {
            var implemented;
            var req = {
                method: 'GET',
                url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.id + SETTINGS.CONFIGS_URL,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': RestServerAuthService.getAuthHash()
                }
            };
            const response = await $http(req);
            implemented = response.data.configs.hasOwnProperty('loggingInterval');
            if (implemented === true){
                configResponse = await $http(configReq(channel, '/loggingInterval'));
                return r => true;
            }
            else {
                console.warn("No loggingInterval was defined for " + channel.id);
                return false;
               }
        };

        this.getChannelData = function (channel) {
            return $http(configReq(channel)).then(response => response.data.configs);
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
