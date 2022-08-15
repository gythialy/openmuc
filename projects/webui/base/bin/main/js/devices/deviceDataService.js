(function(){

	var injectParams = ['$http', 'SETTINGS', 'RestServerAuthService'];

	var DeviceDataService = function($http, SETTINGS, RestServerAuthService) {
    	this.getDeviceData = function(device) {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + device.id,
    			headers: {
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		}
    		return $http(req).then(function(response){
    			return response.data;
    		});
    	};

		this.getDeviceConfigs = function(device) {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + device.id + SETTINGS.CONFIGS_URL,
    			headers: {
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		}
    		return $http(req).then(function(response){
    			return response.data.configs;
    		});
    	};
    };

    DeviceDataService.$inject = injectParams;

	angular.module('openmuc.devices').service('DeviceDataService', DeviceDataService);

})();
