// *** Constants ***
const X = 0; // X value index of the series
const Y = 1; // Y value index of the series

// *** Global Variables ***
var gLastSampleArr;     // global storage of last requested value via JSON. Plotter reads from this storage
var gPlotterActiveFlag; // global flag to determine whether the plotter should run or not. 
var gSeriesLength;		// global series length = number of channels to plot
var gPlotterTimeoutId;  // global id of plotter timeout
var gGetDataTimeoutId;  // global id of getData timeout
var gTimestamp;			 // global timestamp for plotting (value of x-axis)
var gRefreshIntervalInSeconds;	// global refresh interval for creating series and updating the plot

/**
 * Resets the settings of the html
 */
function resetFlotLive() {

    console.debug('###  restFlotLive called ###');

    $('#yAxisMin').prop('disabled', true);
    $('#yAxisMax').prop('disabled', true);
    $('#stopPlot').prop('disabled', true);
    $('#lastS').val("10");
    $('#refresh').val("100");
    $(':checkbox:checked').each(function (i) {
        $(this).prop("checked", false);
    });
    $('#LabelLive1').prop("checked", true);
}


/**
 * Called when user clicked on plot data
 */
function submitLiveFlotClicked() {

    //reset all global variables
//	var gLastSampleArr;     // global storage of last requested value via JSON. Plotter reads from this storage
//	var gPlotterActiveFlag; // global flag to determine whether the plotter should run or not. 
//	var gSeriesLength;		// global series length = number of channels to plot
//	var gPlotterTimeoutId;  // global id of plotter timeout
//	var gGetDataTimeoutId;  // global id of getData timeout
//	var gTimestamp;			 // global timestamp for plotting (value of x-axis)
//	var gRefreshIntervalInSeconds;	// global refresh interval for creating series and updating the plot


    disableControls(true);

    var refreshInterval = $('#refresh').val(); // ms
    var secondsToPlot = $('#lastS').val();
    var maxItemsToPlot = (secondsToPlot / (refreshInterval / 1000)) + 1;


    gTimestamp = 0;
    gRefreshIntervalInSeconds = refreshInterval / 1000;

    var series = createSeries(maxItemsToPlot);
    gSeriesLength = series.length;
    var channelLabels = getChannelLabels(series);

    var plotOptions = getPlotOptions(secondsToPlot);

    plot = $.plot('#plotLive', series, plotOptions);

    // Array as storage for get data and plot data
    // get data writes the current values in the array
    // plot data reads the value from the array
    gLastSampleArr = new Array(series.length);
    for (var seriesNumber = 0; seriesNumber < series.length; seriesNumber++) {
        gLastSampleArr[seriesNumber] = new Array(2);
        gLastSampleArr[seriesNumber][X] = 0; // timestamp
        gLastSampleArr[seriesNumber][Y] = 0; // value
    }

    updateDataForAllChannels(refreshInterval, channelLabels);

//	if($('#fixedRange').is(':checked') && !$('#showLegend').is(':checked') && !$('#xAxisEnable').is(':checked')){
//		// faster than the normal updatePlot(...) method
//		console.info("use updatePlotWithoutSetupGrid method");
//		updatePlotWithoutSetupGrid(refreshInterval,series,plot);		
//	} 
//	else {	
//		console.info("use updatePlot method");
    updatePlot(refreshInterval, series, plot);
//	}

}


/**
 * creates series for plotting
 */
function createSeries(maxItemsToPlot) {
    //create series
    var series = new Array();

    // add a series for each channel
    $(':checkbox:checked').each(function (i) {
        var checkboxId = $(this).attr("id");
        if (checkboxId.indexOf("LabelLive") == 0) {
            var channelName = $(this).val();
//			var unit = getUnit(channelName);
//			console.debug("unit of channel " + channelName + " [" + unit + "]");
//			channelName += " [" + unit + "]";
            var dataPoints = new Array(maxItemsToPlot); //Array which will later contain x and y values of the series
            series.push({label: channelName, data: dataPoints, shadowSize: 0});
        }
    });

    //init series
    var x = 0;
    var y = 0;

    for (var seriesIndex = 0; seriesIndex < series.length; seriesIndex++) {

        x = -gRefreshIntervalInSeconds * (maxItemsToPlot - 1);

        for (var dataIndex = 0; dataIndex < maxItemsToPlot; dataIndex++) {
            series[seriesIndex]["data"][dataIndex] = new Array(2);
            series[seriesIndex]["data"][dataIndex][X] = x;
            series[seriesIndex]["data"][dataIndex][Y] = y;
            x += gRefreshIntervalInSeconds;
        }
    }
    return series;
}


