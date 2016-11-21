(function(){

	var injectParams = ['$http', '$interval', 'SETTINGS', 'ChannelDataService', 'RestServerAuthService'];
	
	var ChannelsService = function($http, $interval, SETTINGS, ChannelDataService, RestServerAuthService) {

		this.getAllChannels = function() {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL,
    			headers: {
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		};
        		
    		return $http(req).then(function(response){			
				return response.data;
			});
		};

		this.getAllChannelsIds = function() {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL,
    			headers: {
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		};
        		
    		return $http(req).then(function(response){			
				var channelsIds = [];

				// add basic data
    			$.each(response.data, function(i, value) {
    				$.each(value, function(j, channel) {
    					channelsIds.push(channel.id);
    				});
    			});
    			
    			return channelsIds;
			});
		};

		this.valuesDisplayPrecision = function(numeric_value, precision){
			//nasty way of default argument in js...
			if(typeof(precision)==='undefined') precision = 0.001;
			
			if(numeric_value % 1. != 0.){
				return Math.floor(numeric_value / precision) * precision;
			}else{
				return numeric_value;
			}
		};
		
		this.getChannels = function(device) {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.DEVICES_URL + device.id + '/' + SETTINGS.CHANNELS_URL,
    			headers: {
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		};
    		
    		return $http(req).then(function(response){
				var channels = [];

				// add basic data
    			$.each(response.data.channels, function(index, value) {
    				channels.push({id: value});
    			});

    			// add additional data
    			$.each(channels, function(index, channel) {
    				ChannelDataService.getChannelData(channel).then(function(d){
    					channel['data'] = d;
    				});

    				ChannelDataService.getChannelDataValues(channel).then(function(d){
    					channel['records'] = d;
    				});
    			});
    			
    			return channels;
			});			
    	};
				
		this.getHistoryValues = function(channelId, from, until) {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channelId + SETTINGS.CHANNELS_HISTORY_URL + '?from=' + from + '&until=' + until,
    			headers: {
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		};
    		
    		var self = this;
    		
    		return $http(req).then(function(response){
    			var values = [];
    			
    			//regular expression matching entry of TimeSeriesString
    			var timeSeriesStringRegExp = /(\d{13}),(-*\d*\.\d*);/;
    			var isTimeSeriesStringChannel = false;
    			
    			
    			$.each(response.data.records, function(index, value) {
    				if ($.isNumeric(value.value)) {
    				
    					//if content of value is numeric, append (timestamp, value) to array
    					values.push({x: value.timestamp, y: self.valuesDisplayPrecision(value.value, 0.001)});
    			
    				}else if(typeof(value.value) == "string"){
    					
    					//if content is string, check if it matches format of TimeSeriesString
    					var match = value.value.match(timeSeriesStringRegExp);
    					if(match !== null){
    						isTimeSeriesStringChannel = true;
    					}
    					//break the loop, i.e. only detect channel property, extend by check for flags etc.
    					return false;
    				}
    			});
    			
    			if(isTimeSeriesStringChannel){
    				//get current time
    				var now = new Date();
    				var latestTimestamp = until;
    				if(until >= now + 60 * 60000){
    					latestTimestamp = until + 8 * 60 * 60000;
    				}

					
    				$.each(response.data.records.reverse(), function(index, value){
    					var reverse_entry_list = value.value.split(";").reverse();
    					$.each(reverse_entry_list, function(jndex, entry){
    						//console.log(entry);
    						if(entry!=""){
    							var stringPair = entry.split(",");
    							var timestamp = parseInt(stringPair[0]);
    							if(timestamp<latestTimestamp && timestamp>from){
    								var valAtTime = parseFloat(stringPair[1]);	
    								values.push({x: timestamp, y: self.valuesDisplayPrecision(valAtTime, 0.001)});
    								//console.log(timestamp);
    								//console.log(valAtTime);
    								latestTimestamp = timestamp;
    							}
    						}
    					});
    				});
    				values.reverse();
    			}
    			
    			return values;
			});
		};

		this.getValuesForExport = function(channelId, from, until) {
    		var req = {
    			method: 'GET',
    			url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channelId + SETTINGS.CHANNELS_HISTORY_URL + '?from=' + from + '&until=' + until,
    			headers: {
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		};
    		
    		return $http(req).then(function(response){			
    			var values = [];
    			var timestamps = [];
    			
    			$.each(response.data.records, function(index, value) {
    				timestamps.push(value.timestamp);
    				values.push(value.value);
    			});
    			
    			return [timestamps, values, channelId];
			});
		};

    	this.getChannel = function(channelId) {
    		var channel = [];
    		channel['id'] = channelId;
    		channel['configs'] = [];
    		
			ChannelDataService.getChannelData(channel).then(function(d){
				channel['configs'] = d;
			});
			
			return channel;
    	};

    	this.getChannelCurrentValue = function(channelId) {
    		var channel = [];
    		channel['id'] = channelId;
    		
			return ChannelDataService.getChannelDataValues(channel).then(function(d){
				return d;
			});
    	};
    	
    	this.writeValue = function(id, type, newValue) {

    		if (type=="STRING") {
    				var dataType = {record: {value: newValue}};
    		}
    		else if (type=="BYTE_ARRAY") {
    			// TODO: find a better solution to parse arrays.
				var dataType = {record: {value: newValue}};
		    }
    		else if (type=="INTEGER" || type=="LONG" || type=="SHORT" || type=="BYTE") {
    				var dataType = {record: {value: parseInt(newValue)}};
    		}
    		else if (type=="BOOLEAN") {
    			if (parseFloat(newValue)==1 || newValue=='true') {
    				newValue = true
    			}
    			else if (parseFloat(newValue) == 0 || newValue=='false') {
    				newValue = false
    			}
    			else {
    				// throw error
    			}
    			var dataType = {record: {value: newValue}};
    		}
    		else {
    				var dataType = {record: {value: parseFloat(newValue)}};
    		}

    		var req = {
        			method: 'PUT',
        			url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + id,
        			dataType: 'json',
        			data: dataType,
        			headers: {
        				'Content-Type': 'application/json', 
        				'Authorization': RestServerAuthService.getAuthHash(),
        			},
        		}
        		return $http(req).then(function(response){
    				return response.data;
    			});    		
    	};
    	
    	this.destroy = function(id) {
    		var req = {
    			method: 'DELETE',
    			url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + id,
    			dataType: 'json',
    			data: '',
    			headers: {
    				'Content-Type': 'application/json', 
    				'Authorization': RestServerAuthService.getAuthHash(),
    			},
    		}
    		return $http(req).then(function(response){
				return response.data;
			});
    	};
    	
    	this.update = function(channel) {
    		var req = {
        		method: 'PUT',
        		url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.id + SETTINGS.CONFIGS_URL,
        		dataType: 'json',
        		data: {configs: channel.configs},
        		headers: {
        			'Content-Type': 'application/json', 
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
        	};

    		return $http(req).then(function(response){
				return response.data;
			});
    	};
    	
    	this.create = function(channel) {
    		var req = {
    			method: 'POST',
        		url: SETTINGS.API_URL + SETTINGS.CHANNELS_URL + channel.configs.id,
        		dataType: 'json',
        		data: channel,
        		headers: {
        			'Content-Type': 'application/json', 
        			'Authorization': RestServerAuthService.getAuthHash(),
        		},
        	}
        	return $http(req).then(function(response){
    			return response.data;
    		});
    	};    	
    	
    };

	ChannelsService.$inject = injectParams;

	angular.module('openmuc.channels').service('ChannelsService', ChannelsService);

})();
