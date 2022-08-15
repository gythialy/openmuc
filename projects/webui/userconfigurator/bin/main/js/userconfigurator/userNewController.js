(function(){

	var injectParams = ['$scope', '$state', '$translate', 'notify', 'UsersService'];

	var UserNewController = function($scope, $state, $translate, notify, UsersService) {

		$translate('USER_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.userOKText = text;
		});

		$translate('USER_CREATED_ERROR').then(function(text) {
			$scope.userErrorText = text;
		});

		$scope.user = {configs: {}};

		$scope.saveUser = function() {
			if ($scope.userForm.$valid) {
				UsersService.create($scope.user).then(function(resp){
					notify({message: $scope.userOKText, position: "right", classes: "alert-success"});
					return $state.go('userconfigurator.index');
				}, function(error) {
					notify({message: $scope.userErrorText, position: "right", classes: "alert-warning"});
				});
			} else {
				$scope.userForm.submitted = true;
			}
		};

	};

	UserNewController.$inject = injectParams;

	angular.module('openmuc.users').controller('UserNewController', UserNewController);

})();
