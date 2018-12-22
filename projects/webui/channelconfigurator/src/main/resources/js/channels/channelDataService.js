(function(){

	var injectParams = ['$http', 'SETTINGS', 'RestServerAuthService'];
	
	var ChannelDataService = function($http, SETTINGS, RestServerAuthService) {
    	this.getChannelData = function(channel) {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.id + SETTINGS.CONFIGS_URL,
    			headers: {
    				'Content-Type': 'application/json', 
    				'Authorization': RestServerAuthService.getAuthHash()
    			}
    		};
    		
    		return $http(req).then(function(response){    		
				return response.data.configs;
			});
    	};

    	this.getChannelDataValues = function(channel) {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.id,
    			headers: {
    				'Content-Type': 'application/json', 
    				'Authorization': RestServerAuthService.getAuthHash()
    			}
    		};
    		
    		return $http(req).then(function(response){
				return response.data.record;
			});
    	};
    };

    ChannelDataService.$inject = injectParams;

	angular.module('openmuc.channels').service('ChannelDataService', ChannelDataService);    

})();
