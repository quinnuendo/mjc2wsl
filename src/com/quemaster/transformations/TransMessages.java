package com.quemaster.transformations;
/*
    Copyright (C) 2014  Doni Pracner
 
    This file is part of mjc2wsl.

    mjc2wsl is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    mjc2wsl is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with mjc2wsl.  If not, see <http://www.gnu.org/licenses/>.
*/
import java.io.PrintStream;

/**
 * Handle the messages that the translator (or someone else) will
 * give to this class depending on the print level.
 *
 */
public class TransMessages {
	private int printLevel;
	private int[] messageCounters;
	public static final int M_DEB = 0;
	public static final int M_WAR = 1;
	public static final int M_ERR = 2;
	public static final int M_QUIET = 3;
	private PrintStream outStream = System.out;

	public TransMessages() {
		this.setPrintLevel(M_ERR);
		this.messageCounters = new int[TransMessages.M_QUIET];
	}

	public void message(String mes, int level){
			if (level>=getPrintLevel()) {
				outStream.println(mes);
			}
			messageCounters[level]++;
	}

	public int getLevelMessageCount(int level){
		if (level < messageCounters.length){
			return messageCounters[level];
		}
		return 0;
	}

	public void printMessageCounters(PrintStream out){
			out.println("total errors:"+messageCounters[TransMessages.M_ERR]+" warnings:"+messageCounters[TransMessages.M_WAR]);
	}

	public void printMessageCounters(){
			printMessageCounters(outStream);
	}

	public int getPrintLevel() {
		return printLevel;
	}

	public void setPrintLevel(int printLevel) {
		this.printLevel = printLevel;
	}
}