(function () {

    var injectParams = ['$scope', '$translate', 'notify', 'UsersService'];

    var UsersController = function ($scope, $translate, notify, UsersService) {

        var userOKText;
        $scope.users = [];

        $translate('USER_DELETED_SUCCESSFULLY').then(text => userOKText = text);

        UsersService.getUsers().then(users => $scope.users = users);

        $scope.deleteUser = function (id) {
            var data = {configs: {id: id}};
            UsersService.destroy(data).then(data => notify({message: userOKText, position: "right", classes: "alert-success"}));

            UsersService.getUsers().then(users => $scope.users = users);
        };

    };

    UsersController.$inject = injectParams;

    angular.module('openmuc.users').controller('UsersController', UsersController);

})();
