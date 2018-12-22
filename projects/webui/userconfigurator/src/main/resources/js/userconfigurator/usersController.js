(function () {

    var injectParams = ['$scope', '$alert', '$translate', 'UsersService'];

    var UsersController = function ($scope, $alert, $translate, UsersService) {

        var userOKText;
        $scope.users = [];

        $translate('USER_DELETED_SUCCESSFULLY').then(text = > userOKText = text
    )
        ;

        UsersService.getUsers().then(users = > $scope.users = users
    )
        ;

        $scope.deleteUser = function (id) {
            var data = {configs: {id: id}};
            UsersService.destroy(data).then(data = > $alert({content: userOKText, type: 'success'})
        )
            ;

            UsersService.getUsers().then(users = > $scope.users = users
        )
            ;
        };

    };

    UsersController.$inject = injectParams;

    angular.module('openmuc.users').controller('UsersController', UsersController);

})();