(function(){

	var injectParams = ['$scope', '$q', 'ChannelsService'];
	
	var DataExporterController = function($scope, $q, ChannelsService) {
		$scope.selectedChannels = [];
		
		$scope.channels = [];
		
		$scope.startDate = new Date();
		$scope.startDate.setHours(0, 0, 0, 0);
		$scope.endDate = new Date();
		$scope.timeFormat = 1;
		
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
                    switch($scope.timeFormat) {
                        case 1:
                            timestamps = $.unique($.merge(timestamps, channels[0]));
                            break;
                        case 2:
                            // TODO; Unix Timestamp
                        case 3:
                            // TODO: Human Readable Timestamp
                            // var a = new Date(TIMESTAMP);
                            // var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
                            // var year = a.getFullYear();
                            // var month = months[a.getMonth()];
                            // var date = a.getDate();
                            // var hour = a.getHours();
                            // var min = a.getMinutes() < 10 ? '0' + a.getMinutes() : a.getMinutes();
                            // var sec = a.getSeconds() < 10 ? '0' + a.getSeconds() : a.getSeconds();
                            // var time = year + ' ' + month + ' ' + date + ' ' + hour + ':' + min + ':' + sec ;
                            // timestamps = $.unique($.merge(timestamps, channels[0]));
                            break;
                        case 4:
                            // TODO: Human Readable Timestamp + Unix Timestamp
                            beak;
                        default:
                            timestamps = $.unique($.merge(timestamps, channels[0]));
                    }
					
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
			switch($scope.timeFormat) {
				case 1:
				case 2:
					var header = ["Timestamp"];
					break;
				case 3:
					var header = ["Date"];
					break;
				case 4:
					var header = ["Timestamp", "Date"];
					beak;
				default:
					var header = ["Timestamp"];
			}

			$.each($scope.selectedChannels, function(i, channel) {
				header.push(channel);
			});

			return header;
		}
	};

	DataExporterController.$inject = injectParams;

	angular.module('openmuc.dataexporter').controller('DataExporterController', DataExporterController);
	
})();