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
package org.openmuc.framework.webui.channelconfigurator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Frederic Robra
 */
public class Content {

    public static Content createMessage(String title, String message) {
        Content content = new Content();
        content.title = title;
        content.html = message;
        return content;
    }

    public static Content createErrorMessage(Exception e, String message) {
        StringBuilder html = new StringBuilder();

        if (message != null) {
            html.append("<p>").append(message).append("</p>");
        }
        html.append("<p>Error processing request.</p>");
        html.append(e.getMessage());
        html.append("<hr><p><a id=expand href=# style=\"");
        html.append("background: url(/openmuc/images/arrow_right_small.png) no-repeat left; padding-left: 20px");
        html.append("\">Stack Trace</a></p>");
        html.append("<div id=expander style=\"display: none; overflow: scroll; white-space: nowrap\">");
        html.append(e.toString()).append(": ").append(e.getMessage()).append("<br>");
        html.append("#set( $bo = \"(\")").append("#set( $bc = \")\")");
        html.append("<div style=\"padding-left: 3em\">");
        for (StackTraceElement element : e.getStackTrace()) {
            html.append("at ").append(element.getClassName()).append(".").append(element.getMethodName());
            html.append("${bo}").append(element.getFileName()).append(':').append(element.getLineNumber());
            html.append("${bc}<br>");
        }
        html.append("</div></div>");
        html.append("<script type=\"text/javascript\">");
        html.append("$(\"#expand\").click(function(event) {");
        html.append("  event.preventDefault();");
        html.append("  if($(\"#expander\").is(\":visible\")) {");
        html.append("    $(\"#expand\").css(\"background-image\", \"url(/openmuc/images/arrow_right_small.png)\");");
        html.append("  }");
        html.append("  else {");
        html.append("    $(\"#expand\").css(\"background-image\", \"url(/openmuc/images/arrow_down_small.png)\");");
        html.append("  }");
        html.append("  $(\"#expander\").slideToggle(500, function() { });");
        html.append("});");
        html.append("</script>");

        Content content = new Content();
        content.title = "Error";
        content.html = html.toString();
        return content;
    }

    public static Content createRedirect(String redirect) {
        Content content = new Content();
        content.redirect = redirect;
        return content;
    }

    public static Content createAjax(String ajax) {
        Content content = new Content();
        content.ajax = true;
        content.html = ajax;
        return content;
    }

    private String title = null;
    private String html = null;
    private MenuItem menuItem = MenuItem.NONE;
    private final Map<String, Object> context = new HashMap<String, Object>();
    private boolean ajax = false;
    private String redirect = null;

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getHtml() {
        return html;
    }

    public void addToContext(String key, Object value) {
        context.put(key, value);
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public boolean isAjax() {
        return ajax;
    }

    public boolean isRedirect() {
        return redirect == null ? false : true;
    }

    public String getRedirect() {
        return redirect;
    }

}
