(function(){

	var injectParams = ['$http', 'SETTINGS'];

	var AvailableAppsService = function($http, SETTINGS) {
    	this.getAll = function() {
    		return $http.get(SETTINGS.APPS_URL).then(function(response){
				return response.data;
			});
    	}
    };

    AvailableAppsService.$inject = injectParams;

	angular.module('openmuc.common').service('AvailableAppsService', AvailableAppsService);

})();
