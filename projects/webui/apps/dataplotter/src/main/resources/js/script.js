//JS script for dataplotter

//Global Variables
//var labelList[label][description][unit]		->index.html
var plot;
var timeout;
var force0 = false;

function getyaxisLabel(label) {
    var unit = [];
    var description = [];
    var yaxisLabel = [];

    $.each(label, function (index, value) {
        description.push(labelList[1][$.inArray(value, labelList[0])]);
        unit.push(labelList[2][$.inArray(value, labelList[0])]);
        if (description[0] != "$des") {
            yaxisLabel.push(description[index] + " [" + unit[index] + "]");
        }
        else {
            yaxisLabel.push(value + " [" + unit[index] + "]");
        }
    });

    return yaxisLabel;
}


//Plots points with label on specific div
function plotJSON(div, points, label) {
    var yaxisLabel = getyaxisLabel(label);
    var plotOptions = {
        seriesDefaults: {
            showMarker: false,							//no markers
            pointLabels: {xpadding: 0},
            shadow: false,
            breakOnNull: true
        },
        series: [
            {},
            {yaxis: 'y2axis'},
            {yaxis: 'y3axis'}
        ],
        cursor: {
            show: true,
            tooltipLocation: 'sw',
            zoom: true
        },
        axesDefaults: {useSeriesColor: true},
        axes: {
            xaxis: {
                renderer: $.jqplot.DateAxisRenderer,
                tickOptions: {
                    formatString: '%v<br>%T'			//Format for the X-Axes
                },
                pad: 0,								//Padding of the X-Axes
                numberTicks: 6						//How many ticks on the x Axis
            },
            yaxis: {
                tickOptions: {
                    formatString: '%.2f',
                    //textColor: '#4bb2c5'
                },
                label: yaxisLabel[0],
                labelRenderer: $.jqplot.CanvasAxisLabelRenderer,
                borderWidth: 4,
                rendererOptions: {
                    forceTickAt0: force0
                },
                pad: 0

            },
            y2axis: {
                tickOptions: {
                    formatString: '%.2f',
                    showGridline: false,
                    //textColor: '#EAA228'
                },
                label: yaxisLabel[1],
                labelRenderer: $.jqplot.CanvasAxisLabelRenderer,
                borderWidth: 4,
                rendererOptions: {
                    forceTickAt0: force0
                },
                pad: 0
            },
            y3axis: {
                tickOptions: {
                    formatString: '%.2f',
                    showGridline: false,
                    //textColor: '#c5b47f'
                },
                label: yaxisLabel[2],
                labelRenderer: $.jqplot.CanvasAxisLabelRenderer,
                borderWidth: 4,
                rendererOptions: {
                    forceTickAt0: force0
                },
                pad: 0
            },
        }
    }

    if (force0) {
        $('#' + div + 'zoom').html("<a id=force" + div + " href=# style=\"text-decoration: none;\">+</a>")
    }
    else {
        $('#' + div + 'zoom').html("<a id=force" + div + " href=# style=\"text-decoration: none;\">-</a>")
    }

    $('#force' + div).click(function (event) {
        event.preventDefault();
        if (force0) {
            $('#force' + div).html("-")
            force0 = false;
            plotOptions.axes.yaxis.rendererOptions.forceTickAt0 = false;
            plotOptions.axes.y2axis.rendererOptions.forceTickAt0 = false;
            plotOptions.axes.y3axis.rendererOptions.forceTickAt0 = false;
        }
        else {
            $('#force' + div).html("+")

            force0 = true;
            plotOptions.axes.yaxis.rendererOptions.forceTickAt0 = true;
            plotOptions.axes.y2axis.rendererOptions.forceTickAt0 = true;
            plotOptions.axes.y3axis.rendererOptions.forceTickAt0 = true;
        }
        try {
            delete plot;
            plot = undefined;
        }
        catch (err) {
        }

        $('#' + div).empty();									//delete the old plot, or the error message

        try {
            plot = $.jqplot(div, points, plotOptions);
        }
        catch (err) {
            $('#' + div).append("Sorry, no data to plot!");
        }
    });

    try {
        delete plot;
        plot = undefined;
    }
    catch (err) {
    }

    $('#' + div).empty();									//delete the old plot, or the error message

    try {
        plot = $.jqplot(div, points, plotOptions);
    }
    catch (err) {
        $('#' + div).append("Sorry, no data to plot!");
    }
}

