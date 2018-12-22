(function(){

    var injectParams = ['$scope', '$q', '$interval', '$translate', '$stateParams', 'ChannelsService'];

    var LivePlotterController = function($scope, $q, $interval, $translate, $stateParams, ChannelsService) {

        $scope.data = [];
        $scope.channels = [];
        $scope.selectedChannels = [];

        if ($stateParams.name) {
            $scope.livePlotter = $scope.getPlotter($stateParams.name);
        }

        if ($scope.livePlotter && $scope.livePlotter.refresh) {
            $scope.refresh = $scope.livePlotter.refresh;
        } else {
            $scope.refresh = 500;
        }

        if ($scope.livePlotter && $scope.livePlotter.timePeriod) {
            $scope.timePeriod = $scope.livePlotter.timePeriod;
        } else {
            $scope.timePeriod = 7;
        }

        if ($scope.livePlotter && $scope.livePlotter.timePeriodUnit) {
            $scope.timePeriodUnit = $scope.livePlotter.timePeriodUnit;
        } else {
            $scope.timePeriodUnit = "seconds";
        }

        if ($scope.livePlotter && $scope.livePlotter.yAxisLabel) {
            $scope.yLabel = $scope.livePlotter.yAxisLabel;
        } else {
            $translate('VALUES').then((text) => $scope.yLabel = text);
        }

        if ($scope.livePlotter && $scope.livePlotter.xAxisLabel) {
            $scope.xLabel = $scope.livePlotter.xAxisLabel;
        } else {
            $translate('TIME').then((text) => $scope.xLabel = text);
        }

        $translate('NOW').then((text) =>$scope.nowText = text);
        $translate('SECONDS').then((text) => $scope.secondsText = text);
        $translate('MINUTES').then((text) => $scope.minutesText = text);
        $translate('HOURS').then((text) => $scope.hoursText = text);

        ChannelsService.getAllChannelsIds().then(function(channels){

            var allConfigChannelsDefined = false;

            if($scope.livePlotter && $scope.livePlotter.channels){

                allConfigChannelsDefined = true;
                $.each($scope.livePlotter.channels, function(index, channel){
                    if($.inArray(channel.id, channels) === -1){
                        console.log("ERROR : No channel with id '" + channel.id + "'");
                        allConfigChannelsDefined = false;
                    }
                    //console.log(channel.id + ", " + channel.label + ", " + allConfigChannelsDefined);
                });
            }

            if (allConfigChannelsDefined){
                //console.log("All Channels defined")
                $scope.channels = $scope.livePlotter.channels;
            }else{
                $scope.channels = channels.map((channel) => {
                    return {id : channel, label : channel, preselect : false};
                });
            }

            //console.log($scope.channels);

            $scope.channels.forEach((channel) => {
                //console.log(channel.preselect);
                if(channel.preselect === "true"){
                    $scope.selectedChannels.push(channel);
                }
            });

        });

        $scope.xFunction = function(){
            return function(d){
                return d.x;
            };
        };

        $scope.yFunction = function(){
            return function(d){
                return d.y;
            }
        };

        $scope.xAxisLabel = function(){
            return $scope.xLabel;
        };

        $scope.yAxisLabel = function(){
            return $scope.yLabel;
        };

        $scope.xAxisTickFormat = function () {
            return function (d) {
                if (d == 0) {
                    return $scope.nowText;
                }
                if ($scope.timePeriodUnit == "seconds") {
                    if (d % 1 === 0) {
                        var seconds = d;
                    } else {
                        var seconds = d.toFixed(2);
                    }
                    return seconds + " " + $scope.secondsText;
                } else if ($scope.timePeriodUnit == "minutes") {
                    var minutes = parseInt(Math.abs(d)/60);
                    var seconds = Math.abs(d) % 60;

                    if (d % 1 !== 0) {
                        seconds = seconds.toFixed(2);
                    }

                    if (seconds.toString().length == 1) {
                        seconds = "0" + seconds;
                    }
                    if (minutes.toString().length == 1) {
                        minutes = "0" + minutes;
                    }
                    return "-" + minutes + ":" + seconds + " " + $scope.minutesText;
                } else {
                    var minutes = parseInt(Math.abs(d)/60);
                    var seconds = Math.abs(d) % 60;
                    var hours = parseInt(minutes/60);
                    minutes = minutes % 60;
                    if (seconds.toString().length == 1) {
                        seconds = "0" + seconds;
                    }
                    if (minutes.toString().length == 1) {
                        minutes = "0" + minutes;
                    }
                    if (hours.toString().length == 1) {
                        hours = "0" + hours;
                    }

                    if (d % 1 !== 0) {
                        seconds = seconds.toFixed(2);
                    }

                    return "-" + hours + ":" + minutes + ":" + seconds + " " + $scope.hoursText;
                }
            }
        };

        $scope.yAxisTickFormat = function () {
            return (d) => d;
        };

        $scope.plotRange = function() {
            if($scope.livePlotter && $scope.livePlotter.plotRange){
                return $scope.livePlotter.plotRange;
            }else{
                return 0;
            }
        };

        $scope.interval = "";

        $scope.disabledPlot = function () {
            return $scope.selectedChannels.length == 0; // || $scope.selectedChannels.length > 3;
        };

        $scope.plotData = function () {
            // get max values to display in seconds
            if ($scope.timePeriodUnit == "seconds") {
                $scope.maxValuesToDisplay = 1000/$scope.refresh * $scope.timePeriod;
            } else if ($scope.timePeriodUnit == "minutes") {
                $scope.maxValuesToDisplay = 1000/$scope.refresh * $scope.timePeriod * 60;
            } else {
                $scope.maxValuesToDisplay = 1000/$scope.refresh * $scope.timePeriod * 60 * 60;
            }

            $scope.data = $scope.selectedChannels.map((channel) => {
                return {
                    key:    channel.label,
                    values: [],
                    color:  channel.color,
                    type:   channel.type};
            });


            $interval.cancel($scope.interval);

            $scope.interval = $interval(() => {
                var requests = $scope.selectedChannels.map((channel) => {
                    return ChannelsService.getChannelCurrentValue(channel.id).then((response) => {
                        return {
                            key:   channel.label,
                            value: response.value,
                            color: channel.color,
                            type:  channel.type
                        };
                    });
                });

                $q.all(requests).then(function(data){

                    $.each(data, function(i, channelData) {

                        $scope.data[i].key = channelData.key;

                        if (typeof(channelData.value) == "number") {


                            var y = ChannelsService.valuesDisplayPrecision(channelData.value, 0.001);

                            // rotate x values to the left
                            $.each($scope.data[i].values, function(j, newData) {
                                $scope.data[i].values[j].x = $scope.data[i].values[j].x - ($scope.refresh/1000);
                            });

                            // complete the values with null values
                            for (j = ($scope.maxValuesToDisplay-$scope.data[i].values.length); j > 0; j--) {
                                $scope.data[i].values.push({x: -(j * ($scope.refresh/1000)), y: ''});
                            }

                            // remove last value
                            if ($scope.data[i].values.length > $scope.maxValuesToDisplay) {
                                $scope.data[i].values.shift();
                            }

                            // push new value
                            $scope.data[i].values.push({x: 0, y: y});

                        }
                    });

                });
            }, $scope.refresh);

        };

        $scope.$on('$destroy', function () {
            $interval.cancel($scope.interval);
        });

    };



    LivePlotterController.$inject = injectParams;

    angular.module('openmuc.dataplotter').controller('LivePlotterController', LivePlotterController);

})();