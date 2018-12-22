(function () {

    var app = angular.module('openmuc');

    app.config(['$stateProvider', '$urlRouterProvider',
        function ($stateProvider, $urlRouterProvider) {
            $stateProvider.state('mediaviewer', {
                url: '/mediaviewer',
                templateUrl: 'mediaviewer/html/index.html',
                requireLogin: true
            }).state('mediaviewer.index', {
                url: '/',
                templateUrl: 'mediaviewer/html/mediaviewer.html',
                controller: 'MediaViewerController',
                requireLogin: true,
                resolve: {
                    openmuc: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(
                            {
                                name: "openmuc.mediaviewer",
                                files: ['mediaviewer/js/mediaviewer/mediaViewerService.js',
                                    'mediaviewer/js/mediaviewer/mediaViewerController.js',
                                    'mediaviewer/css/mediaviewer/mediaviewer.css']
                            }
                        )
                    }
                }
            })
        }]);

})();
