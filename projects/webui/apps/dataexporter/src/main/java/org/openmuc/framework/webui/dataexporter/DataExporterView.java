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

package org.openmuc.framework.webui.dataexporter;

import org.openmuc.framework.webui.spi.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class DataExporterView implements View {

    private final String template;

    public DataExporterView(String template) {
        this.template = template;
    }

    private final HashMap<String, Object> context = new HashMap<String, Object>();

    public void addToContext(String key, Object object) {
        context.put(key, object);
    }

    @Override
    public HashMap<String, Object> getContext() {
        return context;
    }

    @Override
    public List<String> getJavaScripts() {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getStyleSheets() {
        ArrayList<String> styleSheets = new ArrayList<String>();
        styleSheets.add("css/style.css");
        return styleSheets;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getPage() {
        return "Data Export Tool";
    }

    @Override
    public String getRedirectLocation() {
        return null;
    }

    @Override
    public viewtype getViewType() {
        // TODO Auto-generated method stub
        return null;
    }
}
