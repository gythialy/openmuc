(function(){

	var injectParams = ['AuthService'];

	var RestServerAuthService = function(AuthService) {

    	this.getAuthHash = function() {
			if (AuthService.isLoggedIn) {
				return 'Basic ' + btoa(AuthService.currentUsername() + ":" + AuthService.currentPwd());
			}
    	};
    };

    RestServerAuthService.$inject = injectParams;

	angular.module('openmuc.auth').service('RestServerAuthService', RestServerAuthService);    

})();
