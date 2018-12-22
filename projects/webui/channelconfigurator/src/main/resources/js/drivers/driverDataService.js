(function(){

	var injectParams = ['$http', 'SETTINGS', 'RestServerAuthService'];

	var DriverDataService = function($http, SETTINGS, RestServerAuthService) {
    	this.getDriverData = function(driver) {
    		var req = {
    			method: 'GET',
        		url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id,
        		headers: {
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
            };

			return $http(req).then(function(response){
				return response.data;
			});
    	};

    	this.getDriverConfigs = function(driver) {
    		var req = {
    			method: 'GET',
        		url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + SETTINGS.CONFIGS_URL,
        		headers: {
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
            };

			return $http(req).then(function(response){
				return response.data.configs;
			});
    	};

	};

    DriverDataService.$inject = injectParams;

	angular.module('openmuc.drivers').service('DriverDataService', DriverDataService);

})();
