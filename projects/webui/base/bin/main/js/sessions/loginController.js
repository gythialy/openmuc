(function () {

    var injectParams = ['$scope', '$state', 'notify', '$translate', 'AuthService'];

    var LoginController = function ($scope, $state, notify, $translate, AuthService) {

        $translate('LOGIN_CREDENTIALS_INCORRECT').then(text => $scope.loginCredentialsErrorErrorText = text);

        if (AuthService.isLoggedIn()) {
            $state.go('dashboard');
        } else {
            $scope.user = {};

            $scope.login = function () {
                AuthService.login($scope.user).then(res => {
                    $state.go('dashboard');
                }, err => {
                    notify({message: $scope.loginCredentialsErrorText, position: "right", classes: "alert-warning"})
                    $state.go('home');
                });
            }
        }
    };

    LoginController.$inject = injectParams;

    angular.module('openmuc.sessions').controller('LoginController', LoginController);

})();