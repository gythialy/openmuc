(function(){

    var app = angular.module('openmuc');

    app.config(['$stateProvider', '$urlRouterProvider',
        function($stateProvider, $urlRouterProvider) {
            $stateProvider.
            state('simpledemovisualisation', {
                url: '/simpledemovisualisation',
                templateUrl: 'simpledemovisualisation/html/index.html',
                requireLogin: true
            }).
            state('simpledemovisualisation.index', {
                url: '/',
                templateUrl: 'simpledemovisualisation/html/graphic.html',
                controller: 'VisualisationController',
                requireLogin: true,
                resolve: {
                    openmuc: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(
                            {
                                name: 'openmuc.simpledemovisualisation',
                                files: ['openmuc/js/channels/channelsService.js',
                                    'openmuc/js/channels/channelDataService.js',
                                    'simpledemovisualisation/css/simpledemovisualisation/main.css',
                                   'simpledemovisualisation/js/visu/VisualisationController.js']
                            }
                        )
                    }
                }
            })
        }]);

})();