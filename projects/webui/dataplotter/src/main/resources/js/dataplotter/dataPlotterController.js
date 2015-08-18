(function(){

	var injectParams = ['$scope', '$stateParams', '$state', '$q', '$translate', 'ChannelsService'];
	
	var DataPlotterController = function($scope, $stateParams, $state, $q, $translate, ChannelsService) {
		
		$scope.data = [];
		
		if ($stateParams.name) {
			$scope.dataPlotter = $scope.getPlotter($stateParams.name);
		}
				
		if ($scope.dataPlotter && $scope.dataPlotter.startDate) {
			$scope.startDate = new Date(parseInt($scope.dataPlotter.startDate));
		} else {
			//default start time of plot interval 16 hours in the past (rounded to full hrs)
			var now = new Date();
			$scope.startDate = new Date(now.setHours(now.getHours()-16, 0, 0, 0));
		}

		if ($scope.dataPlotter && $scope.dataPlotter.endDate) {
			$scope.endDate = new Date(parseInt($scope.dataPlotter.endDate));
		} else {
			//default final time of plot interval next full hour in the future
			var now = new Date();
			$scope.endDate = new Date(now.setHours(now.getHours()+1, 0, 0, 0));

		}
		

		
		if ($scope.dataPlotter && $scope.dataPlotter.yAxisLabel) {
			$scope.yLabel = $scope.dataPlotter.yAxisLabel;
		} else {
			$translate('VALUES').then(function(text) {
				$scope.yLabel = text;
			});
		}

		if ($scope.dataPlotter && $scope.dataPlotter.xAxisLabel) {
			$scope.xLabel = $scope.dataPlotter.xAxisLabel;
		} else {
			$translate('TIME').then(function(text) {
				$scope.xLabel = text;
			});
		}
				
		$scope.channels = [];
		$scope.selectedChannels = [];
		
		ChannelsService.getAllChannelsIds().then(function(channels){
						
			var allConfigChannelsDefined = false;
			
			if($scope.dataPlotter && $scope.dataPlotter.channels){
				
				allConfigChannelsDefined = true;
				$.each($scope.dataPlotter.channels, function(index, channel){
					if($.inArray(channel.id, channels) === -1){
						console.log("ERROR : No channel with id '" + channel.id + "'");
						allConfigChannelsDefined = false;
					}
					//console.log(channel.id + ", " + channel.label + ", " + allConfigChannelsDefined);
				});
			}
			
			if(allConfigChannelsDefined){
				//console.log("All Channels defined")
				$scope.channels = $scope.dataPlotter.channels;
			}else{
				var dummyChannels = [];
				$.each(channels, function(index, channel){
					dummyChannels.push({id : channel, label : channel, preselect : false});
				});
				$scope.channels = dummyChannels;
			}
	
			//console.log($scope.channels);
				
			$.each($scope.channels, function(index, channel){
				//console.log(channel.preselect);
				if(channel.preselect === "true"){
					$scope.selectedChannels.push(channel);
				}
			});
	
		});
		
		$scope.plotData = function () {

			var requests = [];
			$.each($scope.selectedChannels, function(i, channel) {
				requests.push(ChannelsService.getHistoryValues(channel.id, $scope.startDate.getTime(), $scope.endDate.getTime()).then(function(response){
					return {
				      	         key:    channel.label,
				      	         values: response,
				      	         color : channel.color
					};
				}));
			});
			
			$q.all(requests).then(function(data){
				$scope.data = data;
			});
		};
		
		

        $scope.xFunction = function(){
            return function(d){
                return d.x;
            };
        };
        
        $scope.yFunction = function(){
            return function(d){
                return d.y;
            };
        };

        $scope.xAxisLabel = function(){
        	return $scope.xLabel;
        };

        $scope.yAxisLabel = function(){
        	return $scope.yLabel;
        };
        /*
        var insertLinebreaks = function (d) {
        	var el = d3.select(this).text();
        	var words = d.split(' ');
        	el.text('');
	
		    for (var i = 0; i < words.length; i++) {
		        var tspan = el.append('tspan').text(words[i]);
		        if (i > 0)
		            tspan.attr('x', 0).attr('dy', '15');
		    }
        };*/
        var xRangeHrs = function(){
        	var ret = $scope.endDate - $scope.startDate; 
        	return ret / (60 * 60 * 1000);
        };
        
        var fullHrsInRaster = function(widthHrs){
        	var hourOfDayStart = $scope.startDate.getHours();
        	var div = Math.floor(hourOfDayStart/widthHrs);
        	var initialXTick = new Date($scope.startDate);
        	initialXTick.setHours(widthHrs * (div+1));
        	initialXTick.setMinutes(0);
        	initialXTick.setSeconds(0);
        	initialXTick.setMilliseconds(0);
        	var ret = [];
        	
        	while(initialXTick <= $scope.endDate){
        		ret.push(initialXTick);
        		initialXTick = new Date(initialXTick + widthHrs);
        	}
        	console.log(ret);
        	return ret;
        };
        
        $scope.xAxisTicks = function(){
        	
        	var xRange = xRangeHrs();
        	if(xRange <= 1){
        		return []; 
        	}else if(xRange <= 3){
        		return fullHrsInRaster(1);
        	}else if(xRange <= 24){
        		return fullHrsInRaster(3);
        	}else if(xRange <= 48){
        		return fullHrsInRaster(12);
        	}else{
        		return fullHrsInRaster(24);
        	};
        };
        
		$scope.xAxisTickFormat = function () {
	        
			return function (d) {
				return d3.time.format('%m.%d. %H:%M')(new Date(d));
			};
			//var xRange = xRangeHrs();
	        //if(xRange <= 48){
	        //	return function (d) {
	        //		return d3.time.format('%X')(new Date(d));
	        //	};
	        //}else{
	        //	return function (d) {
	        //		return d3.time.format('%x')(new Date(d));
	        //	};
	        //};
	        
	     };
	
	    $scope.yAxisTickFormat = function () {
	    	return function (d) {
	    		return d;
	    	};
	    };
	    
	    $scope.disabledPlot = function () {
	    	return $scope.selectedChannels.length == 0;// || $scope.selectedChannels.length > 3;
	    };
	    
	    $scope.plotRange = function() {
	    	if($scope.dataPlotter && $scope.dataPlotter.plotRange){
	    		return $scope.dataPlotter.plotRange;
	    	}else{
	    		return 0;
	    	}
	    };
	    
	};

	DataPlotterController.$inject = injectParams;

	angular.module('openmuc.dataplotter').controller('DataPlotterController', DataPlotterController);
	
})();
