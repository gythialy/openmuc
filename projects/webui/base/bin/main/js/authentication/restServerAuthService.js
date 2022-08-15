(function(){

	var injectParams = ['AuthService'];

	var RestServerAuthService = function(AuthService) {

    	this.getAuthHash = function() {
			if (AuthService.isLoggedIn) {
				return AuthService.getRestAuth();
			}
    	};
    };

    RestServerAuthService.$inject = injectParams;

	angular.module('openmuc.auth').service('RestServerAuthService', RestServerAuthService);

})();
