(function(){
	
	var injectParams = ['$scope', '$state', '$alert', '$translate', 'UsersService'];
	
	var UserNewController = function($scope, $state, $alert, $translate, UsersService) {

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
					$alert({content: $scope.userOKText, type: 'success'});
					return $state.go('userconfigurator.index');
				}, function(error) {
					$alert({content: $scope.userErrorText, type: 'warning'});
				});
			} else {
				$scope.userForm.submitted = true;
			}
		};
		
	};

	UserNewController.$inject = injectParams;

	angular.module('openmuc.users').controller('UserNewController', UserNewController);
	
})();