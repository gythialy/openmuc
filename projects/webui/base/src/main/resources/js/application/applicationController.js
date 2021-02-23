(function(){

	var injectParams = ['$scope', '$state', '$cookies', '$translate', 'notify', 'AuthService'];

	var ApplicationController = function($scope, $state, $cookies, $translate, notify, AuthService) {

		$translate('SUCCESSFULLY_LOGGED_OUT').then(function(text) {
			$scope.loggedOutText = text;
		});

		$scope.isLoggedIn = function() {
			return AuthService.isLoggedIn();
		};

		$scope.logout = function() {
			AuthService.logout();

			notify({message: $scope.loggedOutText, position: "right", classes: "alert-success"});
			$state.go('home');
		};

		$scope.changeLanguage = function (key) {
		    $translate.use(key);
		};

		$scope.currentLanguageIsEnglish = function() {
			return $translate.use() === 'en';
		};

		$scope.currentLanguageIsGerman = function() {
			return $translate.use() === 'de';
		};

	};

	ApplicationController.$inject = injectParams;

	angular.module('openmuc.common').controller('ApplicationController', ApplicationController);

})();
