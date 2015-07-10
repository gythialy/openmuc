(function(){

	var injectParams = ['$scope', '$q', 'ChannelsService'];
	
	var DataExporterController = function($scope, $q, ChannelsService) {
		$scope.selectedChannels = [];
		
		$scope.channels = [];
		
		$scope.startDate = new Date();
		$scope.startDate.setHours(0, 0, 0, 0);
		$scope.endDate = new Date();
		
		$scope.disableExport = true;
		
		ChannelsService.getAllChannelsIds().then(function(channels){
			$scope.channels = channels;
		});

		$scope.exportData = function () {
			$scope.disableExport = true;
			var requests = [];

			$.each($scope.selectedChannels, function(i, channel) {
				requests.push(ChannelsService.getValuesForExport(channel, $scope.startDate.getTime(), $scope.endDate.getTime()).then(function(response){
					return response;
				}));
			});
			
			$q.all(requests).then(function(data){
				$scope.data = [];
				var timestamps = [];
				
				/* extract all timestamps */
				$.each(data, function(i, channels) {
					timestamps = $.unique($.merge(timestamps, channels[0]));
				});
				
				$.each(timestamps, function(i, timestamp) {
					var values = {timestamp: timestamp};
					
					$.each(data, function(i, channels) {
						index = $.inArray(timestamp, channels[0]);
						if (index >= 0) {
							values[channels[2]] = channels[1][index];
						}
					});
					
					$scope.data.push(values);
				});
				
				$scope.disableExport = false;
			});
		};
		
		$scope.getHeader = function() {
			var header = ["Timestamp"];

			$.each($scope.selectedChannels, function(i, channel) {
				header.push(channel);
			});

			return header;
		}
	};

	DataExporterController.$inject = injectParams;

	angular.module('openmuc.dataexporter').controller('DataExporterController', DataExporterController);
	
})();