//Functions for dataPlotter
function reset() {
    var time = new Date();
    $('#endH').val(time.getHours());
    $('#endM').val(time.getMinutes());
    $('#endDate').datepicker("setDate", time);

    time.setDate(time.getDate() - 1);

    $('#startH').val(time.getHours());
    $('#startM').val(time.getMinutes());
    $('#startDate').datepicker("setDate", time);

    $('#resolution').val("500");

    $(':checkbox:checked').each(function (i) {
        $(this).prop("checked", false);
    });
    $("#Label1").prop("checked", true);

    window.clearTimeout(timeout);
}

function submitClicked() {
    var start;
    var end;


    var label = [];

    $(':checkbox:checked').each(function (i) {
        label[i] = $(this).val();
    });

    while (label.length > 3) {										//only 2 labels get calculated
        label.pop();
    }

    var resolution = parseInt($("#resolution").val(), 10);			//is a Number because of spinner

    start = $('#startDate').datepicker("getDate");				//Get Start Time from Datepicker
    if (start == null || start == undefined) {
        start = new Date();
        start.setDate(start.getDate() - 1);
    }
    else {
        start.setHours(start.getHours() + $('#startH').val());
        start.setMinutes(start.getMinutes() + $('#startM').val());
    }
    var startStamp = start.getTime();								//Get the UNIX Timestamp

    end = $('#endDate').datepicker("getDate");
    if (end == null || end == undefined) {
        end = new Date();
    }
    else {
        end.setHours(end.getHours() + $('#endH').val());
        end.setMinutes(end.getMinutes() + $('#endM').val());
    }
    var endStamp = end.getTime();

    updateJSON(startStamp, endStamp, label, resolution);
}

function updateJSON(startStamp, endStamp, label, resolution) {
    var points = [];
    getData(label.length, 0, points, startStamp, endStamp, label, resolution);

}

function getData(requests, index, points, startStamp, endStamp, label, resolution) {
    $.getJSON("dataplotter/getData", {
        start: startStamp,
        end: endStamp,
        label: label[index],
        resolution: resolution
    }, function (data) {
        points[index] = new Array();
        $.each(data, function (i, value) {
            if (value.flag == "VALID") {
                points[index].push([value.timestamp, value.value]);
            }
            else {
                points[index].push([value.timestamp, null]);
            }
        });
        requests--;
        index++;
        if (requests > 0) {
            getData(requests, index, points, startStamp, endStamp, label, resolution)
        }
        else {
            plotJSON('plot', points, label);
        }
    });
}

//Functions for Live Plot
function resetLive() {
    $('#lastH').val("0");
    $('#lastM').val("10");
    $('#lastS').val("0");

    $('#refresh').val("10")

    $(':checkbox:checked').each(function (i) {
        $(this).prop("checked", false);
    });
    $('#LabelLive1').prop("checked", true);
    window.clearTimeout(timeout);
}

function submitLiveClicked() {

    window.clearTimeout(timeout);
    var points = [];
    var label = [];

    $(':checkbox:checked').each(function (i) {
        label[i] = $(this).val();
    });

    while (label.length > 3) {										//only 2 labels get calculated
        label.pop();
    }

    $.each(label, function (index, value) {
        points[index] = new Array();
    });

    time = $('#lastH').val() * 3600000;
    time = time + $('#lastM').val() * 60000;
    time = time + $('#lastS').val() * 1000;
    refresh = $('#refresh').val() * 1000;
    updateLiveJSON(time, label, refresh, points);
}

