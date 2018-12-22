(function () {

    var injectParams = ['$scope', '$stateParams', '$state', '$q', '$translate', 'ChannelsService', '$alert'];
    var noData;

    var DataPlotterController = function ($scope, $stateParams, $state, $q, $translate, ChannelsService, $alert) {

        if ($stateParams.name) {
            $scope.dataPlotter = $scope.getPlotter($stateParams.name);
        }

        if ($scope.dataPlotter && $scope.dataPlotter.startDate) {
            $scope.startDate = new Date(parseInt($scope.dataPlotter.startDate));
        } else {
            //default start time of plot interval 16 hours in the past (rounded to full hrs)
            var now = new Date();
            $scope.startDate = new Date(now.setHours(now.getHours() - 16, 0, 0, 0));
        }

        if ($scope.dataPlotter && $scope.dataPlotter.endDate) {
            $scope.endDate = new Date(parseInt($scope.dataPlotter.endDate));
        } else {
            //default final time of plot interval next full hour in the future
            var now = new Date();
            $scope.endDate = new Date(now.setHours(now.getHours() + 1, 0, 0, 0));

        }

        if ($scope.dataPlotter && $scope.dataPlotter.yAxisLabel) {
            $scope.yLabel = $scope.dataPlotter.yAxisLabel;
        } else {
            $translate('VALUES').then((text) => $scope.yLabel = text);
        }

        if ($scope.dataPlotter && $scope.dataPlotter.xAxisLabel) {
            $scope.xLabel = $scope.dataPlotter.xAxisLabel;
        } else {
            $translate('TIME').then((text) => $scope.xLabel = text);
        }

        $scope.isTS = $scope.dataPlotter && $scope.dataPlotter.isTS;

        $translate('NO_DATA_TO_DISPLAY').then((text) => noData = text);

        $scope.channels = [];
        $scope.selectedChannels = [];

        ChannelsService.getAllChannelsIds().then(function (channels) {

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
            }

            //console.log($scope.channels);

        });

        nv.addGraph(function () {
            var data = [];
            buildChart(data);
        });


        function buildChart(data) {
            var chart = nv.models.lineChart()
                .margin({left: 70, right: 22})  //Adjust chart margins to give the x-axis some breathing room.
                .interactive(true)
                .useInteractiveGuideline(true)  //We want nice looking tooltips and a guideline!
                .showLegend(false)       //Show the legend, allowing users to turn on/off line series.
                .showYAxis(true)        //Show the y-axis
                .showXAxis(true)        //Show the x-axis
                .tooltips(true)
                .forceY($scope.plotRange())
                .noData(noData)
                .width(450)
                .height(300);

            chart.xAxis     //Chart x-axis settings
                .axisLabel($scope.xAxisLabel())
                .tickFormat($scope.xAxisTickFormat());

            chart.yAxis     //Chart y-axis settings
                .axisLabel($scope.yAxisLabel())
                .tickFormat($scope.yAxisTickFormat());

            /* Done setting the chart up? Time to render it!*/
            //     d3.select('#graph svg')    //Select the <svg> element you want to render the chart in.
            d3.select('#graph svg')
                .datum(data)         //Populate the <svg> element with chart data...
                .transition().duration(350)  //how fast do you want the lines to transition?
                .call(chart);          //Finally, render the chart!

            //Update the chart when window resizes.
            nv.utils.windowResize(function () {
                chart.update()
            });

            return chart;
        }

        $scope.plotData = function () {
            var requests = $scope.selectedChannels.map((channel) => {
                if ($scope.isTS) {
                    return ChannelsService.getTSChannelValuesForDiagram(channel.id).then(function (response) {
                        return {
                            key: channel.id,
                            values: response,
                            color: channel.color
                        };
                    });
                } else {
                    return ChannelsService.getHistoryValues(channel.id, $scope.startDate.getTime(), $scope.endDate.getTime()).then(function (response) {
                        return {
                            key: channel.label,
                            values: response,
                            color: channel.color
                        };
                    });
                }
            });

            $q.all(requests).then((d) => {
                var d2 = d.filter((c) => c.values.length !== 0);
                if (d2.length !== d.length) {
                    $alert({content: 'Can not plot all channels.', type: 'warning'});
                }
                buildChart(d2);
            });
        };

        $scope.xFunction = function () {
            return function (d) {
                return d.x;
            };
        };

        $scope.yFunction = function () {
            return function (d) {
                return d.y;
            };
        };

        $scope.xAxisLabel = function () {
            return $scope.xLabel;
        };

        $scope.yAxisLabel = function () {
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
        var xRangeHrs = function () {
            var delta = $scope.endDate - $scope.startDate;
            return delta / (60 * 60 * 1000);
        };

        var fullHrsInRaster = function (widthHrs) {
            var hourOfDayStart = $scope.startDate.getHours();
            var div = Math.floor(hourOfDayStart / widthHrs);
            var initialXTick = new Date($scope.startDate);
            initialXTick.setHours(widthHrs * (div + 1));
            initialXTick.setMinutes(0);
            initialXTick.setSeconds(0);
            initialXTick.setMilliseconds(0);
            var ret = [];

            while (initialXTick <= $scope.endDate) {
                ret.push(initialXTick);
                initialXTick = new Date(initialXTick + widthHrs);
            }
            return ret;
        };

        $scope.xAxisTicks = function () {
            var xRange = xRangeHrs();
            if (xRange <= 1) {
                return [];
            } else if (xRange <= 3) {
                return fullHrsInRaster(1);
            } else if (xRange <= 24) {
                return fullHrsInRaster(3);
            } else if (xRange <= 48) {
                return fullHrsInRaster(12);
            } else {
                return fullHrsInRaster(24);
            }
        };

        $scope.xAxisTickFormat = function () {
            return (d) => d3.time.format('%m.%d. %H:%M')(new Date(d));
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
            return (d) => d;
        };

        $scope.disabledPlot = function () {
            return $scope.selectedChannels.length == 0;// || $scope.selectedChannels.length > 3;
        };

        $scope.plotRange = function () {
            if ($scope.dataPlotter && $scope.dataPlotter.plotRange) {
                return $scope.dataPlotter.plotRange;
            } else {
                return 0;
            }
        };

    };

    DataPlotterController.$inject = injectParams;

    angular.module('openmuc.dataplotter').controller('DataPlotterController', DataPlotterController);

})();
