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
package org.openmuc.framework.demo.restclient;

public class Option {
    long id;
    char optionName;
    String optionFullName;
    String description;
    int nbrOfParameters;

    public Option(char optName, String optFName, String des) {
        this.id = 0;
        this.optionName = optName;
        this.optionFullName = optFName;
        this.description = des;
        this.nbrOfParameters = 0;
    }

    @Override
    public String toString() {
        return "short name: "
               + this.optionName
               + " full name: "
               + this.optionFullName
               + " id: "
               + this.id;
    }

    public Boolean equals(Option opt) {
        if (this.optionName == opt.getName()) {
            return true;
        }
        if (this.optionFullName.equals(opt.getFullName())) {
            return true;
        }
        return false;
    }

    public char getName() {
        return this.optionName;
    }

    public String getFullName() {
        return this.optionFullName;
    }

    public void setNbrOfParamaters(int nbr) {
        this.nbrOfParameters = nbr;
    }

    public void setID(long i) {
        this.id = i;
    }

    public long getID() {

        return this.id;
    }

    public int getParameterNbr() {
        return this.nbrOfParameters;
    }

    public String getDescription() {
        return this.description;
    }

}
