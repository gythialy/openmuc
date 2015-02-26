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
//package org.openmuc.framework.server.asciisocket;
//
//import java.io.BufferedReader;
//import java.io.DataInputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintStream;
//import java.io.PrintWriter;
//import java.net.InetAddress;
//import java.net.Socket;
//import java.net.UnknownHostException;
//
///**
// *
// * Just to test the service.
// *
// * FIXME Replace by JUnit tests
// *
// * @author mzillgit
// *
// */
//public class SimpleProtocolClient {
//
//	public static void main(String[] args) {
//		Socket clientSock;
//		DataInputStream input;
//		PrintStream output;
//		PrintWriter out;
//		BufferedReader br;
//
//		try {
//			String str;
//
//			clientSock = new Socket("217.72.192.157", 25, InetAddress.getLocalHost(), 56032);
//
//			// clientSock = new Socket("217.72.192.157", 25);
//
//			clientSock.setSoTimeout(6000);
//			input = new DataInputStream(clientSock.getInputStream());
//			output = new PrintStream(clientSock.getOutputStream());
//
//			out = new PrintWriter(output, true);
//
//			br = new BufferedReader(new InputStreamReader(input));
//
//			out.println("!get TEMP_OUT");
//
//			str = br.readLine();
//
//			System.out.println(str);
//
//			clientSock.close();
//			System.out.println("Socket closed. Exit.");
//
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
// }
