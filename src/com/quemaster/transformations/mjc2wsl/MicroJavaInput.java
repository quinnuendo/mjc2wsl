package com.quemaster.transformations.mjc2wsl;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class MicroJavaInput {
	private HashMap<Integer, String> opMap;
	public String opCodeFile = "/mj-bytecodes.properties";
	private InputStream mainIn;
	int counter = -1;

	int mainAdr;

	int numberOfWords;
	private int codesize;

	public MicroJavaInput() {

	}

	public int get() {
		int res = -1;
		try {
			res = mainIn.read();
			if (res >= 0)
				res = res << 24 >>> 24;
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		counter++;
		return res;
	}

	public int get2() {
		return (get() * 256 + get()) << 16 >> 16;
	}

	public int get4() {
		return (get2() << 16) + (get2() << 16 >>> 16);
	}

	public void processHeader(mjc2wsl mjc2wsl) throws Exception {
		byte m = (byte) get();
		byte j = (byte) get();
		if (m != 'M' || j != 'J')
			throw new Exception("Wrong start of bytecode file");
		codesize = get4();
		setNumberOfWords(get4());
		setMainAdr(get4());
	}

	public void setStream(InputStream ins) {
		mainIn = ins;
	}

	public int getCounter() {
		return counter;
	}

	public int getCodesize() {
		return codesize;
	}

	public int getMainAdr(mjc2wsl mjc2wsl) {
		return mainAdr;
	}

	public int getNumberOfWords(mjc2wsl mjc2wsl) {
		return numberOfWords;
	}

	void setNumberOfWords(int numberOfWords) {
		this.numberOfWords = numberOfWords;
	}

	void setMainAdr(int mainAdr) {
		this.mainAdr = mainAdr;
	}

	String getRelationFor(int opcode) throws Exception {
		switch (opcode) {
		case mjc2wsl.jeq:
			return "=";
		case mjc2wsl.jne:
			return "<>";
		case mjc2wsl.jlt:
			return "<";
		case mjc2wsl.jle:
			return "<=";
		case mjc2wsl.jgt:
			return ">";
		case mjc2wsl.jge:
			return ">=";
		}
		throw new Exception("Wrong opcode for a relation");
	}

	boolean isJumpCode(int opcode) {
		return (opcode >= mjc2wsl.jmp) && (opcode <= mjc2wsl.jge);
	}

	private HashMap<Integer, String> getOpMap() {
		if (opMap == null) {
			opMap = new HashMap<Integer, String>(60, 0.98f);
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream(opCodeFile)));
				String str = in.readLine();
				while (str != null) {
					String[] ss = str.split("=");
					opMap.put(Integer.parseInt(ss[0]), ss[1]);
					str = in.readLine();
				}
				in.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return opMap;
	}

	public String getOpString(int op) {
		return getOpMap().get(op);
	}

	public String describeOpCode(int op) {
		return op + " (" + getOpString(op) + ")";
	}

}