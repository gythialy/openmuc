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

public interface View {

    List<String> getStyleSheets();

    List<String> getJavaScripts();

    String getTemplate();

    HashMap<String, Object> getContext();

    String getPage();

    String getRedirectLocation();

    viewtype getViewType();

    public enum viewtype {
        PLUGIN,
        AJAX,
        REDIRECT,
        OTHERS;
    }
}
