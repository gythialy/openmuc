(function(){
	
	var injectParams = ['$scope', '$alert', '$translate', 'UsersService'];
	
	var UsersController = function($scope, $alert, $translate, UsersService) {
		
		$translate('USER_DELETED_SUCCESSFULLY').then(function(text) {
			$scope.userOKText = text;
		});
		
		UsersService.getUsers().then(function(users) {
			$scope.users = users;
		});
		
		$scope.deleteUser = function(id) {
			var data = {configs: {id: id}};
			UsersService.destroy(data).then(function(data) {
				$alert({content: $scope.userOKText, type: 'success'});
			});
			
			UsersService.getUsers().then(function(users) {
				$scope.users = users;
			});
		};
		
	};

	UsersController.$inject = injectParams;

	angular.module('openmuc.users').controller('UsersController', UsersController);
	
})();