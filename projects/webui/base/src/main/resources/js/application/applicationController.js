(function(){

	var injectParams = ['$scope', '$state', '$cookieStore', '$alert', '$translate', 'AuthService'];
	
	var ApplicationController = function($scope, $state, $cookieStore, $alert, $translate, AuthService) {

		$translate('SUCCESSFULLY_LOGGED_OUT').then(function(text) {
			$scope.loggedOutText = text;
		});
		
		$scope.isLoggedIn = function() {
			return AuthService.isLoggedIn();
		}
		
		$scope.currentUsername = function() {
			return AuthService.currentUsername();
		}
		
		$scope.logout = function() {
			AuthService.logout();
			
			$alert({content: $scope.loggedOutText, type: 'success'});
			$state.go('home');
		};

		$scope.changeLanguage = function (key) {
		    $translate.use(key);
		};
		  
		$scope.currentLanguageIsEnglish = function() {
			return $translate.use() == 'en';
		};

		$scope.currentLanguageIsGerman = function() {
			return $translate.use() == 'de';
		};
				
	};

	ApplicationController.$inject = injectParams;

	angular.module('openmuc.common').controller('ApplicationController', ApplicationController);
	
})();