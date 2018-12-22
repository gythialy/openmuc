(function () {

    var app = angular.module('openmuc');

    app.config(['$stateProvider', '$urlRouterProvider',
        function ($stateProvider, $urlRouterProvider) {
            $stateProvider.state('channelaccesstool', {
                url: "/channelaccesstool",
                templateUrl: "channelaccesstool/html/index.html",
                requireLogin: true,
                resolve: {
                    users: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(
                            {
                                name: "openmuc.channelaccesstool",
                                files: ['openmuc/js/devices/devicesService.js',
                                    'openmuc/js/devices/deviceDataService.js',
                                    'channelaccesstool/js/channelaccesstool/channelsAccessToolFilters.js',
                                    'channelaccesstool/js/channelaccesstool/channelsAccessToolController.js']
                            }
                        )
                    }
                }
            }).state('channelaccesstool.index', {
                url: '/',
                templateUrl: 'channelaccesstool/html/list.html',
                controller: 'ChannelsAccessToolController',
                requireLogin: true
            }).state('channelaccesstool.access', {
                url: "/access",
                templateUrl: 'channelaccesstool/html/access.html',
                controller: 'ChannelsAccessController',
                requireLogin: true,
                resolve: {
                    users: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(
                            {
                                name: "openmuc.channelaccesstool",
                                files: ['openmuc/js/channels/channelsService.js',
                                    'openmuc/js/channels/channelDataService.js',
                                    'channelaccesstool/js/channelaccesstool/channelsAccessController.js']
                            }
                        )
                    }
                }
            })
        }]);

})();
