(function () {

    var injectParams = ['$scope', '$q', '$interval', '$translate', '$stateParams', 'ChannelsService', 'ChannelDataService'];

    var LivePlotterController = function ($scope, $q, $interval, $translate, $stateParams, ChannelsService, ChannelDataService) {

        $scope.channels = [];
        $scope.selectedChannels = [];
        $scope.plotting = false;
        $scope.paused = false;
        $scope.advanced = false;
        $scope.enableAutoRange = true;
        $scope.yMinValue = null;
        $scope.yMaxValue = null;

        var data = [];
        var nowText;
        var secondsText;
        var minutesText;
        var hoursText;
        var noData;
        var maxValuesToDisplay;

        if ($stateParams.name) {
            $scope.livePlotter = $scope.getPlotter($stateParams.name);
        }


        if ($scope.livePlotter && $scope.livePlotter.timePeriod) {
            $scope.timePeriod = $scope.livePlotter.timePeriod;
        } else {
            $scope.timePeriod = 60;
        }
        if ($scope.livePlotter && $scope.livePlotter.timePeriodUnit) {
            $scope.timePeriodUnit = $scope.livePlotter.timePeriodUnit;
        } else {
            $scope.timePeriodUnit = 'seconds';
        }

        var computeNewRefreshrate = function () {
            var timePeriod = $scope.timePeriod;
            var tu = $scope.timePeriodUnit;
            if (tu === 'seconds') {
                timePeriod *= 1e3;
            } else if (tu === 'minutes') {
                timePeriod *= 60 * 1e3;
            } else {
                timePeriod *= 60 * 60 * 1e3;
            }
            var r = Math.round(timePeriod / 60);
            if (r < 500) {
                r = 500;
            } else if (r > 10e3) {
                r = 10e3;
            }
            $scope.refresh = r;
        };


        if ($scope.livePlotter && $scope.livePlotter.refresh) {
            $scope.refresh = $scope.livePlotter.refresh;
        } else {
            computeNewRefreshrate();
        }

        $translate('NOW').then((text) => nowText = text);
        $translate('SECONDS').then((text) => secondsText = text);
        $translate('MINUTES').then((text) => minutesText = text);
        $translate('HOURS').then((text) => hoursText = text);
        $translate('NO_DATA_TO_DISPLAY').then(text => noData = text);

        ChannelsService.getAllChannels().then(async function(channels) {
            channels = channels.records;
            var allConfigChannelsDefined = false;

            if ($scope.livePlotter && $scope.livePlotter.channels) {

                allConfigChannelsDefined = true;
                $scope.livePlotter.channels.forEach(c => {
                    if (channels.find(ch => ch.id === c.id) === undefined) {
                        console.log('ERROR : No channel with id \'' + c.id + '\'');
                        allConfigChannelsDefined = false;
                    }
                    //console.log(channel.id + ", " + channel.label + ", " + allConfigChannelsDefined);
                });
            }

            if (allConfigChannelsDefined) {
                //console.log("All Channels defined")
                $scope.channels = $scope.livePlotter.channels;
            } else {
                $scope.channels = channels.map(channel => ({
                    id: channel.id,
                    label: channel.id,
                    preselect: false,
                    valueType: channel.valueType
                }));
            }

            //console.log($scope.channels);
            for (let channel of $scope.channels){
                //console.log(channel.preselect);
                if (channel.preselect === 'true') {
                    $scope.selectedChannels.push(channel);
                }
                var config = await ChannelDataService.getChannelConfig(channel, 'unit');
                if (config === true) {}
                else{
                    channel.unit = await config.unit;
                }
            }

        });

        var buildChart = function (data) {
            var chart = nv.models.lineChart()
                .margin({left: 50, right: 10})  //Adjust chart margins to give the x-axis some breathing room.
                .interactive(true)
                .useInteractiveGuideline(true)  //We want nice looking tooltips and a guideline!
                .showLegend(true)       //Show the legend, allowing users to turn on/off line series.
                .showYAxis(true)        //Show the y-axis
                .showXAxis(true)        //Show the x-axis
                .forceY(plotRange())
                .noData(noData)
                .width(450)
                .height(300)
                .defined((d, i) => typeof(d.y) === 'number' && !isNaN(d.y) && d.y !== null);

            chart.legend.maxKeyLength(100);

            if ($scope.yMinValue != null && $scope.yMaxValue != null){
                if ((isNaN(parseInt($scope.yMinValue)) === true || isNaN(parseInt($scope.yMaxValue))) || $scope.enableAutoRange == true){
                    console.log(parseInt($scope.yMinValue));
                    console.log(parseInt($scope.yMaxValue));
                }
                else {
                    chart.yDomain([$scope.yMinValue,$scope.yMaxValue]);
                }
            }

            chart.interactiveLayer.tooltip.contentGenerator(function (d) {
                var header = d.value;
                var headerhtml = "<thead><tr><td colspan='3'><strong class='x-value'>" + header + "</strong></td></tr></thead>";
                var bodyhtml = "<tbody>";
                var series = d.series;
                series.forEach((c) => {
                    var unit;
                    let channel = data.find(o => o.id === c.key);
                    if (channel.unit == undefined) {
                        unit = '';
                    } else {
                        unit = ' ' + channel.unit;
                    }
                    var valueUnit;
                    if (c.value == undefined) {
                        valueUnit = 'undefined';
                    } else {
                        valueUnit = c.value + unit;
                    }


                    bodyhtml = bodyhtml + '<tr><td class="legend-color-guide"><div style="background-color: ' + c.color + ';"></div></td><td class="key">' + c.key
                        + '</td><td class="value">' + valueUnit
                        + '</td></tr>';
                });
                bodyhtml = bodyhtml + '</tbody>';
                return "<table>" + headerhtml + '' + bodyhtml + "</table>";
            });

            chart.yAxis     //Chart y-axis settings
                .tickFormat(d3.format('.03f'));



            chart.xAxis     //Chart x-axis settings
                .tickFormat(xAxisTickFormat);

            /* Done setting the chart up? Time to render it!*/
            //     d3.select('#graph svg')    //Select the <svg> element you want to render the chart in.
            d3.select('#graph svg')
                .datum(data)         //Populate the <svg> element with chart data...
                .transition().duration(350)  //how fast do you want the lines to transition?
                .call(chart);          //Finally, render the chart!

            //Update the chart when window resizes.
            nv.utils.windowResize(() => chart.update());

            return chart;
        };

        nv.addGraph(function () {
            var data = [];
            buildChart(data);
        });

        var xAxisTickFormat = function (d) {
            var seconds;
            var minutes;

            if (d === 0) {
                return nowText;
            }
            if ($scope.timePeriodUnit === 'seconds') {
                if (d % 1 === 0) {
                    seconds = d;
                } else {
                    seconds = d.toFixed(2);
                }
                return seconds;
            } else if ($scope.timePeriodUnit === 'minutes') {
                minutes = parseInt(Math.abs(d) / 60);
                seconds = Math.abs(d) % 60;

                if (d % 1 !== 0) {
                    seconds = seconds.toFixed(2);
                }

                if (seconds.toString().length == 1) {
                    seconds = '0' + seconds;
                }
                if (minutes.toString().length === 1) {
                    minutes = '0' + minutes;
                }
                return "-" + minutes + ":" + seconds;
            } else {
                minutes = parseInt(Math.abs(d) / 60);
                seconds = Math.abs(d) % 60;
                var hours = parseInt(minutes / 60);
                minutes = minutes % 60;
                if (seconds.toString().length == 1) {
                    seconds = '0' + seconds;
                }
                if (minutes.toString().length == 1) {
                    minutes = '0' + minutes;
                }
                if (hours.toString().length === 1) {
                    hours = '0' + hours;
                }

                if (d % 1 !== 0) {
                    seconds = seconds.toFixed(2);
                }

                return "-" + hours + ":" + minutes + ":" + seconds + " ";
            }
        };

        var plotRange = function () {
            if ($scope.livePlotter && $scope.livePlotter.plotRange) {
                return $scope.livePlotter.plotRange;
            } else {
                return 0;
            }
        };

        var timer = null;

        $scope.plotDisabled = function () {
            return $scope.selectedChannels.length === 0; // || $scope.selectedChannels.length > 3;
        };

        var updateData = function () {

            var requests = data.map((d) => {
                return ChannelsService.getChannelCurrentValue(d.id).then(r => ({
                    key: d.key,
                    value: r.value,
                    color: d.color,
                    type: d.type,
                    unit: d.unit
                }));
            });

            $q.all(requests).then(function (d) {
                d.forEach((channelData, i) => {

                    var dataI = data[i];
                    dataI.key = channelData.key;
                    if (typeof(channelData.value) !== 'number') {
                        return;
                    }


                    // rotate x values to the left
                    dataI.values.forEach(d => d.x = d.x - ($scope.refresh / 1000));

                    // complete the values with null values
                    for (var j = (maxValuesToDisplay - dataI.values.length); j > 0; j--) {
                        dataI.values.push({x: -(j * ($scope.refresh / 1000)), y: null});
                    }

                    // remove last value
                    if (dataI.values.length > maxValuesToDisplay) {
                        dataI.values.shift();
                    }

                    var y = channelData.value;

                    // push new value
                    dataI.values.push({x: 0, y: y});
                });
                if (!$scope.paused) {
                    buildChart(data);
                }
                d3.selectAll('.nv-series')[0].forEach(function(d,i){
                    var group = d3.select(d);
                    var circle = group.select('circle');
                    circle.attr('transform', 'scale(0.6)');
                });
            });
        };

        $scope.plotData = function () {
            $scope.pause = false;
            $scope.plotting = true;
            if (!$scope.advanced) {
                computeNewRefreshrate();
            }

            // get max values to display in seconds
            if ($scope.timePeriodUnit === 'seconds') {
                maxValuesToDisplay = 1000 / $scope.refresh * $scope.timePeriod;
            } else if ($scope.timePeriodUnit === 'minutes') {
                maxValuesToDisplay = 1000 / $scope.refresh * $scope.timePeriod * 60;
            } else {
                maxValuesToDisplay = 1000 / $scope.refresh * $scope.timePeriod * 60 * 60;
            }

            data = $scope.selectedChannels.map((channel) => ({
                id: channel.id,
                key: channel.label,
                values: [],
                color: channel.color,
                type: channel.type,
                unit: channel.unit
            }
            ));

            $interval.cancel(timer);
            updateData(); // update now, because the interval starts with a delay.
            timer = $interval(updateData, $scope.refresh);

        };

        $scope.togglePlotting = function () {
            $scope.paused = !$scope.paused;
            $scope.plotting = !$scope.paused;
        };

        $scope.$on('$destroy', () => {
            $interval.cancel(timer);
            timer = null;
        });

    };


    LivePlotterController.$inject = injectParams;

    angular.module('openmuc.dataplotter').controller('LivePlotterController', LivePlotterController);
})();
