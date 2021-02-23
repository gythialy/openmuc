/*
 * Copyright 2011-2021 Fraunhofer ISE
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

package org.openmuc.framework.driver.ehz.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.junit.jupiter.api.Test;
import org.openmuc.jsml.transport.MessageExtractor;

public class EhzDriverTest {

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Test
    public void testMessage() throws ParseException, IOException {
        byte[] message1 = hexStringToByteArray(
                "1B1B1B1B0101010176090000000017849D826201620072630101760101090000000007D6DF2F0B064841470104C537527B010163AAD90076090000000017849D83620162007263070177010B064841470104C537527B070100620AFFFF7262016509A29F6EF17977078181C78203FF01010101044841470177070100000009FF010101010B064841470104C537527B0177070100010800FF628201621E52FF5512D8BD2D0177070100010801FF0101621E52FF5512D8961D0177070100010802FF0101621E52FF5327100177070100020800FF628201621E52FF551B2BBCCC0177070100020801FF0101621E52FF551B2B95BC0177070100020802FF0101621E52FF53271001770701001007BFFF1B1F6F7FDFBF7323B9BFF7070100240700FF0101621B52FF53134101770701001F0700FF0101622152FE5301000177070100200700FF0101622352FE5357BD0177070100380700FF0101621B52FF5305EA0177070100330700FF0101622152FE5300630177070100340700FF0101622352FE53597701770701004C0700FF0101621B52FF5300760177070100470700FF0101622152FE5300310177070100480700FF0101622352FE53577E0177070100603200020101620952FF53012D0177078181C78205FF010101018302BF093F6DE4D2CA3E3C2C04CCB9CBFD5084E67C5192CDD4FF4929E5D6FFDFFFDDF73E160F9070AFD0FF0BE2FFC5834EAC01770701006032030301016223520062680177070100607B47A6FB4D6FFB5F4F62FB017707010060320003010162095200520A01770701006032000401016209520052280177070100603200050101620952005206010101638F260076090000000017849D8762016200726302017101636B7F0000001B1B1B1B1A02FB20");

        InputStream targetStream = new ByteArrayInputStream(message1);
        DataInputStream dis = new DataInputStream(targetStream);
        MessageExtractor me = new MessageExtractor(dis, 10000);
    }
}
