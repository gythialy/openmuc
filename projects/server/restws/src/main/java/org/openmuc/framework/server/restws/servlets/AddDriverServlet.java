package org.openmuc.framework.server.restws.servlets;

import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.ConfigWriteException;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.config.RootConfig;
import org.openmuc.framework.server.restws.Activator;
import org.openmuc.framework.server.restws.JsonHelper;
import org.openmuc.framework.server.restws.PathHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@SuppressWarnings("serial")
public class AddDriverServlet extends HttpServlet {
    private ConfigService configService;
    private RootConfig rootCfg;

    @Override
    public void init() throws ServletException {
        this.configService = Activator.getConfigService();
        this.rootCfg = configService.getConfig();
    }

    @Override
    public void destroy() {

    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.getWriter();

        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }

        InputStream in = request.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String line, text = "";
        try {
            while ((line = br.readLine()) != null) {
                text += line;
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        String value = JsonHelper.JsonToConfigValue(text);
        value = value.replace("\"", "");
        in.close();
        if (!request.getContentType().equals("application/json") || value == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (PathHandler.isValidRequest(pathInfo, queryStr)) {
            if (pathInfo.equals("/")) {
                try {
                    rootCfg.addDriver(value);
                    configService.setConfig(rootCfg);
                    configService.writeConfigToFile();
                }
                catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                catch (ConfigWriteException e) {
                    e.printStackTrace();
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }
}
