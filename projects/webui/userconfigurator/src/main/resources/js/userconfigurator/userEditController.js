(function(){
	
	var injectParams = ['$scope', '$stateParams', '$state', '$alert', '$translate', 'AuthService', 'UsersService'];
	
	var UserEditController = function($scope, $stateParams, $state, $alert, $translate, AuthService, UsersService) {	

		$translate('USER_UPDATED_SUCCESSFULLY').then(function(text) {
			$scope.userOKText = text;
		});
		
		$translate('USER_UPDATED_ERROR').then(function(text) {
			$scope.userErrorText = text;
		});

		$translate('USER_PASSWORD_UPDATED_SUCCESSFULLY').then(function(text) {
			$scope.userPasswordOKText = text;
		});

		$translate('USER_PASSWORD_UPDATED_ERROR').then(function(text) {
			$scope.userPasswordErrorText = text;
		});

		UsersService.getUser(AuthService.currentUsername()).then(function(user) {
			$scope.user = user;
		});
		
		$scope.updatePassword = function() {
			if ($scope.edit_password_form.$valid) {			
				UsersService.updatePassword($scope.user).then(function(resp){
					$alert({content: $scope.userPasswordOKText, type: 'success'});
					return $state.go('userconfigurator.index');
				}, function(error) {
					$alert({content: $scope.userPasswordErrorText, type: 'warning'});
				});
			} else {
				$scope.edit_password_form.submitted = true;
			}
		};

	};

	UserEditController.$inject = injectParams;

	angular.module('openmuc.users').controller('UserEditController', UserEditController);
	
})();