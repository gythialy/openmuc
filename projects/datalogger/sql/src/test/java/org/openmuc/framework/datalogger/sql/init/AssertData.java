/*
 * Copyright 2011-2022 Fraunhofer ISE
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

package org.openmuc.framework.datalogger.sql.init;

import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BOOLEAN_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BYTE_ARRAY_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BYTE_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.DOUBLE_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.FLOAT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.INT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.LONG_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.SHORT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.STRING_VALUE;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssertData {

    public static List<String> getOpenmucTableConstraints() {
        List<String> tableConstrains = new ArrayList<>();

        ArrayList<String> tableNameList = new ArrayList<>();
        Collections.addAll(tableNameList, BOOLEAN_VALUE, BYTE_ARRAY_VALUE, FLOAT_VALUE, DOUBLE_VALUE, INT_VALUE,
                LONG_VALUE, BYTE_VALUE, SHORT_VALUE, STRING_VALUE);
        ArrayList<JDBCType> typeList = new ArrayList<>();
        Collections.addAll(typeList, JDBCType.BOOLEAN, JDBCType.LONGVARBINARY, JDBCType.FLOAT, JDBCType.DOUBLE,
                JDBCType.INTEGER, JDBCType.BIGINT, JDBCType.SMALLINT, JDBCType.SMALLINT, JDBCType.VARCHAR);

        for (int i = 0; i < 9; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS ").append(tableNameList.get(i));

            sb.append("(time TIMESTAMP NOT NULL,\n");

            sb.append("channelID VARCHAR(40) NOT NULL,")
                    .append("flag ")
                    .append(JDBCType.SMALLINT)
                    .append(" NOT NULL,")
                    .append("value ");

            sb.append(typeList.get(i));

            if (i == 8) {
                sb.append(" (100)");
            }
            sb.append(",INDEX ").append(tableNameList.get(i)).append("Index(time)");
            sb.append(",PRIMARY KEY (channelid, time));");

            tableConstrains.add(sb.toString());
        }
        return tableConstrains;
    }

}