function updateLiveJSON(time, label, refresh, points) {
    getLiveData(label.length, 0, points, time, refresh, label);
}

function getLiveData(requests, index, points, time, refresh, label) {

    $.getJSON("dataplotter/getLiveData", {label: label[index]}, function (data) {
        points[index].push([data.timestamp, data.value]);

        if (points[index][0][0] < (points[index][points[index].length - 1][0] - time)) {
            points[index].splice(0, 1);
        }

        requests--;
        index++;
        if (requests > 0) {
            getLiveData(requests, index, points, time, refresh, label);
        }
        else {
            plotJSON('plotLive', points, label);
            timeout = window.setTimeout(function () {
                updateLiveJSON(time, label, refresh, points);
            }, refresh);
        }
    });
}


//Functions Bar Plotter
function resetBar() {
    zoom = 0;
    var time = new Date();
    start = new Date(time.getFullYear(), time.getMonth(), 1);
    end = new Date(time.getFullYear(), time.getMonth() + 1, 0);

    $(':checkbox:checked').each(function (i) {
        $(this).prop("checked", false);
    });
    $("#LabelBar1").prop("checked", true);

    window.clearTimeout(timeout);
    submitBarClicked(start.getTime(), end.getTime());
}


function submitBarClicked(start, end) {
    window.clearTimeout(timeout);
    var label = [];

    $(':checkbox:checked').each(function (i) {
        label[i] = $(this).val();
    });

    while (label.length > 3) {										//only 2 labels get calculated
        label.pop();
    }

    updateBarJSON(label, start, end);

}

function updateBarJSON(label, startStamp, endStamp) {
    var points = [];
    var steps;
    if (zoom == 0) {
        endStamp = endStamp + 86400000;									//including day selected
        steps = endStamp - startStamp;
        steps = steps / 86400000;
    }
    else if (zoom == 1) {
        endStamp = endStamp + 86400000;									//including day selected
        steps = endStamp - startStamp;
        steps = steps / 3600000;
    }
    else if (zoom == 2) {
        steps = endStamp - startStamp;
        steps = steps / 900000;
    }
    getBarData(label.length, 0, points, startStamp, endStamp, label, steps);
}


function getBarData(requests, index, points, startStamp, endStamp, label, steps) {
    $.getJSON("dataplotter/getBarData", {
        start: startStamp,
        end: endStamp,
        label: label[index],
        steps: steps
    }, function (data) {
        points[index] = new Array();
        $.each(data, function (i, value) {
            points[index].push([value.timestamp, value.value, value.flag]);
        });

        requests--;
        index++;
        if (requests > 0) {
            getBarData(requests, index, points, startStamp, endStamp, label, steps)
        }
        else {
            plotBar('plotBar', points, label);
        }
    });
}

