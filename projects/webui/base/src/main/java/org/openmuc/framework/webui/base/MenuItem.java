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

package org.openmuc.framework.webui.base;

import java.util.ArrayList;
import java.util.List;

public final class MenuItem {

    private final String name;
    private final String reference;
    private String description;

    private boolean active = false;

    private final List<MenuItem> subItems = new ArrayList<MenuItem>();

    public MenuItem(String name, String reference) {
        this.name = name;
        this.reference = reference;
    }

    public MenuItem(String name, String reference, String description) {
        this.name = name;
        this.reference = reference;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        if (description != null) {
            return description;
        } else {
            return "";
        }
    }

    public List<MenuItem> getSubItems() {
        return subItems;
    }

    public void addSubItem(MenuItem item) {
        subItems.add(item);
    }

    public boolean hasSubItems() {
        if (subItems.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void setActive() {
        active = true;
    }

    public boolean isActive() {
        return active;
    }

    public String getCssClass() {
        if (subItems.size() == 0) {
            return "menuEndNode";
        } else {
            if (active) {
                return "menuActive";
            } else {
                return "menuClosed";
            }
        }
    }
}
