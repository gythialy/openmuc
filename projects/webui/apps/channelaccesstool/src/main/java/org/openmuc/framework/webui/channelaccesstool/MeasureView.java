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

package org.openmuc.framework.webui.channelaccesstool;

import org.openmuc.framework.webui.spi.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MeasureView implements View {

    private final String template;

    public MeasureView(String template) {
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
        ArrayList<String> javaScripts = new ArrayList<String>();
        javaScripts.add("js/script.js");
        return javaScripts;
    }

    @Override
    public List<String> getStyleSheets() {
        ArrayList<String> styleSheets = new ArrayList<String>();
        styleSheets.add("/openmuc/css/openmuc-theme/jquery-ui-1.8.14.custom.css");
        styleSheets.add("css/style.css");
        return styleSheets;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getPage() {
        return "Measurement";
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
