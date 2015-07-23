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
package org.openmuc.framework.datalogger.ascii;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.exceptions.WrongCharacterException;
import org.openmuc.framework.datalogger.ascii.exceptions.WrongScalingException;
import org.openmuc.framework.datalogger.ascii.utils.Const;
import org.openmuc.framework.datalogger.ascii.utils.IESDataFormatUtils;
import org.openmuc.framework.datalogger.ascii.utils.LoggerUtils;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogFileWriter {

	private final String directoryPath;
	private static final Logger logger = LoggerFactory.getLogger(LogFileWriter.class);
	private final LogFileHeader header = new LogFileHeader();
	private File actualFile;

	public LogFileWriter() {
		directoryPath = Const.DEFAULT_DIR;
	}

	public LogFileWriter(String directoryPath) {

		this.directoryPath = directoryPath;
	}

	/**
	 * Main logger writing controller.
	 * 
	 * @param group
	 * @param loggingInterval
	 * @param date
	 * @param logChannelList
	 */
	public void log(LogIntervalContainerGroup group, int loggingInterval, int logTimeOffset, Date date,
			HashMap<String, LogChannel> logChannelList) {

		PrintStream out = getStream(group, loggingInterval, logTimeOffset, date, logChannelList);

		if (out == null) {
			return;
		}

		List<LogRecordContainer> logRecordContainer = group.getList();

		// TODO match column with container id, so that they don't get mixed up
		String logLine = setLoggingStringBuilder(logRecordContainer, logChannelList, date);

		out.print(logLine); // print because of println makes different newline char on different systems
		out.flush();
		out.close();
	}

	private String setLoggingStringBuilder(List<LogRecordContainer> logRecordContainer,
			HashMap<String, LogChannel> logChannelList, Date date) {

		StringBuilder sb = new StringBuilder();

		Calendar calendar = new GregorianCalendar(Locale.getDefault());
		calendar.setTime(date);
		LoggerUtils.setLoggerTimestamps(sb, calendar);

		for (int i = 0; i < logRecordContainer.size(); i++) {
			String value = "";
			int size = Const.VALUE_SIZE_MINIMAL;
			boolean left = true;

			if (logRecordContainer.get(i).getRecord() != null) {
				if (logRecordContainer.get(i).getRecord().getFlag() == Flag.VALID) {
					if (logRecordContainer.get(i).getRecord().getValue() == null) {
						// write error flag
						value = LoggerUtils.buildError(Flag.CANNOT_WRITE_NULL_VALUE);
						size = getDataTypeSize(logChannelList.get(logRecordContainer.get(i).getChannelId()), i);
					}
					else {
						ValueType valueType = logChannelList.get(logRecordContainer.get(i).getChannelId())
								.getValueType();
						// logger.debug("channel: " + containers.get(i).getChannelId());
						switch (valueType) {
						case BOOLEAN:
							value = String.valueOf(logRecordContainer.get(i).getRecord().getValue().asShort());
							break;
						case LONG:
							value = String.valueOf(logRecordContainer.get(i).getRecord().getValue().asLong());
							size = Const.VALUE_SIZE_LONG;
							break;
						case INTEGER:
							value = String.valueOf(logRecordContainer.get(i).getRecord().getValue().asInt());
							size = Const.VALUE_SIZE_INTEGER;
							break;
						case SHORT:
							value = String.valueOf(logRecordContainer.get(i).getRecord().getValue().asShort());
							size = Const.VALUE_SIZE_SHORT;
							break;
						case DOUBLE:
						case FLOAT:
							size = Const.VALUE_SIZE_DOUBLE;
							try {
								value = IESDataFormatUtils.convertDoubleToStringWithMaxLength(logRecordContainer.get(i)
										.getRecord().getValue().asDouble(), size);
							} catch (WrongScalingException e) {
								value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
								logger.error(e.getMessage() + " ChannelId: " + logRecordContainer.get(i).getChannelId());
							}
							break;
						case BYTE_ARRAY:
							left = false;
							size = checkMinimalValueSize(logChannelList.get(logRecordContainer.get(i).getChannelId())
									.getValueTypeLength());
							byte[] byteArray = logRecordContainer.get(i).getRecord().getValue().asByteArray();
							if (byteArray.length > size) {
								value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
								logger.error("The byte array is too big, length is " + byteArray.length
										+ " but max. length allowed is " + size + ", ChannelId: "
										+ logRecordContainer.get(i).getChannelId());
							}
							else {
								value = Const.HEXADECIMAL + LoggerUtils.byteArrayToHexString(byteArray);
							}
							break;
						case STRING:
							left = false;
							size = checkMinimalValueSize(logChannelList.get(logRecordContainer.get(i).getChannelId())
									.getValueTypeLength());
							value = logRecordContainer.get(i).getRecord().getValue().toString();
							try {
								checkStringValue(value);
							} catch (WrongCharacterException e) {
								value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
								logger.error(e.getMessage());
							}
							if (value.length() > size) {
								value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
								logger.error("The string is too big, length is " + value.length()
										+ " but max. length allowed is " + size + ", ChannelId: "
										+ logRecordContainer.get(i).getChannelId());
							}
							break;
						case BYTE:
							value = String.format("0x%02x", logRecordContainer.get(i).getRecord().getValue().asByte());
							break;
						default:
							throw new RuntimeException("unsupported valueType");
						}
					}
				}
				else {
					// write errorflag
					value = LoggerUtils.buildError(logRecordContainer.get(i).getRecord().getFlag());
					size = getDataTypeSize(logChannelList.get(logRecordContainer.get(i).getChannelId()), i);
				}
			}
			else {
				// got no data
				value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
				size = getDataTypeSize(logChannelList.get(logRecordContainer.get(i).getChannelId()), i);
			}

			if (left) {
				value = LoggerUtils.addSpacesLeft(value, size);
			}
			else {
				value = LoggerUtils.addSpacesRight(value, size);
			}
			sb.append(value);

			if (LoggerUtils.hasNext(logRecordContainer, i)) {
				sb.append(Const.SEPARATOR);
			}
		}
		sb.append(Const.LINESEPARATOR); // All systems with the same newline charter
		return sb.toString();
	}

	/**
	 * Checkes a string if it is IESData conform, f.e. wrong characters. If not it will drop a error.
	 * 
	 * @param value
	 *            the string value which should be checked
	 */
	private void checkStringValue(String value) throws WrongCharacterException {

		if (value.contains(Const.SEPARATOR)) {
			throw new WrongCharacterException("Wrong character: String contains Seperator character: "
					+ Const.SEPARATOR);
		}
		else if (value.startsWith(Const.ERROR)) {
			throw new WrongCharacterException("Wrong character: String begins with: " + Const.ERROR);
		}
		else if (value.startsWith(Const.HEXADECIMAL)) {
			throw new WrongCharacterException("Wrong character: String begins with: " + Const.HEXADECIMAL);
		}
		else if (!value.matches("^[\\x00-\\x7F]*")) {
			throw new WrongCharacterException("Wrong character: Non ASCII character in String.");
		}
	}

	private int checkMinimalValueSize(int size) {

		if (size < Const.VALUE_SIZE_MINIMAL) {
			size = Const.VALUE_SIZE_MINIMAL;
		}
		return size;
	}

	/**
	 * Returns the PrintStream for logging.
	 * 
	 * @param group
	 * @param loggingInterval
	 * @param date
	 * @param logChannelList
	 * @return the PrintStream for logging.
	 */
	private PrintStream getStream(LogIntervalContainerGroup group, int loggingInterval, int logTimeOffset, Date date,
			HashMap<String, LogChannel> logChannelList) {

		Calendar calendar = new GregorianCalendar(Locale.getDefault());
		calendar.setTime(date);
		String filename = LoggerUtils.buildFilename(loggingInterval, logTimeOffset, calendar);

		File file = new File(directoryPath + filename);
		actualFile = file;
		PrintStream out = null;

		try {
			if (file.exists()) {
				out = new PrintStream(new FileOutputStream(file, true), false, Const.CHAR_SET);
			}
			else {
				out = new PrintStream(new FileOutputStream(file, true), false, Const.CHAR_SET);
				String headerString = header.getIESDataFormatHeaderString(group, file.getName(), loggingInterval,
						logChannelList);

				out.print(headerString);
				out.flush();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return out;
	}

	/**
	 * Returns the size of a DataType / ValueType.
	 * 
	 * @param logChannel
	 * @param iterator
	 * @return size of DataType / ValueType.
	 */
	private int getDataTypeSize(LogChannel logChannel, int iterator) {

		int size = Const.VALUE_SIZE_MINIMAL;

		if (logChannel != null) {
			boolean isByteArray = logChannel.getValueType().equals(ValueType.BYTE_ARRAY);
			boolean isString = logChannel.getValueType().equals(ValueType.STRING);

			if ((isByteArray || isString)) {
				// get length from channel for ByteString
				size = logChannel.getValueTypeLength();
			}
			else if (!(isByteArray || isString)) {
				// get length from channel for simple value types
				size = LoggerUtils.getLengthOfValueType(logChannel.getValueType());
			}
		}
		else {
			// get length from file
			ValueType vt = LoggerUtils.identifyValueType(iterator + Const.NUM_OF_TIME_TYPES_IN_HEADER + 1, actualFile);
			size = LoggerUtils.getLengthOfValueType(vt);
			if ((vt.equals(ValueType.BYTE_ARRAY) || (vt.equals(ValueType.STRING))) && size <= Const.VALUE_SIZE_MINIMAL) {
				size = LoggerUtils.getValueTypeLengthFromFile(iterator + Const.NUM_OF_TIME_TYPES_IN_HEADER + 1,
						actualFile);
			}
		}
		return size;
	}
}
