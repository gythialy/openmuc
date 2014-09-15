/*
 * Copyright 2011-14 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openmuc.framework.webui.dataplotter;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.DataLoggerNotAvailableException;
import org.openmuc.framework.webui.spi.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public final class DataPlotter implements WebUiPluginService {

    private final static Logger logger = LoggerFactory.getLogger(DataPlotter.class);

    private BundleContext context;
    private ResourceLoader loader;
    private DataAccessService dataAccessService;

    protected void activate(ComponentContext context) {
        this.context = context.getBundleContext();
        loader = new ResourceLoader(context.getBundleContext());
    }

    protected void setDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    protected void unsetDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = null;
    }

    @Override
    public Hashtable<String, String> getResources() {
        Hashtable<String, String> resources = new Hashtable<String, String>();

        resources.put("css", "css");
        resources.put("js", "js");
        resources.put("images", "images");

        return resources;
    }

    @Override
    public String getAlias() {
        return "dataplotter";
    }

    @Override
    public String getName() {
        return "Data Plotter";
    }

    @Override
    public String getDescription() {
        return "A plotter to visualize measurement data.";
    }

    @Override
    public View getContentView(HttpServletRequest request, PluginContext context) {
        String template = "";

        List<String> labels = dataAccessService.getAllIds();

        if (context.getLocalPath().equals("/getData")) { // AJAX request, for
            // data
            String label = request.getParameter("label");
            long end = new Long(request.getParameter("end"));
            long start = new Long(request.getParameter("start"));
            int resolution = new Integer(request.getParameter("resolution"));
            Channel channel;
            if (labels.contains(label)) {
                channel = dataAccessService.getChannel(label);
            } else {
                channel = dataAccessService.getChannel(labels.get(0));
            }

            List<Record> values = null;
            try {
                values = channel.getLoggedRecords(start, end);
            }
            catch (DataLoggerNotAvailableException e) {
                // TODO Auto-generated catch block
                logger.debug("Cannot get logged Values. Reason " + e);
                return new AjaxView("[]");
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                logger.debug("Cannot get logged Values. Reason " + e);
                return new AjaxView("[]");
            }

            int period = channel.getLoggingInterval();

            for (int i = 1; i < values.size(); i++) {
                if (values.get(i).getTimestamp() - values.get(i - 1).getTimestamp() > period) {
                    values.add(i,
                               new Record(new DoubleValue(0),
                                          values.get(i).getTimestamp() - 1,
                                          Flag.UNKNOWN_ERROR));
                    i++;
                }
            }

            List<Record> newvalues = new ArrayList<Record>();
            String serializedJSONContent;

            if (resolution != 0 && resolution <= values.size()) {
                long diff = (end - start) / (resolution);

                int oldi = 0;
                for (int i = 0; i < values.size(); i++) {
                    if (values.get(i).getTimestamp() >= start + diff || i + 1 == values.size()) {
                        if (i != oldi) {
                            newvalues.add(values.get(i));
                        }

                        oldi = i;

                        start = start + diff;

                    }
                }
                serializedJSONContent = recordsToJson(newvalues);
            } else {
                serializedJSONContent = recordsToJson(values);
            }

            AjaxView view = new AjaxView(serializedJSONContent);
            return view;
        } else if (context.getLocalPath().equals("/getLiveData")) {
            // AJAX request, for livedata
            String label = request.getParameter("label");
            Channel store;
            if (labels.contains(label)) {
                store = dataAccessService.getChannel(label);
            } else {
                store = dataAccessService.getChannel(labels.get(0));
            }

            AjaxView view = new AjaxView(recordToJson(store.getLatestRecord()));

            return view;
        } else if (context.getLocalPath().equals("/getBarData")) {
            String label = request.getParameter("label");
            long start = new Long(request.getParameter("start"));
            long end = new Long(request.getParameter("end"));
            int steps = new Double(request.getParameter("steps")).intValue();

            Channel store;
            if (labels.contains(label)) {
                store = dataAccessService.getChannel(label);
            } else {
                store = dataAccessService.getChannel(labels.get(0));
            }

            List<Record> values = null;
            try {
                values = store.getLoggedRecords(start, end);
            }
            catch (DataLoggerNotAvailableException e) {
                // TODO Auto-generated catch block
                logger.debug("Cannot get logged Values. Reason " + e);
                return new AjaxView("[]");
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                logger.debug("Cannot get logged Values. Reason " + e);
                return new AjaxView("[]");
            }

            ArrayList<Record> barvalues = new ArrayList<Record>();

            long diff = (end - start) / (steps);
            int period = (store.getLoggingInterval()) * 1000;

            int oldi = 0;
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i).getTimestamp() >= start + diff || i + 1 == values.size()) {
                    if (i != oldi) {
                        Record temp;
                        if (values.get(i - 1).getTimestamp() - values.get(oldi).getTimestamp()
                            < diff - period) {
                            // values only valid, if there are data at end and
                            // beginning
                            temp = new Record(new DoubleValue(values.get(i - 1)
                                                                    .getValue()
                                                                    .asDouble()
                                                              - values.get(oldi)
                                                                      .getValue()
                                                                      .asDouble()),
                                              start,
                                              Flag.UNKNOWN_ERROR);
                        } else {
                            temp = new Record(new DoubleValue(values.get(i - 1)
                                                                    .getValue()
                                                                    .asDouble()
                                                              - values.get(oldi)
                                                                      .getValue()
                                                                      .asDouble()),
                                              start,
                                              Flag.VALID);
                        }
                        barvalues.add(temp);
                    } else {
                        Record temp = new Record(new DoubleValue(0), start, Flag.UNKNOWN_ERROR);
                        barvalues.add(temp);
                    }

                    oldi = i;

                    start = start + diff;

                }
            }
            while (start < end) { // Fill requested values with nothing
                barvalues.add(new Record(new DoubleValue(0), start, Flag.UNKNOWN_ERROR));
                start = start + diff;
            }

            // JSONSerializer serializer = new JSONSerializer();
            String serializedJSONContent = recordsToJson(barvalues);
            AjaxView view = new AjaxView(serializedJSONContent);
            return view;
        } else if (context.getLocalPath().equals("/getDataForMultipleChannelsLiveFlotLogged")) {
            // AJAX request, for livedata

            String requestedChannels = request.getParameter("labels");

            long timestamp = Long.parseLong(request.getParameter("timestampT"));

            String[] channelList = requestedChannels.split(",");
            ArrayList<Record> recordList = new ArrayList<Record>();

            for (String channel : channelList) {
                if (labels.contains(channel)) {

                    Record record = null;
                    try {
                        record = dataAccessService.getChannel(channel).getLoggedRecord(timestamp);
                    }
                    catch (DataLoggerNotAvailableException e) {
                        record = dataAccessService.getChannel(channel).getLatestRecord();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (record.getFlag() == Flag.VALID) {
                        recordList.add(record);
                    } else {
                        logger.error("Flag NOT valid for value of channel '" + channel + "'");
                    }

                } else {
                    logger.error("Channel '" + channel + "' not available via dataAccessService");
                }
            }

            AjaxView view = new AjaxView(recordToJsonAdvanced(recordList));
            return view;

        } else if (context.getLocalPath().equals("/getDataForMultipleChannelsLiveFlot")) {
            // AJAX request, for livedata

            String requestedChannels = request.getParameter("labels");

            String[] channelList = requestedChannels.split(",");
            ArrayList<Record> recordList = new ArrayList<Record>();

            for (String channel : channelList) {
                if (labels.contains(channel)) {

                    Record record = dataAccessService.getChannel(channel).getLatestRecord();

                    if (record.getFlag() == Flag.VALID) {
                        recordList.add(record);
                    } else {
                        logger.error("Flag NOT valid for value of channel '" + channel + "'");
                    }

                } else {
                    logger.error("Channel '" + channel + "' not available via dataAccessService");
                }
            }

            AjaxView view = new AjaxView(recordToJsonAdvanced(recordList));
            return view;

        } else if (context.getLocalPath().equals("/dataPlotter")) {
            // AJAX request for data Plotter
            template = loader.getResourceAsString("dataPlotter.html");
            AjaxView view = new AjaxView(template);
            view.addToContext("labels", labels);
            return view;
        } else if (context.getLocalPath().equals("/livePlotter")) {
            // AJAX request for live Plotter
            template = loader.getResourceAsString("livePlotter.html");
            AjaxView view = new AjaxView(template);
            view.addToContext("labels", labels);
            return view;
        } else if (context.getLocalPath().equals("/barPlotter")) {
            // AJAX request for bar Plotter
            template = loader.getResourceAsString("barPlotter.html");
            AjaxView view = new AjaxView(template);
            List<String> barPlottableLabels = removeInapproriateBarPlotUnits(labels);
            view.addToContext("labels", barPlottableLabels);
            return view;
        } else if (context.getLocalPath().equals("/dataLister")) {
            // AJAX request for data Lister
            template = loader.getResourceAsString("dataLister.html");
            AjaxView view = new AjaxView(template);
            view.addToContext("labels", labels);
            return view;
        } else if (context.getLocalPath().equals("/livePlotterFlot")) {
            // AJAX request for data Lister
            template = loader.getResourceAsString("livePlotterFlot.html");
            AjaxView view = new AjaxView(template);
            view.addToContext("labels", labels);
            return view;
        } else {
            Channel channel;
            ArrayList<String> description = new ArrayList<String>();
            ArrayList<String> unit = new ArrayList<String>();

            for (int i = 0; i < labels.size(); i++) {
                channel = dataAccessService.getChannel(labels.get(i));
                description.add(channel.getDescription());
                unit.add(channel.getUnit().toString());
            }

            template = loader.getResourceAsString("index.html");
            PlotView view = new PlotView(template);
            view.addToContext("labels", labels);
            view.addToContext("description", description);
            view.addToContext("unit", unit);
            return view;
        }
    }

    /**
     * This function removes channels, whose units are not reasonable for Bar Plots.
     *
     * @param labels
     * @return list
     */
    private List<String> removeInapproriateBarPlotUnits(List<String> labels) {
        List<String> plottable = new ArrayList<String>();
        List<String> unplottable = Arrays.asList("°",
                                                 "°C",
                                                 "currency",
                                                 "m/s",
                                                 "m^3/h",
                                                 "m^3/d",
                                                 "m^3/s':m^3/m':kg/h':kg",
                                                 "N",
                                                 "Nm",
                                                 "Pa",
                                                 "bar",
                                                 "J/h",
                                                 "W",
                                                 "VA",
                                                 "var':A",
                                                 "C",
                                                 "V",
                                                 "V/m",
                                                 "F",
                                                 "Ohm",
                                                 "A/m",
                                                 "H",
                                                 "Hz",
                                                 "K",
                                                 "%'");
        for (String label : labels) {
            if (!unplottable.contains(dataAccessService.getChannel(label).getUnit())) {
                plottable.add(label);
            }
        }
        return plottable;
    }

    private String recordToJson(Record record) {
        return "{\"timestamp\":"
               + record.getTimestamp()
               + ",\"value\":"
               + record.getValue()
               + ",\"flag\":\""
               + record.getFlag()
               + "\"}";
    }

    private String recordsToJson(List<Record> records) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Record> it = records.iterator();

        stringBuilder.append('[');
        if (it.hasNext()) {
            Record record = it.next();
            stringBuilder.append("{\"timestamp\":"
                                 + record.getTimestamp()
                                 + ",\"value\":"
                                 + record.getValue()
                                 + ",\"flag\":\""
                                 + record.getFlag()
                                 + "\"}");
        }

        while (it.hasNext()) {
            Record record = it.next();
            stringBuilder.append(",{\"timestamp\":" + record.getTimestamp() + ",\"value\":" + record
                    .getValue()
                                 + ",\"flag\":\"" + record.getFlag() + "\"}");
        }
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    // TODO recordsToJson(List<Record> records) { does the same take the best...
    private String recordToJsonAdvanced(ArrayList<Record> recordList) {

        String response = "[";
        int counter = 0;
        for (Record record : recordList) {
            response += "{\"timestamp\":"
                        + record.getTimestamp()
                        + ",\"value\":"
                        + record.getValue()
                        + ",\"flag\":\""
                        + record.getFlag()
                        + "\"}";

            if (counter < recordList.size() - 1) {
                response += ",";
            }
            counter++;
        }
        response += "]";

        return response;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return true;
    }

    @Override
    public URL getResource(String name) {
        return context.getBundle().getResource(name);
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

    @Override
    public PluginCategory getCategory() {
        return PluginCategory.APPLICATION;
    }

}
