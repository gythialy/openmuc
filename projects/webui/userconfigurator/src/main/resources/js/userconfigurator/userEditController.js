(function () {

    var injectParams = ['$scope', '$state', '$alert', '$translate', 'AuthService', 'UsersService'];

    var UserEditController = function ($scope, $state, $alert, $translate, AuthService, UsersService) {

        $translate('USER_UPDATED_SUCCESSFULLY').then(text = > $scope.userOKText = text
    )
        ;
        $translate('USER_UPDATED_ERROR').then(text = > $scope.userErrorText = text
    )
        ;
        $translate('USER_PASSWORD_UPDATED_SUCCESSFULLY').then(text = > $scope.userPasswordOKText = text
    )
        ;
        $translate('USER_PASSWORD_UPDATED_ERROR').then(text = > $scope.userPasswordErrorText = text
    )
        ;

        UsersService.getUser(AuthService.currentUsername()).then(user = > {
            $scope.user = user;
        $scope.user.configs.password = null;
    })
        ;

        $scope.updatePassword = function () {
            if (!$scope.edit_password_form.$valid) {
                $scope.edit_password_form.submitted = true;
                return;
            }

            UsersService.updatePassword($scope.user).then(resp = > {
                $alert({content: $scope.userPasswordOKText, type: 'success'});
            return $state.go('userconfigurator.index');
        },
            e =
        >
            $alert({content: $scope.userPasswordErrorText, type: 'warning'})
        )
            ;
        };

    };

    UserEditController.$inject = injectParams;

    angular.module('openmuc.users').controller('UserEditController', UserEditController);

})();