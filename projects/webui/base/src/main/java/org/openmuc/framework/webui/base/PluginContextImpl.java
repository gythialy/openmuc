/*
 * Copyright 2011-15 Fraunhofer ISE
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

package org.openmuc.framework.webui.base;

import org.openmuc.framework.webui.spi.PluginCategory;
import org.openmuc.framework.webui.spi.PluginContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class PluginContextImpl implements PluginContext {

    private String applicationPath;
    private String applicationAlias;
    private String localPath;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public PluginContextImpl(String pathInfo, HttpServletRequest request, HttpServletResponse response) {
        extractPathInfoParts(pathInfo);
        this.request = request;
        this.response = response;
    }

    @Override
    public String getApplicationPath() {
        return applicationPath;
    }

    @Override
    public String getLocalPath() {
        return localPath;
    }

    @Override
    public String getApplicationAlias() {
        return applicationAlias;
    }

    private void extractPathInfoParts(String pathInfo) {
        String categoryPrefixes[] = {"/" + PluginCategory.APPLICATION.toString() + "/", "/" + PluginCategory.CONFIGTOOL
                .toString() + "/", "/" + PluginCategory.DRIVERTOOL.toString() + "/"};

        for (String categoryPrefix : categoryPrefixes) {
            if (pathInfo.startsWith(categoryPrefix)) {
                String applicationPath = pathInfo.replaceFirst(categoryPrefix, "");

                int slashIndex = applicationPath.indexOf('/');

                if (slashIndex != -1) {
                    applicationAlias = applicationPath.substring(0, slashIndex);
                    localPath = applicationPath.substring(slashIndex);
                } else {
                    applicationAlias = applicationPath;
                    localPath = "";
                }

                this.applicationPath = "/openmuc" + categoryPrefix + applicationAlias;
                return;
            }
        }

        applicationAlias = "";
        localPath = "";
        applicationPath = "";
    }

    @Override
    public HttpServletResponse getResponse() {
        return response;
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }
}
