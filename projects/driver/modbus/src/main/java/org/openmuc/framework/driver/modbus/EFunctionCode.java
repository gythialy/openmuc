/*
 * Copyright 2011-18 Fraunhofer ISE
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

package org.openmuc.framework.driver.modbus;

/**
 * Supported modbuss function codes
 */
public enum EFunctionCode {
    FC_01_READ_COILS, //
    FC_02_READ_DISCRETE_INPUTS, //
    FC_03_READ_HOLDING_REGISTERS, //
    FC_04_READ_INPUT_REGISTERS, //
    FC_05_WRITE_SINGLE_COIL, //
    FC_06_WRITE_SINGLE_REGISTER, //
    FC_15_WRITE_MULITPLE_COILS, //
    FC_16_WRITE_MULTIPLE_REGISTERS //
}
