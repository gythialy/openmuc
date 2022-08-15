(function () {

    var injectParams = ['$scope', '$q', 'ChannelsService', 'ChannelDataService'];

    var DataExporterController = function ($scope, $q, ChannelsService, ChannelDataService) {

        $scope.selectedChannels = [];

        $scope.channels = [];

        $scope.startDate = new Date();
        $scope.startDate.setHours(0, 0, 0, 0);
        $scope.endDate = new Date();
        $scope.timeFormat = 1;

        $scope.disableExport = true;

        $scope.mstep = 1;
        $scope.options = {mstep: [1, 5, 10, 15]} 

        ChannelsService.getAllChannelsIds().then(async function (channels) {

            var allConfigChannelsDefined = false;

            if ($scope.dataPlotter && $scope.dataPlotter.channels) {
                var cIndex = $scope.dataPlotter.channels.findIndex((channel) => channels.indexOf(channel.id) === -1);
                allConfigChannelsDefined = cIndex === -1;
            }

            if (allConfigChannelsDefined) {
                //console.log("All Channels defined")
                $scope.channels = $scope.dataPlotter.channels;
                $scope.selectedChannels = $scope.channels.filter((channel) => channel.preselect === 'true');
            } else {
                $scope.channels = channels.map((channel) => {
                    return {id: channel, label: channel, preselect: false};
                });

                for (let channel of $scope.channels){
                    var config = await ChannelDataService.channelHasHistoricValues(channel);
                    if (config === false) {}
                    else {
                        channel.historic = await config;
                    }
                }
                $scope.$apply();
            }
            //console.log($scope.channels);

        });

        $scope.exportData = function () {
            $scope.disableExport = true;
            var requests = [];

            $scope.selectedChannels.forEach(function (channel) {
                requests.push(ChannelsService.getValuesForExport(channel.id, $scope.startDate.getTime(), $scope.endDate.getTime()).then(r => r));
            });

            $q.all(requests).then(function (data) {
                $scope.data = [];
                var timestamps = [];

                /* extract all timestamps */
                data.forEach((channels) => {
                    switch ($scope.timeFormat) {
                        case 1:
                            // get all unique time stamps
                            // note: the three dos convert the set back to an array
                            readableTime = channels[0].map(function (element){
                                var javaTime = new Date(element);
                                var year = javaTime.getFullYear();
                                var month = ('0'+(javaTime.getMonth() + 1)).slice(-2);
                                var day = ('0'+javaTime.getDate()).slice(-2);
                                var hour = ('0'+javaTime.getHours()).slice(-2);
                                var min = ('0'+javaTime.getMinutes()).slice(-2);
                                var sec = ('0'+javaTime.getSeconds()).slice(-2);
                                var time = year + "" + month + day + '_' + hour + min + sec; 
                                element = time;
                                return element;
                            });
                            $scope.firstTimestamp = readableTime[0];
                            $scope.lastTimestamp = readableTime[readableTime.length-1];
                            timestamps = [...new Set(timestamps.concat(channels[0]))];
                            break;
                        case 2:
                            readableTime = channels[0].map(function (element){
                                var javaTime = new Date(element);
                                var year = javaTime.getFullYear();
                                var month = ('0'+(javaTime.getMonth() + 1)).slice(-2);
                                var day = ('0'+javaTime.getDate()).slice(-2);
                                var hour = ('0'+javaTime.getHours()).slice(-2);
                                var min = ('0'+javaTime.getMinutes()).slice(-2);
                                var sec = ('0'+javaTime.getSeconds()).slice(-2);
                                var time = year + month + day + '_'+ hour + min + sec;
                                element = time;
                                return element;
                            });
                            $scope.firstTimestamp = readableTime[0];
                            $scope.lastTimestamp = readableTime[readableTime.length-1];
                            unixTime = channels[0].map(function (element){
                                return element * 0.001;
                            });
                            timestamps = [...new Set(timestamps.concat(unixTime))];
                            break;
                        case 3:
                            readableTime = channels[0].map(function (element){
                                var javaTime = new Date(element);
                                var year = javaTime.getFullYear();
                                var month = ('0'+(javaTime.getMonth() + 1)).slice(-2);
                                var day = ('0'+javaTime.getDate()).slice(-2);
                                var hour = ('0'+javaTime.getHours()).slice(-2);
                                var min = ('0'+javaTime.getMinutes()).slice(-2);
                                var sec = ('0'+javaTime.getSeconds()).slice(-2);
                                var time = hour + ':' + min + ':' + sec;
                                var date = day + '.' +  month + '.' + year;
                                element = date + " " + time;
                                return element;
                            });
                            timestamps = [...new Set(timestamps.concat(readableTime))];
                            csvTime = channels[0].map(function (element){
                                var javaTime = new Date(element);
                                var year = javaTime.getFullYear();
                                var month = ('0'+(javaTime.getMonth() + 1)).slice(-2);
                                var day = ('0'+javaTime.getDate()).slice(-2);
                                var hour = ('0'+javaTime.getHours()).slice(-2);
                                var min = ('0'+javaTime.getMinutes()).slice(-2);
                                var sec = ('0'+javaTime.getSeconds()).slice(-2);
                                var time = year + "" + month + day + '_' + hour + min + sec; 
                                element = time;
                                return element;
                            });
                            $scope.firstTimestamp = csvTime[0];
                            $scope.lastTimestamp = csvTime[csvTime.length-1];
                            break;
                        case 4:
                            readableTime = channels[0].map(function (element){
                                var javaTime = new Date(element);
                                var year = javaTime.getFullYear();
                                var month = ('0'+(javaTime.getMonth() + 1)).slice(-2);
                                var day = ('0'+javaTime.getDate()).slice(-2);
                                var hour = ('0'+javaTime.getHours()).slice(-2);
                                var min = ('0'+javaTime.getMinutes()).slice(-2);
                                var sec = ('0'+javaTime.getSeconds()).slice(-2);
                                var time = hour + ':' + min + ':' + sec;
                                var date = day + '.' +  month + '.' + year;
                                element = date + " " + time + " " + element;
                                return element;
                            });
                            timestamps = [...new Set(timestamps.concat(readableTime))];
                            csvTime = channels[0].map(function (element){
                                var javaTime = new Date(element);
                                var year = javaTime.getFullYear();
                                var month = ('0'+(javaTime.getMonth() + 1)).slice(-2);
                                var day = ('0'+javaTime.getDate()).slice(-2);
                                var hour = ('0'+javaTime.getHours()).slice(-2);
                                var min = ('0'+javaTime.getMinutes()).slice(-2);
                                var sec = ('0'+javaTime.getSeconds()).slice(-2);
                                var time = year + "" + month + day + '_' + hour + min + sec; 
                                element = time;
                                return element;
                            });
                            $scope.firstTimestamp = csvTime[0];
                            $scope.lastTimestamp = csvTime[csvTime.length-1];
                            break;
                        default:
                            timestamps = [...new Set(timestamps.concat(channels[0]))];
                            break;
                    }

                });

                var index = 0;
                var headerName = ["date", "time", "timestamp"];
                timestamps.forEach((timestamp) => {
                    if (typeof timestamp === 'string'){
                        if (timestamp.indexOf(" ") >= 0){
                            var date = timestamp.split(" ");
                            var count = 0;
                            var values = {};
                            date.forEach(function (element){
                                values[headerName[count]] = element;
                                count++;
                            });
                        }
                        else{
                            var values = {timestamp: timestamp};
                        }
                    } 
                    else{
                        var values = {timestamp: timestamp};
                    }   
                    data.forEach((channels) => {
                        if (channels[1][index] == undefined) {
                            values[channels[2]] = " ";
                        }
                        else {
                            values[channels[2]] = channels[1][index];
                        }
                    });
                    ++index;
                    $scope.data.push(values);
                });
                $scope.disableExport = false;
            });
        };

        $scope.getHeader = function () {
            var header = [];
            switch ($scope.timeFormat) {
                case 1:
                    header.push('Timestamp');
                    break;
                case 2:
                    header.push('Timestamp');
                    break;
                case 3:
                    header.push('Date');
                    header.push('Time');
                    break;
                case 4:
                    header.push('Date');
                    header.push('Time');
                    header.push('Timestamp');
                    break;
                default:
                    header = ['Timestamp'];
                    break;
            }

            $scope.selectedChannels.forEach(channel => header.push(channel.id));

            return header;
        }

        $scope.disabledPlot = function () {
            if ($scope.selectedChannels.length === 0){
                return true;
            }    
            else {
                if($scope.startDate === null || $scope.endDate === null){
                    return true;
                }
                else {
                    return false
                }   
            }
        };
    };

    DataExporterController.$inject = injectParams;

    angular.module('openmuc.dataexporter').controller('DataExporterController', DataExporterController);

})();