function plotBar(div, points, label) {
    var yaxisLabel = getyaxisLabel(label);
    var date = new Date();
    var ticks = [];
    var newpoints = [];

    $.each(points[0], function (index, value) {
        date.setTime(value[0]);
        if (zoom == 0) {
            ticks.push(date.getDate());
        }
        else if (zoom == 1) {
            ticks.push(date.getHours() + "h");
        }
        else if (zoom == 2)
            ticks.push(date.getHours() + ":" + date.getMinutes());
    });

    var monthNames = ["January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December"];

    if (zoom == 0) {
        $('#month').html("<center> <a id=preMonth href=#>&lt;&lt;</a> "
        + monthNames[date.getMonth()]
        + " <a id=nextMonth href=#>&gt;&gt;</a></center>");

        $('#preMonth').click(function (event) {
            event.preventDefault();
            start = new Date(start.getFullYear(), start.getMonth() - 1, 1);
            end = new Date(start.getFullYear(), start.getMonth() + 1, 0);
            submitBarClicked(start.getTime(), end.getTime());
        });

        $('#nextMonth').click(function (event) {
            event.preventDefault();
            start = new Date(start.getFullYear(), start.getMonth() + 1, 1);
            end = new Date(start.getFullYear(), start.getMonth() + 1, 0);
            submitBarClicked(start.getTime(), end.getTime());
        });

    }
    else if (zoom == 1) {
        $('#month').html("<center> <a id=preDay href=#>&lt;&lt;</a> "
        + date.getDate() + " - <a id=disabelzoom href=#>"
        + monthNames[date.getMonth()]
        + "</a> <a id=nextDay href=#>&gt;&gt;</a></center>");

        $('#preDay').click(function (event) {
            event.preventDefault();
            start = new Date(start.getFullYear(), start.getMonth(), start.getDate() - 1);
            end = start;
            submitBarClicked(start.getTime(), end.getTime());
        });

        $('#nextDay').click(function (event) {
            event.preventDefault();
            start = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 1);
            end = start;
            submitBarClicked(start.getTime(), end.getTime());
        });

        $('#disabelzoom').click(function (event) {
            event.preventDefault();
            zoom = 0;
            start = new Date(start.getFullYear(), start.getMonth(), 1);
            end = new Date(start.getFullYear(), start.getMonth() + 1, 0);
            submitBarClicked(start.getTime(), end.getTime());
        });
    }
    else if (zoom == 2) {
        $('#month').html("<center> <a id=preHour href=#>&lt;&lt;</a> "
        + "<a id=zoom1 href=#>"
        + date.getDate() + "</a> - <a id=zoom0 href=#>"
        + monthNames[date.getMonth()]
        + "</a> <a id=nextHour href=#>&gt;&gt;</a></center>");

        $('#preHour').click(function (event) {
            event.preventDefault();
            start = new Date(start.getTime() - 3600000);
            end = new Date(end.getTime() - 3600000);
            submitBarClicked(start.getTime(), end.getTime());
        });

        $('#nextHour').click(function (event) {
            event.preventDefault();
            start = new Date(start.getTime() + 3600000);
            end = new Date(end.getTime() + 3600000);
            submitBarClicked(start.getTime(), end.getTime());
        });

        $('#zoom0').click(function (event) {
            event.preventDefault();
            zoom = 0;
            start = new Date(start.getFullYear(), start.getMonth(), 1);
            end = new Date(start.getFullYear(), start.getMonth() + 1, 0);
            submitBarClicked(start.getTime(), end.getTime());
        });

        $('#zoom1').click(function (event) {
            event.preventDefault();
            zoom = 1;
            start = new Date(start.getFullYear(), start.getMonth(), start.getDate());
            end = start;
            submitBarClicked(start.getTime(), end.getTime());
        });
    }

    $('#year').html("<center> <a id=preYear href=#>&lt;&lt;</a> "
    + date.getFullYear()
    + " <a id=nextYear href=#>&gt;&gt;</a></center>");

    $('#preYear').click(function (event) {
        event.preventDefault();
        start = new Date(start.getFullYear() - 1, start.getMonth(), start.getDate());
        end = new Date(end.getFullYear() - 1, start.getMonth(), end.getDate());
        submitBarClicked(start.getTime(), end.getTime());
    });

    $('#nextYear').click(function (event) {
        event.preventDefault();
        start = new Date(start.getFullYear() + 1, start.getMonth(), start.getDate());
        end = new Date(end.getFullYear() + 1, start.getMonth(), end.getDate());
        submitBarClicked(start.getTime(), end.getTime());
    });


    $.each(points, function (i, v) {
        newpoints[i] = new Array();
        $.each(v, function (index, value) {
            newpoints[i].push(value[1]);
        });
    });


    var canvasLines = [];
    var scaler = points.length;

    $.each(points, function (i, v) {

        $.each(v, function (index, value) {
            var offset = index + ((i + 1) / (scaler + 1));
            if (value[2] != "VALID") {
                var cap;
                if (value[1] == 0) {
                    cap = 'round';
                    value[1] = 0.01;		//chromium prints no round cap with no data
                }
                else {
                    cap = 'butt'
                }
                canvasLines.push({
                    line: {
                        name: 'invalid mark',
                        start: [offset + 0.5, 0],
                        stop: [offset + 0.5, value[1]],
                        lineWidth: (8 / scaler),
                        color: 'rgb(255, 80, 0)',
                        shadow: false,
                        lineCap: cap
                    }
                });
            }
        });
    });

    $('#' + div).unbind();

    $('#' + div).bind('jqplotDataClick', function (ev, seriesIndex, pointIndex, data) {
        if (zoom == 0) {
            zoom = 1;
            start = new Date(start.getFullYear(), start.getMonth(), pointIndex + 1);
            end = start;
            submitBarClicked(start.getTime(), end.getTime());
        }
        else if (zoom == 1) {
            zoom = 2;
            start = new Date(start.getFullYear(), start.getMonth(), start.getDate(), pointIndex - 1);
            end = new Date(start.getFullYear(), start.getMonth(), start.getDate(), pointIndex + 2);
            submitBarClicked(start.getTime(), end.getTime());
        }
    });


    $('#' + div).bind('jqplotDataHighlight', function (ev, seriesIndex, pointIndex, data) {
        $('#highlighter').html(yaxisLabel[seriesIndex] + ": " + Math.round(points[seriesIndex][pointIndex][1] * 100) / 100);
        if (points[seriesIndex][pointIndex][2] != "VALID") {
            $('#highlighter').append(" (Invalid data)");
        }
    });
    $('#' + div).bind('jqplotDataUnhighlight', function (ev) {
        $('#highlighter').html(" ");
    });

    var plotOptions = {
        enablePlugins: true,
        seriesDefaults: {
            renderer: $.jqplot.BarRenderer,
            rendererOptions: {
                fillToZero: true,
                barMargin: 2,
                barPadding: 0,
                barWidth: null
            },
            shadow: false
        },
        axes: {
            xaxis: {
                renderer: $.jqplot.CategoryAxisRenderer,
                ticks: ticks
            },
            yaxis: {
                tickOptions: {formatString: '%.2f'},
                autoscale: true
            }
        },
        legend: {
            show: true
        },
        series: [
            {
                label: yaxisLabel[0]
            },
            {
                label: yaxisLabel[1]
            },
            {
                label: yaxisLabel[2]
            }
        ],
        canvasOverlay: {									//draw a line at y=0, forces the plot to show 0
            show: true,
            objects: canvasLines
        }
    }
    try {
        delete plot;
        plot = undefined;
    }
    catch (err) {
    }

    $('#' + div).empty();									//delete the old plot, or the error message

    try {
        plot = $.jqplot(div, newpoints, plotOptions);


        //Append to the legend a fake label, for the invalid data
        $('table.jqplot-table-legend').append(
            "<tr class=\"jqplot-table-legend\">"
            + "<td class=\"jqplot-table-legend\">"
            + "<div class=\"jqplot-table-legend\">"
            + "<div class=\"jqplot-table-legend\" style=\"background-color: rgb(255,80,0); height:8px; padding:1px;\"></div>"
            + "</div>"
            + "</td>"
            + "<td class=\"jqplot-table-legend\">Invalid Data</td>"
            + "</tr>");

    }
    catch (err) {
        $('#' + div).append("Sorry, no data to plot!");
    }
}


