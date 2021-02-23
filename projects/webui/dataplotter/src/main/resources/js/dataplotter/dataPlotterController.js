(function () {

    var injectParams = ['$scope', '$stateParams', '$state', '$q', '$translate', 'ChannelsService', 'notify', 'ChannelDataService'];
    var noData;

    var DataPlotterController = function ($scope, $stateParams, $state, $q, $translate, ChannelsService, notify, ChannelDataService) {

        $scope.mstep = 1;
        $scope.options = {mstep: [1, 5, 10, 15]} 

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

        $scope.isTS = $scope.dataPlotter && $scope.dataPlotter.isTS;

        $translate('NO_DATA_TO_DISPLAY').then(text => noData = text);

        $scope.channels = [];
        $scope.selectedChannels = [];

        ChannelsService.getAllChannels().then(async function (channels) {
            channels = channels.records;
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
                    return {id: channel.id, label: channel.id, preselect: false, valueType: channel.valueType};
                });

                for (let channel of $scope.channels){
                    var config = await ChannelDataService.channelHasHistoricValues(channel);
                    if (config != false) {
                        channel.historic = await config;
                        var logging = await ChannelDataService.getChannelConfig(channel, 'loggingInterval');
                        channel.loggingInterval =  await logging.loggingInterval;
                    }
                }

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
            }
            //console.log($scope.channels);

        });
        
        var buildChart = function (data) {
            var chart = nv.models.lineWithFocusChart()
                .margin({left: 55, right: 25})  //Adjust chart margins to give the x-axis some breathing room.
                .interactive(true)
                .useInteractiveGuideline(true)  //We want nice looking tooltips and a guideline!
                .showLegend(true)       //Show the legend, allowing users to turn on/off line series.
                .showYAxis(true)        //Show the y-axis
                .showXAxis(true)        //Show the x-axis
                .forceY(plotRange())
                .noData(noData)
                .width(450)
                .height(300);

            chart.legend.maxKeyLength(100);

            if (data == undefined){
                console.log(data);
                chart.useInteractiveGuideline(false);
                chart.showXAxis(false);
            }

            chart.interactiveLayer.tooltip.contentGenerator(function (d) {
                var unixDate = new Date(d.value+1000);
                var month = unixDate.getMonth() + 1;
                var date = ('0'+unixDate.getDate()).slice(-2);
                var hour = unixDate.getHours();
                var min = ('0'+unixDate.getMinutes()).slice(-2);
                var time = date + '.' + month + '. ' +  hour + ':' + min;
                var header = time;
                var headerhtml = "<thead><tr><td colspan='3'><strong class='x-value'>" + header + "</strong></td></tr></thead>";
                var bodyhtml = "<tbody>";
                var series = d.series;
                series.forEach((c, i) => {
                    var unit;
                    let channel = data.find(o => o.key === c.key);
                    if (channel.unit == undefined) {
                        unit = '';
                    } else {
                        unit = ' ' + channel.unit;
                    }
                    var valueUnit;
                    if (c.value == undefined) {
                        valueUnit = 'undefined';
                    } else {
                        valueUnit = c.value.toFixed(3) + unit;
                    }

                    bodyhtml = bodyhtml + '<tr><td class="legend-color-guide"><div style="background-color: ' + c.color + ';"></div></td><td class="key">' + c.key
                        + '</td><td class="value">' + valueUnit
                        + '</td></tr>';

                });
                bodyhtml = bodyhtml + '</tbody>';
                return "<table>" + headerhtml + '' + bodyhtml + "</table>";
            });    

            data.forEach((c, i) => {
                let channel = data.find(o => o.key === c.key);
                position = data.indexOf(channel); 
                channel.values.forEach(function(value){
                    valuePosition = channel.values.indexOf(value);
                    if (value.x !== channel.values[valuePosition+1].x - channel.loggingInterval && value.y !== null){
                        data[position].values.splice(valuePosition+1, 0, {x: value.x+channel.loggingInterval, y: null, series: 0});
                    }
                }); 
            });    

            chart.xAxis     //Chart x-axis settings
                .tickFormat(xAxisTickFormat());

            chart.yAxis     //Chart y-axis settings
                .tickFormat(d3.format('.03f'));

            chart.x2Axis     //Chart x-axis settings
                .tickFormat(xAxisTickFormat());

            /* Done setting the chart up? Time to render it!*/
            //     d3.select('#graph svg')    //Select the <svg> element you want to render the chart in.
            d3.select('#graph svg')
                .datum(data)         //Populate the <svg> element with chart data...
                .transition().duration(350)  //how fast do you want the lines to transition?
                .call(chart);          //Finally, render the chart!

            //Update the chart when window resizes.
            nv.utils.windowResize(() => chart.update());
            d3.selectAll('.nv-series')[0].forEach(function(d,i){
                var group = d3.select(d);
                var circle = group.select('circle');
                circle.attr('transform', 'scale(0.6)');
            });
            return chart;
        };

        nv.addGraph(function () {
            var data = [];
            buildChart(data);
        });

        $scope.channelPlottable = function (channel) {
            return channel.historic && (channel.valueType !== 'STRING');
        };

        $scope.plotData = function () {
            var requests = $scope.selectedChannels.map((channel) => {
                if ($scope.isTS) {
                    return ChannelsService.getTSChannelValuesForDiagram(channel.id).then(function (response) {
                        return {
                            key: channel.id,
                            values: response,
                            color: channel.color,
                            unit: channel.unit,
                            loggingInterval: channel.loggingInterval
                        };
                    });
                } else {
                    return ChannelsService.getHistoryValues(channel.id, $scope.startDate.getTime(), $scope.endDate.getTime()).then(function (response) {
                        return {
                            key: channel.label,
                            values: response,
                            color: channel.color,
                            unit: channel.unit,
                            loggingInterval: channel.loggingInterval
                        };
                    });
                }
            });

            $q.all(requests).then(d => {
                var d2 = d.filter((c) => c.values.length !== 0);
                if (d2.length !== d.length) {
                    notify({message: 'Can not plot all channels', position: "right", classes: "alert-warning"});
                }
                buildChart(d2);
            });
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


        // TDOD: is this needed? remove
        var xAxisTicks = function () {
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

        var xAxisTickFormat = function () {
            return (d) => d3.time.format('%d.%m. %H:%M')(new Date(d));
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

        var plotRange = function () {
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