/**
 * builds a string with comma separated channel labels
 */
function getChannelLabels(series) {
    var channelLabels = "";
    for (var i = 0; i < series.length; i++) {
        channelLabels = channelLabels + series[i].label;
        if (i < series.length - 1) {
            channelLabels = channelLabels + ",";
        }
    }
    return channelLabels;
}


function updateDataForAllChannels(refresh, channelLabels) {

    var timeBeforeJSONRequest = new Date();

    $.getJSON("dataplotter/getDataForMultipleChannelsLiveFlot", {labels: channelLabels}, function (data) {

        for (var seriesNumber = 0; seriesNumber < gSeriesLength; seriesNumber++) {
            gLastSampleArr[seriesNumber][X] = gTimestamp;
            gLastSampleArr[seriesNumber][Y] = data[seriesNumber].value;
        }

        gTimestamp += gRefreshIntervalInSeconds;

        var timeAfterJSONRequest = new Date();
        var diffTime = timeAfterJSONRequest - timeBeforeJSONRequest;
        var timeoutForNextUpdate = refresh - diffTime;

        if (gPlotterActiveFlag) {
            if (timeoutForNextUpdate < 0) {
                // if condition true: then the time for JSON request is longer than
                // the refresh interval. therefore the next update is called immediately
                updateDataForAllChannels(refresh, channelLabels);
            } else {
                // if condition false: then call update after the remaining timeout
                gGetDataTimeoutId = setTimeout(function () {
                    updateDataForAllChannels(refresh, channelLabels)
                }, timeoutForNextUpdate);
            }
        } else {
            window.clearTimeout(gGetDataTimeoutId);
            console.debug("stopped getting data");
        }
    });
}

function updatePlot(refresh, series, plot) {
    var timeBeforePlotting = new Date();

    for (var seriesNumber = 0; seriesNumber < series.length; seriesNumber++) {
        // remove element from beginning
        series[seriesNumber]["data"].shift();
        // add new element at end
        series[seriesNumber]["data"].push([gLastSampleArr[seriesNumber][X], gLastSampleArr[seriesNumber][Y]]);
    }

    plot.setData(series);
    plot.setupGrid();
    plot.draw();

    callNextPlotUpdate(timeBeforePlotting, refresh, series, plot);
}

function callNextPlotUpdate(timeBeforePlotting, refresh, series, plot) {

    var timeAfterPlotting = new Date();
    var diffTime = timeAfterPlotting - timeBeforePlotting;
    var timeoutForNextUpdate = refresh - diffTime;

    if (gPlotterActiveFlag) {
        if (timeoutForNextUpdate < 0) {
            // calls updateData immediately
            updatePlot(refresh, series, plot);
        } else {
            // calls updateData after a timeout (might save resources)
            gPlotterTimeoutId = setTimeout(function () {
                updatePlot(refresh, series, plot)
            }, timeoutForNextUpdate);
        }
    } else {
        window.clearTimeout(gPlotterTimeoutId);
        console.debug("stopped plotting");
    }
}

///**
// * Identical to updatePlot but without setupGrid. Used another method instead of an if condition in updatePlot to speed up performance?
// * @param refresh
// * @param series
// * @param plot
// */
//function updatePlotWithoutSetupGrid(refresh, series, plot) {
//	var timeBeforePlotting = new Date();
//	
//	for ( var seriesNumber = 0; seriesNumber < series.length; seriesNumber++) {
//		// remove element from beginning 
//		series[seriesNumber]["data"].shift();	
//		// add new element at end
//		series[seriesNumber]["data"].push( [ gLastSampleArr[seriesNumber][X] ,	gLastSampleArr[seriesNumber][Y] ]);
//	}
//	console.time("plot without Grid");
//	
//	//update x values!
//	
//	var timestamp = Number(0);
//	refresh = Number(refresh);
//	console.warn("timestamp = " + timestamp);
//	for ( var dataArrayIndex = 0; dataArrayIndex < series[0]["data"].length; dataArrayIndex++) {
//		series[0]["data"][dataArrayIndex][X] = timestamp; 
//		timestamp = timestamp + refresh;
//		//console.warn("timestamp = " + timestamp);
//	}
//			
//	debugging_PrintSeriesValues(series, 0);
//	
//	plot.setData(series);
//	//plot.setupGrid();
//	plot.draw();
//	console.timeEnd("plot without Grid");
//	console.warn("timestamp endvalue = " + timestamp);
//	callNextPlotWithoutUpdateSetupGridUpdate(timeBeforePlotting, refresh, series, plot);
//}
//
///**
// * Identical to callNextPlotUpdate but an other plot function is called.
// * @param timeBeforePlotting
// * @param refresh
// * @param series
// * @param plot
// */
//function callNextPlotWithoutUpdateSetupGridUpdate(timeBeforePlotting, refresh, series, plot){
//	
//	var timeAfterPlotting = new Date();
//	var diffTime = timeAfterPlotting - timeBeforePlotting;
//	var timeoutForNextUpdate = refresh - diffTime;
//
//	if (gPlotterActiveFlag) {
//		if (timeoutForNextUpdate < 0) {
//			// calls updateData immediately
//			updatePlotWithoutSetupGrid(refresh, series, plot);
//		} else {
//			// calls updateData after a timeout (might save resources)	
//			gPlotterTimeoutId = setTimeout(function(){updatePlotWithoutSetupGrid(refresh, series, plot)}, timeoutForNextUpdate);
//		}
//	} else {
//		window.clearTimeout(gPlotterTimeoutId); 
//		console.debug("stopped plotting");
//	}	
//}


