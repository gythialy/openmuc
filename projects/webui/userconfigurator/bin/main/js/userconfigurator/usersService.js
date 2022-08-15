(function(){

	var injectParams = ['$http', 'SETTINGS', 'RestServerAuthService'];

	var UsersService = function($http, SETTINGS, RestServerAuthService) {

		this.getUsers = function() {
    		var req = {
    			method: 'GET',
        		url: SETTINGS.API_URL + SETTINGS.USERS_URL,
        		headers: {
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
            };

			return $http(req).then(function(response) {
    			return response.data['users'];
    		});
    	};

    	this.getUser = function(id) {
    		var req = {
    			method: 'GET',
        		url: SETTINGS.API_URL + SETTINGS.USERS_URL + id,
        		headers: {
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
            };

			return $http(req).then(function(response) {
    			return response.data;
    		});
    	};

    	this.create = function(user) {
    		var req = {
    			method: 'POST',
        		url: SETTINGS.API_URL + SETTINGS.USERS_URL,
        		dataType: 'json',
        		data: user,
        		headers: {
        			'Content-Type': 'application/json',
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
        	}
        	return $http(req).then(function(response){
    			return response.data;
    		});
    	};

    	this.destroy = function(data) {
    		var req = {
    			method: 'DELETE',
    			url: SETTINGS.API_URL + SETTINGS.USERS_URL,
    			dataType: 'json',
    			data: data,
    			headers: {
    				'Content-Type': 'application/json',
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		}
    		return $http(req).then(function(response){
				return response.data;
			});
    	};

    	this.updatePassword = function(user) {
    		var req = {
        		method: 'PUT',
        		url: SETTINGS.API_URL + SETTINGS.USERS_URL,
        		dataType: 'json',
        		data: user,
        		headers: {
        			'Content-Type': 'application/json',
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
        	};

    		return $http(req).then(function(response){
				return response.data;
			});
    	};

	};

    UsersService.$inject = injectParams;

	angular.module('openmuc.users').service('UsersService', UsersService);

})();
