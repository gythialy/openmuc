(function () {

    var injectParams = ['$rootScope', '$http', '$state', 'SETTINGS'];

    var AuthService = function ($rootScope, $http, $state, SETTINGS) {
        var userName;
        var auth;

        this.login = function (credentials) {
            var req = {
                method: 'POST',
                url: SETTINGS.LOGIN_URL,
                data: $.param({user: credentials.user, pwd: credentials.pwd}),
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            };
            userName = credentials.user;
            var p = $http(req);
            p.then(r = > {
                auth = 'Basic ' + btoa(credentials.user + ":" + credentials.pwd);
        })
            ;
            return p;
        };

        this.currentUsername = function () {
            return userName;
        };

        this.getRestAuth = function () {
            return auth;
        };

        this.redirectToLogin = function () {
            $state.go('home');
        };

        this.isLoggedIn = function () {
            return auth != null;
        };

        this.logout = function () {
            auth = null;
        };

    };

    AuthService.$inject = injectParams;

    angular.module('openmuc.auth').service('AuthService', AuthService);

})();
