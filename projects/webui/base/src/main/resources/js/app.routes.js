(function () {

    var app = angular.module('openmuc');

    app.config(['$stateProvider', '$urlRouterProvider',
        function ($stateProvider, $urlRouterProvider) {
            $urlRouterProvider.otherwise('/');

            $stateProvider.state('home', {
                url: '/',
                templateUrl: 'openmuc/html/sessions/new.html',
                controller: 'LoginController',
                requireLogin: false
            }).state('dashboard', {
                url: '/dashboard',
                templateUrl: 'openmuc/html/dashboard/index.html',
                controller: 'DashboardController',
                requireLogin: true,
                resolve: {
                    openmuc: ['AppsDependenciesService', function (AppsDependenciesService) {
                        return AppsDependenciesService.loadDependencies();
                    }]
                }
            })

        }]);

    app.run(['$rootScope', '$state', 'notify', 'AuthService', function ($rootScope, $state, notify, AuthService) {
        $rootScope.$on("$stateChangeStart", function (event, toState, toParams, fromState, fromParams) {
            if (!toState.requireLogin || AuthService.isLoggedIn()) {
                return;
            }
            notify({message: 'You need to be authenticated to see this page!', position: "right", classes: "alert-warning"});
            AuthService.redirectToLogin();
            event.preventDefault();
        });
    }]);

})();
