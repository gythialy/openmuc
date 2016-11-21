(function(){

	var injectParams = ['$http', 'DriverDataService', 'RestServerAuthService', 'SETTINGS'];
	
	var DriversService = function($http, DriverDataService, RestServerAuthService, SETTINGS) {

		this.getDrivers = function() {
    		var req = {
    			method: 'GET',
        		url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL,
        		headers: {
        			'Authorization': RestServerAuthService.getAuthHash(), 
        		},
            };
        	
			return $http(req).then(function(response){    			
    			var drivers = [];
    			
    			// add basic data
    			$.each(response.data['drivers'], function(index, value) {
    				drivers.push({id: value});
    			});
    			
    			// add additional data
    			$.each(drivers, function(index, driver) {
    				DriverDataService.getDriverData(driver).then(function(d){
    					drivers[index]['data'] = d;

    					DriverDataService.getDriverConfigs(driver).then(function(c){
    						drivers[index]['configs'] = c;
        				});
    				});
    			});
    			
    			return drivers;
    		});
    	};
		
    	this.getDriver = function(id) {
    		var driver = [];
    		driver['id'] = id;
    		driver['configs'] = [];
			DriverDataService.getDriverConfigs(driver).then(function(d){
				driver['configs'] = d;
			});
			
			return driver;
    	};

		this.getInfos = function(id) {
			var req = {
				method: 'GET',
				url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + id + SETTINGS.INFOS_URL,
				headers: {
					'Authorization': RestServerAuthService.getAuthHash(),
				},
			};

			return $http(req).then(function(response){
				return response.data.infos;
			});
		};

    	this.scan = function(driver, settings) {
    		var url = SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + '/'+ SETTINGS.SCAN_URL;

    		if (settings) {
    			var url = url + '?settings='+settings;
    		}

    		var req = {
    			method: 'GET',
        		url: url,
        		headers: {
        			'Authorization': RestServerAuthService.getAuthHash(), 
        		},
            };

    		return $http(req).then(function(response){
				return response.data;
			});
    	};

		this.scanInterrupt = function(driver) {
			var req = {
				method: 'PUT',
				url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + '/'+ SETTINGS.SCAN_INTERRUPT_URL,
				headers: {
					'Authorization': RestServerAuthService.getAuthHash(),
				},
			};

			return $http(req).then(function(response){
				return response.data;
			});
		};

		this.scanProgressInfo = function(driver) {
			var req = {
				method: 'GET',
				url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + '/'+ SETTINGS.SCAN_PROGRESS_INFO_URL,
				headers: {
					'Authorization': RestServerAuthService.getAuthHash(),
				},
			};

			return $http(req).then(function(response){
				return response.data;
			});
		};
    	
    	this.destroy = function(id) {
    		var req = {
    			method: 'DELETE',
    			url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + id,
    			dataType: 'json',
    			data: '',
    			headers: {
    				'Content-Type': 'application/json',
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		};
    		return $http(req).then(function(response){
				return response.data;
			});
    	};

    	this.update = function(driver) {
    		var req = {
        		method: 'PUT',
        		url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id + SETTINGS.CONFIGS_URL,
        		dataType: 'json',
        		data: {configs: driver.configs},
        		headers: {
        			'Content-Type': 'application/json', 
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
        	};

    		return $http(req).then(function(response){
				return response.data;
			});
    	};

    	this.create = function(driver) {
    		var req = {
    			method: 'POST',
        		url: SETTINGS.API_URL + SETTINGS.DRIVERS_URL + driver.id,
        		dataType: 'json',
        		data: driver,
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

    DriversService.$inject = injectParams;

	angular.module('openmuc.drivers').service('DriversService', DriversService);

})();
