/*
 * Copyright 2011-16 Fraunhofer ISE
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

package org.openmuc.framework.webui.spi;

import java.util.HashMap;
import java.util.List;

public final class MessageView implements View {

    public static final int INFO = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;

    private String page = "";
    private final String message;

    public MessageView(String page, String message, int type) {
        this.page = page;
        this.message = message;
    }

    public MessageView(String message, int type) {
        this.message = message;
    }

    @Override
    public List<String> getStyleSheets() {
        return null;
    }

    @Override
    public List<String> getJavaScripts() {
        return null;
    }

    @Override
    public String getTemplate() {
        return "<div id=\"message\"><p>" + message + "</p></div>";
    }

    @Override
    public HashMap<String, Object> getContext() {
        return new HashMap<>();
    }

    @Override
    public String getPage() {
        return page;
    }

    @Override
    public String getRedirectLocation() {
        return null;
    }

    @Override
    public viewtype getViewType() {
        return null;
    }

}
