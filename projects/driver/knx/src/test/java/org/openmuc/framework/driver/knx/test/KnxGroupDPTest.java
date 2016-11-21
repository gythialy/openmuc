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
package org.openmuc.framework.driver.knx.test;

/**
 * @author Frederic Robra
 * 
 */
public class KnxGroupDPTest {

    /*
     * @Test public void test() throws KNXException {
     * 
     * @SuppressWarnings("unchecked") Map<Integer, MainType> mainTypes = TranslatorTypes.getAllMainTypes(); KnxGroupDP
     * dp = null; GroupAddress main = new GroupAddress(new byte[] { 0x08, 0x00 }); for (Map.Entry<Integer, MainType>
     * mainType : mainTypes.entrySet()) {
     * 
     * @SuppressWarnings("unchecked") Map<String, DPT> subTypes = mainType.getValue().getSubTypes();
     * 
     * for (Map.Entry<String, DPT> subType : subTypes.entrySet()) { DPT dpt = subType.getValue(); System.out.println(
     * "testing: " + dpt.toString()); // try { dp = new KnxGroupDP(main, dpt.getDescription(), dpt.getID()); // } catch
     * (KNXException e) { // // fail("could not create KnxGroupDP with: " + dpt.toString()); // } try {
     * dp.getKnxValue().setDPTValue(dpt.getLowerValue()); dp.getKnxValue().setDPTValue(dpt.getUpperValue()); } catch
     * (KNXFormatException e) { fail("could not set upper and lower value to KnxGroupDP: " + dpt.toString()); }
     * assertTrue(dp.getKnxValue().getOpenMucValue() instanceof Value); } } }
     */

}
