(function(){

	var injectParams = [];
	
	var RestServerAuthService = function() {
    	this.getAuthHash = function() {
    		return 'Basic ' + btoa("admin:admin");
    	};
    };

    RestServerAuthService.$inject = injectParams;

	angular.module('openmuc.auth').service('RestServerAuthService', RestServerAuthService);    

})();
