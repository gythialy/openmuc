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
package tuwien.auto.calimero.serial.rc1180;

import java.util.Arrays;

import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * @author Frederic Robra
 * 
 */
public class SNorDoA {

	private final boolean AET;
	private final byte[] number;

	public SNorDoA(boolean AET, byte[] number) {
		if (number.length != 6) {
			throw new KNXIllegalArgumentException("wrong length of SN or DoA");
		}
		this.AET = AET;
		this.number = number;
	}

	public boolean isSerialNumber() {
		return !AET;
	}

	public boolean isDomainAddress() {
		return AET;
	}

	public byte[] get() {
		return Arrays.copyOf(number, number.length);
	}
}
