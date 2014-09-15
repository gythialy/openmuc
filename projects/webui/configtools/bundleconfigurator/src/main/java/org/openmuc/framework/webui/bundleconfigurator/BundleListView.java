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

package org.openmuc.framework.webui.bundleconfigurator;

import org.openmuc.framework.webui.spi.ResourceLoader;
import org.openmuc.framework.webui.spi.View;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.*;

public final class BundleListView implements View {

    private String template = null;
    private HashMap<String, Object> context = null;

    public BundleListView(BundleContext bc, ResourceLoader loader, String message) {
        template = loader.getResourceAsString("bundleview.html");
        context = new HashMap<String, Object>();
        List<BundleInfo> bundles = new ArrayList<BundleInfo>();

        for (Bundle bundle : bc.getBundles()) {
            bundles.add(new BundleInfo(bundle));
        }
        Collections.sort(bundles, new BundleInfoComparator());

        context.put("bundles", bundles);
        context.put("message", message);
    }

    private class BundleInfoComparator implements Comparator<BundleInfo> {

        @Override
        public int compare(BundleInfo o1, BundleInfo o2) {
            if (o1.getBundleId() > o2.getBundleId()) {
                return 1;
            }
            if (o1.getBundleId() < o2.getBundleId()) {
                return -1;
            }
            return 0;
        }
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
        styleSheets.add("/openmuc/css/openmuc-theme/jquery-ui-1.8.14.custom.css");
        return styleSheets;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getPage() {
        return "Installed bundles";
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