/**
 * Returns the unit of the channel
 * @param label Label of the channel
 */
function getUnit(label) {
    var unit = labelList[2][$.inArray(label, labelList[0])]
    return unit;
}


/**
 *
 * @returns plot options (like axis settings)
 */
function getPlotOptions() {

    var yAxisMin = $('#yAxisMin').val();
    var yAxisMax = $('#yAxisMax').val();
    var plotOptions;

    // performance boost when legend is not shown in the plot
    // saves around 7 ms (tested on firefox)
    var showLegend;
    if ($('#showLegend').is(':checked')) {
        showLegend = true;
    } else {
        showLegend = false;
    }

    // performance boost when x axis is not shown in the plot
    // saves around 20 ms (tested on firefox)
    var xAxisEnable;
    if ($('#xAxisEnable').is(':checked')) {
        xAxisEnable = true;
    } else {
        xAxisEnable = false;
    }

    var fixedRange;
    if ($('#fixedRange').is(':checked')) {
        fixedRange = true;
    } else {
        fixedRange = false;
    }


    if (fixedRange) {
        // fixed range for y-axis;
        plotOptions = {
            yaxis: {
                show: true,
                ticks: 5,
                min: yAxisMin,
                max: yAxisMax
            },
            xaxis: {
                show: xAxisEnable
            },
            legend: {
                show: showLegend,
                position: "nw",
                labelFormatter: function (label) {
                    return label + " [" + getUnit(label) + "]";
                }
            }
        };

    } else {
        // auto scaling of y-axis
        plotOptions = {
            xaxis: {
                show: xAxisEnable
            },
            legend: {
                show: showLegend,
                position: "nw",
                labelFormatter: function (label) {
                    return label + " [" + getUnit(label) + "]";
                }
            }
        };
    }

    return plotOptions;
}

/**
 * Sets the Flag state
 * @param state true or false
 */
function setPlotterActiveFlag(state) {
    gPlotterActiveFlag = state;
}

/**
 * Disables or enables GUI control
 *
 * @param state true = disable, false = enable
 */
function disableControls(state) {

    // input text fields
    if ($('#fixedRange').is(':checked')) {
        $('#yAxisMin').prop('disabled', state);
        $('#yAxisMax').prop('disabled', state);
    }
    $('#lastS').prop('disabled', state);
    $('#refresh').prop('disabled', state);
    // radio buttons
    $('#radioAutoScaling').prop('disabled', state);
    $('#radioFixedRange').prop('disabled', state);
    // buttons
    $('#submitLivePlot').prop('disabled', state);
    $('#stopPlot').prop('disabled', !state);
    $('#resetLive').prop('disabled', state);
    // check boxes
    $(':checkbox').each(function (i) {
        $(this).prop("disabled", state);
    });
}

/**
 * Prints all data points of a specific series for debugging purpose.
 * @param series Array with all series
 * @param seriesnumber Number of the series to print
 */
function debugging_PrintSeriesValues(series, seriesNumber) {
    for (var dataArrayIndex = 0; dataArrayIndex < series[seriesNumber]["data"].length; dataArrayIndex++) {
        console.debug("series[" + seriesNumber + "][data][" + dataArrayIndex + "][" + X + "] x:" + series[seriesNumber]["data"][dataArrayIndex][X]);
        console.debug("series[" + seriesNumber + "][data][" + dataArrayIndex + "][" + Y + "] y:" + series[seriesNumber]["data"][dataArrayIndex][Y]);
    }
}
