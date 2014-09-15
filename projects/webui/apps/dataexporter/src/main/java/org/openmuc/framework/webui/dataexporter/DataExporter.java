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

package org.openmuc.framework.webui.dataexporter;

import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.webui.spi.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Calendar;
import java.util.Hashtable;

import static java.util.Calendar.*;

public final class DataExporter implements WebUiPluginService {

    private final static Logger logger = LoggerFactory.getLogger(DataExporter.class);

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

        return resources;
    }

    @Override
    public String getAlias() {
        return "dataexporter";
    }

    @Override
    public String getName() {
        return "Data Exporter";
    }

    @Override
    public String getDescription() {
        return "A tool to export data as CSV table.";
    }

    @Override
    public View getContentView(HttpServletRequest request, PluginContext context) {
        String template = "";

        if (context.getLocalPath().equals("/exportdata")) {
            return handleExportForm(context);
        } else {

            if (dataAccessService == null) {
                return new MessageView("Data currently not available!", MessageView.ERROR);
            }

            template = loader.getResourceAsString("exportform.html");
            DataExporterView view = new DataExporterView(template);

            Calendar now = Calendar.getInstance();

            view.addToContext("startDaySelected", new Integer(now.get(DAY_OF_MONTH)));
            view.addToContext("startMonthSelected", new Integer(now.get(MONTH) + 1));
            view.addToContext("startYearSelected", new Integer(now.get(YEAR)));
            view.addToContext("labellist", dataAccessService.getAllIds());

            return view;
        }
    }

    private View handleExportForm(PluginContext context) {
        HttpServletRequest request = context.getRequest();

        String[] labels = request.getParameterValues("labellist");

        if (labels == null) {
            return new MessageView("Please select at least on channel!", MessageView.ERROR);
        }

        try {
            int startDay = new Integer(request.getParameter("startdateday"));
            int startMonth = new Integer(request.getParameter("startdatemonth"));
            int startYear = new Integer(request.getParameter("startdateyear"));

            int dateFormat = new Integer(request.getParameter("timeformat"));

            long end;

            if (request.getParameter("enddate") != null) {
                int endDay = new Integer(request.getParameter("enddateday"));
                int endMonth = new Integer(request.getParameter("enddatemonth"));
                int endYear = new Integer(request.getParameter("enddateyear"));

                Calendar cal = Calendar.getInstance();
                cal.clear();

                cal.set(endYear, endMonth - 1, endDay);
                cal.add(DAY_OF_MONTH, 1);

                end = cal.getTimeInMillis();
            } else {
                end = System.currentTimeMillis();
            }

            Calendar cal = Calendar.getInstance();

            cal.clear();
            cal.set(startYear, startMonth - 1, startDay);
            long start = cal.getTimeInMillis();

            CsvDataExport exporter = new CsvDataExport(dataAccessService);

            HttpServletResponse response = context.getResponse();

            PrintWriter pw;
            try {
                pw = response.getWriter();
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment;filename=exporteddata.csv");

                exporter.exportData(labels, pw, start, end, dateFormat);
                pw.flush();

                return new MessageView("Data exported.", MessageView.INFO);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            return new MessageView("Invalid date format!", MessageView.ERROR);
        }

        return new MessageView("Function not available in evaluation version!",
                               MessageView.WARNING);
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
