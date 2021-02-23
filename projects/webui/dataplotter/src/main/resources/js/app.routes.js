(function(){

    var app = angular.module('openmuc');

    app.config(['$stateProvider', '$urlRouterProvider',
        function($stateProvider, $urlRouterProvider) {
            $stateProvider.
            state('dataplotter', {
                url: '/dataplotter',
                templateUrl: 'dataplotter/html/index.html',
                requireLogin: true,
                controller: 'DataPlotterIndexController',
                resolve: {
                    openmuc: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(
                            {
                                name: "openmuc.dataplotter",
                                serie: true,
                                files: ['openmuc/js/channels/channelsService.js',
                                    'openmuc/js/channels/channelDataService.js',
                                    'dataplotter/js/dataplotter/dataPlotterService.js',
                                    'dataplotter/js/dataplotter/dataPlotterIndexController.js',
                                    'dataplotter/js/dataplotter/plottingTabsDirective.js',
                                    'dataplotter/css/main.css',
                                    'dataplotter/css/libs/d3/nv.d3.css',
                                    'dataplotter/js/libs/d3/d3.v3.min.js',
                                    'dataplotter/js/libs/d3/nv.d3.min.js',
                                    'openmuc/js/libs/checklistmodel/checklist-model.min.js']
                            }
                        )
                    }
                }
            }).
            state('dataplotter.index', {
                url: '/',
                templateUrl: 'dataplotter/html/list.html',
                requireLogin: true
            }).
            state('dataplotter.data', {
                url: '/data/:name',
                templateUrl: 'dataplotter/html/dataPlotter.html',
                controller: 'DataPlotterController',
                requireLogin: true,
                resolve: {
                    openmuc: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(
                            {
                                name: "openmuc.dataplotter",
                                files: ['dataplotter/js/dataplotter/dataPlotterController.js']
                            }
                        )
                    }
                }
            }).
            state('dataplotter.live', {
                url: '/live/:name',
                templateUrl: 'dataplotter/html/livePlotter.html',
                controller: 'LivePlotterController',
                requireLogin: true,
                resolve: {
                    openmuc: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(
                            {
                                name: "openmuc.dataplotter",
                                files: ['dataplotter/js/dataplotter/livePlotterController.js']
                            }
                        )
                    }
                }
            })
        }]);

})();
