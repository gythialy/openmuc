(function(){

	var injectParams = ['$scope', '$state', '$alert', '$translate', 'AuthService'];
	
	var LoginController = function($scope, $state, $alert, $translate, AuthService) {

		$translate('LOGIN_CREDENTIALS_INCORRECT').then(function(text) {
			$scope.loginCredentialsErrorErrorText = text;
		});
		
		if (AuthService.isLoggedIn()) {
			$state.go('dashboard');
		} else { 
			$scope.user = {};
					
			$scope.login = function() {				
				AuthService.login($scope.user).then(function(response){
					AuthService.setCurrentUser({user: $scope.user.user});
					$state.go('dashboard');
				}, function(error) {
					$alert({content: $scope.loginCredentialsErrorErrorText, type: 'warning'});
					$state.go('home');
				});								
			}
		}
	};

	LoginController.$inject = injectParams;

	angular.module('openmuc.sessions').controller('LoginController', LoginController);
	
})();