//jqplotToImg
//
//Plots an div to an image
//https://bitbucket.org/cleonello/jqplot/issue/14/export-capabilities#comment-550704
//should move into jqPlot
function jqplotToImg(obj) {
    var newCanvas = document.createElement("canvas");
    newCanvas.width = obj.find("canvas.jqplot-base-canvas").width();
    newCanvas.height = obj.find("canvas.jqplot-base-canvas").height() + 10;
    var baseOffset = obj.find("canvas.jqplot-base-canvas").offset();

    // make white background for pasting
    var context = newCanvas.getContext("2d");
    context.fillStyle = "rgba(255,255,255,1)";
    context.fillRect(0, 0, newCanvas.width, newCanvas.height);

    obj.children().each(function () {
        // for the div's with the X and Y axis
        if ($(this)[0].tagName.toLowerCase() == 'div') {
            // X axis is built with canvas
            $(this).children("canvas").each(function () {
                var offset = $(this).offset();
                newCanvas.getContext("2d").drawImage(this,
                    offset.left - baseOffset.left,
                    offset.top - baseOffset.top
                );
            });
            // Y axis got div inside, so we get the text and draw it on the canvas
            $(this).children("div").each(function () {
                var offset = $(this).offset();
                var context = newCanvas.getContext("2d");
                context.font = $(this).css('font-style') + " " + $(this).css('font-size') + " " + $(this).css('font-family');
                context.fillStyle = $(this).css('color');
                context.fillText($(this).text(),
                    offset.left - baseOffset.left,
                    offset.top - baseOffset.top + $(this).height()
                );
            });
        } else if ($(this)[0].tagName.toLowerCase() == 'canvas') {
            // all other canvas from the chart
            var offset = $(this).offset();
            newCanvas.getContext("2d").drawImage(this,
                offset.left - baseOffset.left,
                offset.top - baseOffset.top
            );
        }
    });

    // add the point labels
    obj.children(".jqplot-point-label").each(function () {
        var offset = $(this).offset();
        var context = newCanvas.getContext("2d");
        context.font = $(this).css('font-style') + " " + $(this).css('font-size') + " " + $(this).css('font-family');
        context.fillStyle = $(this).css('color');
        context.fillText($(this).text(),
            offset.left - baseOffset.left,
            offset.top - baseOffset.top + $(this).height() * 3 / 4
        );
    });

    // add the title
    obj.children("div.jqplot-title").each(function () {
        var offset = $(this).offset();
        var context = newCanvas.getContext("2d");
        context.font = $(this).css('font-style') + " " + $(this).css('font-size') + " " + $(this).css('font-family');
        context.textAlign = $(this).css('text-align');
        context.fillStyle = $(this).css('color');
        context.fillText($(this).text(),
            newCanvas.width / 2,
            offset.top - baseOffset.top + $(this).height()
        );
    });

    // add the legend
    obj.children("table.jqplot-table-legend").each(function () {
        var offset = $(this).offset();
        var context = newCanvas.getContext("2d");
        context.strokeStyle = $(this).css('border-top-color');
        context.strokeRect(
            offset.left - baseOffset.left,
            offset.top - baseOffset.top,
            $(this).width(), $(this).height()
        );
        context.fillStyle = $(this).css('background-color');
        context.fillRect(
            offset.left - baseOffset.left,
            offset.top - baseOffset.top,
            $(this).width(), $(this).height()
        );
    });

    // add the rectangles
    obj.find("div.jqplot-table-legend-swatch").each(function () {
        var offset = $(this).offset();
        var context = newCanvas.getContext("2d");
        context.fillStyle = $(this).css('background-color');
        context.fillRect(
            offset.left - baseOffset.left,
            offset.top - baseOffset.top,
            $(this).parent().width(), $(this).parent().height()
        );
    });

    obj.find("td.jqplot-table-legend").each(function () {
        var offset = $(this).offset();
        var context = newCanvas.getContext("2d");
        context.font = $(this).css('font-style') + " " + $(this).css('font-size') + " " + $(this).css('font-family');
        context.fillStyle = $(this).css('color');
        context.textAlign = $(this).css('text-align');
        context.textBaseline = $(this).css('vertical-align');
        context.fillText($(this).text(),
            offset.left - baseOffset.left,
            offset.top - baseOffset.top + $(this).height() / 2 + parseInt($(this).css('padding-top').replace('px', ''))
        );
    });

    // convert the image to base64 format
    return newCanvas.toDataURL("image/png");
}


