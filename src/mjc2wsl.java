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
import java.io.*;
import java.util.*;

/**
 * This program converts file from compiled MicroJava bytecode to WSL language
 * which is a part of the FermaT Transformation system. MicroJava is a subset
 * used in Compiler Construction courses by Hanspeter Moessenboeck, not
 * "Java ME". 
 * 
 * @author Doni Pracner, http://perun.dmi.rs/pracner http://quemaster.com
 */
public class mjc2wsl{
	public static String versionN = "0.1.5";

	private TransMessages messages = new TransMessages();

	private boolean genPauseAfterEachAddress=false, 
		genPrintForEachAddress = false,
		genPrintEStackOnChange = false;
	
	/** Constant used for marking a regular comment from the original file */
	public static final char C_REG = ' ';
	/**
	 * Constant used for marking when original code is inserted in the file,
	 * next to the translations
	 */
	public static final char C_OC = '#';
	/** Constant used for marking special messages from the translator */
	public static final char C_SPEC = '&';
	/** Constant used for marking error messages from the translator */
	public static final char C_ERR = '!';

	/** instruction code in MicroJava bytecode. */
	public static final int 
		load        =  1,
		load_0      =  2,
		load_1      =  3,
		load_2      =  4,
		load_3      =  5,
		store       =  6,
		store_0     =  7,
		store_1     =  8,
		store_2     =  9,
		store_3     = 10,
		getstatic   = 11,
		putstatic   = 12,
		getfield    = 13,
		putfield    = 14,
		const_0     = 15,
		const_1     = 16,
		const_2     = 17,
		const_3     = 18,
		const_4     = 19,
		const_5     = 20,
		const_m1    = 21,
		const_      = 22,
		add         = 23,
		sub         = 24,
		mul         = 25,
		div         = 26,
		rem         = 27,
		neg         = 28,
		shl         = 29,
		shr         = 30,
		inc         = 31,
		new_        = 32,
		newarray    = 33,
		aload       = 34,
		astore      = 35,
		baload      = 36,
		bastore     = 37,
		arraylength = 38,
		pop         = 39,
		dup         = 40,
		dup2        = 41,
		jmp         = 42,
		jeq         = 43,
		jne         = 44,
		jlt         = 45,
		jle         = 46,
		jgt         = 47,
		jge         = 48,
		call        = 49,
		return_     = 50,
		enter       = 51,
		exit        = 52,
		read        = 53,
		print       = 54,
		bread       = 55,
		bprint      = 56,
		trap		= 57;

	private boolean originalInComments = false;	
	
	private HashMap<Integer,String> opMap = null;
	
	private String opCodeFile = "mj-bytecodes.properties";
	
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
	
	private InputStream mainIn;
	private PrintWriter out = null;
	private int counter = -1;
	
	private void pr(int i){
			out.print(i);
	}
	
	private void pr(char i){
			out.print(i);
	}
	
	private void pr(String i){
			out.print(i);
	}
	
	private void prl(String i){
			out.println(i);
	}
	
	private int get() {
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
	
	private int get2() {
		return (get() * 256 + get()) << 16 >> 16;
	}

	private int get4() {
		return (get2() << 16) + (get2() << 16 >>> 16);
	}
	
	public String createStandardStart(){
			return createStandardStart(10);
	}

	public String createStandardStart(int numWords){
		StringBuilder ret = new StringBuilder(
			"C:\" This file automatically converted from microjava bytecode\";\n"
			+"C:\" with mjc2wsl v "+versionN+"\";\n");
	
		ret.append("BEGIN ");
		ret.append("VAR < tempa := 0, tempb := 0, tempres :=0,\n\t");
		ret.append("mjvm_locals := ARRAY(1,0), ");
		ret.append("\n\tmjvm_statics := ARRAY("+numWords+",0), ");
		ret.append("\n\tmjvm_arrays := < >, ");
		ret.append("\n\tmjvm_objects := < >, ");
		ret.append("\n	mjvm_estack := < >, mjvm_mstack := < > > : "); 
	
		return ret.toString();
	}

	public String createStandardEnd(){
		StringBuilder ret = new StringBuilder("SKIP\nENDVAR");
		ret.append("\nWHERE\n");
		
		ret.append("\nPROC Print_MJ(val, format VAR)==\n");
		ret.append(createComment("print spacing", C_SPEC));
				
		ret.append("\n\tIF format>1 THEN\n\t\tFOR i:=2 TO ");
		ret.append("format STEP 1 DO PRINFLUSH(\" \") OD\n");
		ret.append("\tFI;\n\tPRINFLUSH(val)\nEND\n");
		
		ret.append("\nEND\n");
		
		return ret.toString();
	}

	private String createLocal(int i) {
		// arrays start at 1 in WSL, so we need an offset
		return "mjvm_locals[" + (i + 1) + "]";
	}

	private String createStatic(int i) {
		return "mjvm_statics[" + (i + 1) + "]";
	}

	private String createArray(int i) {
		return "mjvm_arrays[" + i + "]";
	}

	private String createArray(String i) {
		return "mjvm_arrays[" + i + "]";
	}

	private String createObject(String i) {
		return "mjvm_objects[" + i + "]";
	}
	
	/**
	 * Creates a WSL comment with care to quote chars.
	 */
	public static String createComment(String str){
		return createComment(str, C_REG);
	}
	
	/**
	 * Creates a WSL comment with care to quote chars, of the
	 * given type.  Types are given as char constants. They can be
	 * default comments, comments that contain the original code
	 * in them, or additional comments regarding the translation
	 * process.
	 */
	public static String createComment(String str, char type) {
		return "C:\"" + type + str.replace("\"", "''") + "\";";
	}

	//Expression stack
	
	private String createToEStack(int i) {
		String res = "mjvm_estack := <" + i + " > ++ mjvm_estack;";
		if (genPrintEStackOnChange)
			res += "PRINT(\"eStack\",mjvm_estack);";
		return res;
	}

	private String createToEStack(String i) {
		String res = "mjvm_estack := <" + i + " > ++ mjvm_estack;";
		if (genPrintEStackOnChange)
			res += "PRINT(\"eStack\",mjvm_estack);";
		return res;
	}

	private String createFromEStack(String st) {
		String res = st
				+ " := HEAD(mjvm_estack); mjvm_estack := TAIL(mjvm_estack);";
		if (genPrintEStackOnChange)
			res += "PRINT(\"eStack\",mjvm_estack);";
		return res;
	}

	private String createPopEStack() {
		String res = "mjvm_estack := TAIL(mjvm_estack);";
		if (genPrintEStackOnChange)
			res += "PRINT(\"eStack\",mjvm_estack);";
		return res;
	}

	private String createTopTwoEStack() {
		return createFromEStack("tempa") + "\n" + createFromEStack("tempb");
	}

	private String createTopEStack() {
		return createFromEStack("tempa");
	}
	
	//Method stack

	private String createToMStack(int i) {
		return "mjvm_mstack := <" + i + " > ++ mjvm_mstack;";
	}

	private String createToMStack(String i) {
		return "mjvm_mstack := <" + i + " > ++ mjvm_mstack;";
	}

	private String createFromMStack(String st) {
		return st + " := HEAD(mjvm_mstack); mjvm_mstack := TAIL(mjvm_mstack);";
	}

	private String getRelationFor(int opcode) throws Exception {
		switch (opcode) {
				case jeq: return "=";
				case jne: return "<>";
				case jlt: return "<";
				case jle: return "<=";
				case jgt: return ">";
				case jge: return ">=";
		}
		throw new Exception("Wrong opcode for a relation");
	}

	private boolean isJumpCode(int opcode) {
		return (opcode >= jmp) && (opcode <= jge);
	}

	public void convertStream(InputStream ins) throws Exception{
		mainIn = ins;
		//process start 
		byte m = (byte) get();
		byte j = (byte) get();
		if (m!='M' || j !='J') 
				throw new Exception("Wrong start of bytecode file");
		int codesize = get4();
		int numberOfWords = get4();
		int mainAdr = get4();
		
		prl(createStandardStart(numberOfWords));
		prl("SKIP;\n ACTIONS A_S_start:\n A_S_start == CALL a" + (14 + mainAdr)
				+ " END");
		int op = get();
		while (op >= 0) {
			if (originalInComments)
				prl(createComment(describeOpCode(op), C_OC));
			prl("a" + counter + " == ");
			if (genPrintForEachAddress) {
				prl("PRINT(\"a" + counter + "\");");
				if (genPauseAfterEachAddress)
					prl("debug_disposable_string := @Read_Line(Standard_Input_Port);");
			}
			switch (op) {
			case load: {
				prl(createToEStack(createLocal(get())));
				break;
			}
			case load_0:
			case load_1:
			case load_2:
			case load_3: {
				prl(createToEStack(createLocal(op - load_0)));
				break;
			}
			case store: {
				prl(createFromEStack(createLocal(get())));
				break;
			}
			case store_0:
			case store_1:
			case store_2:
			case store_3: {
				prl(createFromEStack(createLocal(op - store_0)));
				break;
			}

			case getstatic: {
				prl(createToEStack(createStatic(get2())));
				break;
			}
			case putstatic: {
				prl(createFromEStack(createStatic(get2())));
				break;
			}

			case getfield: {
				int f = get2();
				prl(createTopEStack());
				prl(createToEStack(createObject("tempa") + "[" + (f + 1) + "]"));
				break;
			}
			case putfield: {
				int f = get2();
				// we need to use a temparray as a pointer, WSL
				// otherwise tries to access it as a list of lists and fails
				prl(createTopTwoEStack());
				prl("VAR < tempArray := " + createObject("tempb") + " > :");
				prl("tempArray[" + (f + 1) + "]:=tempa ENDVAR;");
				break;
			}

			case const_: {
				prl(createToEStack(get4()));
				break;
			}

			case const_0:
			case const_1:
			case const_2:
			case const_3:
			case const_4:
			case const_5: {
				prl(createToEStack(op - const_0));
				break;
			}

			case add: {
				prl(createTopTwoEStack());
				prl("tempres := tempb + tempa;");
				prl(createToEStack("tempres"));
				break;
			}
			case sub: {
				prl(createTopTwoEStack());
				prl("tempres := tempb - tempa;");
				prl(createToEStack("tempres"));
				break;
			}
			case mul: {
				prl(createTopTwoEStack());
				prl("tempres := tempb * tempa;");
				prl(createToEStack("tempres"));
				break;
			}
			case div: {
				prl(createTopTwoEStack());
				prl("IF tempa = 0 THEN ERROR(\"division by zero\") FI;");
				prl("tempres := tempb DIV tempa;");
				prl(createToEStack("tempres"));
				break;
			}
			case rem: {
				prl(createTopTwoEStack());
				prl("IF tempa = 0 THEN ERROR(\"division by zero\") FI;");
				prl("tempres := tempb MOD tempa;");
				prl(createToEStack("tempres"));
				break;
			}

			case neg: {
				prl(createTopEStack());
				prl(createToEStack("-tempa"));
				break;
			}

			case shl: {
				prl(createTopTwoEStack());
				prl("VAR <tempres :=tempb, i:=1 >:");
				prl("\tFOR i:=1 TO tempa STEP 1 DO tempres := tempres * 2 OD;");
				prl(createToEStack("tempres"));
				prl("ENDVAR;");
				break;
			}
			case shr: {
				prl(createTopTwoEStack());
				prl("VAR <tempres :=tempb, i:=1 >:");
				prl("\tFOR i:=1 TO tempa STEP 1 DO tempres := tempres DIV 2 OD;");
				prl(createToEStack("tempres"));
				prl("ENDVAR;");
				break;
			}

			case inc: {
				int b1 = get(), b2 = get();
				prl(createLocal(b1) + " := " + createLocal(b1) + " + " + b2 + ";");
				break;
			}

			case new_: {
				int size = get2();
				// TODO maybe objects and arrays should be in the same list?
				prl("mjvm_objects := mjvm_objects ++ < ARRAY(" + size
						+ ",0) >;");
				prl(createToEStack("LENGTH(mjvm_objects)"));
				break;
			}
			case newarray: {
				get();// 0 - bytes, 1 - words; ignore for now
				// TODO take into consideration 0/1
				prl(createTopEStack());
				prl("mjvm_arrays := mjvm_arrays ++ < ARRAY(tempa,0) >;");
				prl(createToEStack("LENGTH(mjvm_arrays)"));
				break;
			}

			case aload:
			case baload: {
				prl(createTopTwoEStack());
				prl(createToEStack(createArray("tempb") + "[tempa+1]"));
				break;
			}
			case astore:
			case bastore: {
				prl(createFromEStack("tempres"));
				prl(createTopTwoEStack());
				// we need to use a temparray as a pointer, WSL
				// otherwise tries to access it as a list of lists and fails
				prl("VAR < tempArray := " + createArray("tempb") + " > :");
				prl("tempArray[tempa+1]:=tempres ENDVAR;");
				break;
			}
			case arraylength: {
				prl(createTopEStack());
				prl("tempb := LENGTH("+ createArray("tempa") + ");");
				prl(createToEStack("tempb"));
				break;
			}

			case dup: {
				prl(createTopEStack());
				prl(createToEStack("tempa"));
				prl(createToEStack("tempa"));
				break;
			}
			case dup2: {
				prl(createTopTwoEStack());
				prl(createToEStack("tempb"));
				prl(createToEStack("tempa"));
				prl(createToEStack("tempb"));
				prl(createToEStack("tempa"));
				break;
			}

			case pop: {
				prl(createPopEStack());
				break;
			}

			case jmp: {
				prl("CALL a" + (counter + get2()) + ";");
				break;
			}

			case jeq:
			case jne:
			case jlt:
			case jle:
			case jgt:
			case jge: {
				prl(createTopTwoEStack());
				prl("IF tempb " + getRelationFor(op) + " tempa THEN CALL a"
						+ (counter + get2()) + " ELSE CALL a" + (counter + 1)
						+ " FI;");
				break;
			}

			case call: {
				prl("CALL a" + (counter + get2()) + ";");
				break;
			}

			case return_: {
				// we let the actions return
				// there is nothing to clean up
				prl("SKIP END b" + counter + " ==");
				break;
			}
			case enter: {
				int parameters = get();

				int locals = get();
				prl(createToMStack("mjvm_locals"));
				prl("mjvm_locals := ARRAY(" + locals + ",0);");
				for (int i = parameters - 1; i >= 0; i--)
					prl(createFromEStack(createLocal(i)));
				break;
			}
			case exit: {
				prl(createFromMStack("mjvm_locals"));
				break;
			}

			// read, print
			case bread: {
				// TODO make it a char for read
				messages.message("char is read like a number", TransMessages.M_WAR);
				prl(createComment("char is read like a number", C_SPEC));
			}
			case read: {
				prl("tempa := @String_To_Num(@Read_Line(Standard_Input_Port));");
				prl(createToEStack("tempa"));
				break;
			}

			// the prints
			case bprint: {
				// TODO need to make it a char on print
				messages.message("chars will be printed as number codes", TransMessages.M_WAR);
				prl(createComment("char will be printed as a number code",
						C_SPEC));
			}
			case print: {
				// TODO printing numbers needs different lengths of spacing
				prl(createTopTwoEStack());
				prl("Print_MJ(tempb,tempa);");
				break;
			}

			case trap: {
				prl("ERROR(\"Runtime error: trap(" + get() + ")\");");
				break;
			}

			default:
				prl(createComment("unknown op error: " + op, C_ERR));
				messages.message("unknown op error: " + op, TransMessages.M_ERR);
				break;
			}

			boolean wasJump = isJumpCode(op);
			op = get();
			if (op >= 0)
				if (wasJump)
					prl("SKIP END");
				else
					prl("CALL a" + counter + " END");
		}
		prl("\nSKIP END\nENDACTIONS;\n");
		prl(createStandardEnd());
	}

	public void convertFile(File f) {
		try {
			convertStream(new FileInputStream(f));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void printHelp() {
		printVersion();
		printUsage();
		printHelpOutput();
		printHelpHelp();
	}
	
	public void printLongHelp() {
		printVersion();
		printUsage();
		System.out.println();
		printHelpOutput();
		System.out.println();
		printHelpGenerating();
		System.out.println();
		printHelpHelp();
	}

	public void printHelpOutput() {
		System.out.println("Output options:");
		System.out.println("  --screen print output to screen");
		System.out.println("  -o --oc[+-] include original code in comments");
		System.out.println("  -v verbose, print warning messages");
		System.out.println("  -q quiet; don't print even the error messages");
		System.out.println("  -d print detailed debug messages");
	}
		
	public void printHelpGenerating() {
		System.out.println("Options for generating extra code for tracking code execution");
		System.out.println("  --genEStackPrint generate print for all EStack changes");
		System.out.println("  --genAddrPrint  generate prints after every address of the original code ");
		System.out.println("  --genAddrPause  generate a pause after every address of the original code ");
		System.out.println("  --genAddr  short for --genAddrPrint and --genAddrPause");
		System.out.println("  --genAll   short for applying all code generation");
	}

	public void printHelpHelp() {
		System.out.println("Help and info options");
		System.out.println("  -h basic help");
		System.out.println("  --help print more detailed help");
		System.out.println("  --version or -version print version and exit");
	}
	
	public void printUsage(){
		System.out.println("usage:\n\t mjc2wsl {options} filename [outfile]");
	}

	public void printVersion() {
		System.out.println("MicroJava bytecode to WSL converter. v " + versionN
				+ ", by Doni Pracner");
	}
	
	public String makeDefaultOutName(String inname){
		String rez = inname;
		if (inname.endsWith(".obj"))
			rez = rez.substring(0, rez.length() - 4);
		return rez + ".wsl";
	}
	
	public void run(String[] args) {
		if (args.length == 0) {
			printHelp();
		} else {
			int i = 0;
			while (i < args.length && args[i].charAt(0) == '-') {
				if (args[i].compareTo("-h") == 0) {
					printHelp();
					return;
				} else if (args[i].compareTo("--help") == 0) {
					printLongHelp();
					return;
				} else if (args[i].compareTo("--version") == 0 
						|| args[i].compareTo("-version") == 0) {
					printVersion();
					return;
				} else if (args[i].compareTo("-o") == 0
						|| args[i].startsWith("--oc")) {
					if (args[i].length() == 2)
						originalInComments = true;
					else if (args[i].length() == 5)
						originalInComments = args[i].charAt(4) == '+';
					else
						originalInComments = true;
				} else if (args[i].compareTo("--screen") == 0) {
					out = new PrintWriter(System.out);
				} else if (args[i].compareTo("-d") == 0) {
					messages.setPrintLevel(TransMessages.M_DEB);// print debug info
				} else if (args[i].compareTo("-v") == 0) {
					messages.setPrintLevel(TransMessages.M_WAR);// print warnings
				} else if (args[i].compareTo("-q") == 0) {
					messages.setPrintLevel(TransMessages.M_QUIET);// no printing
				} else if (args[i].compareToIgnoreCase("--genEStackPrint") == 0) {
					genPrintEStackOnChange = true;
				} else if (args[i].compareToIgnoreCase("--genAddrPause") == 0) {
					genPauseAfterEachAddress = true;
				} else if (args[i].compareToIgnoreCase("--genAddrPrint") == 0) {
					genPrintForEachAddress = true;
				} else if (args[i].compareToIgnoreCase("--genAddr") == 0) {
					genPrintForEachAddress = true;
					genPauseAfterEachAddress = true;
				} else if (args[i].compareToIgnoreCase("--genAll") == 0) {
					genPrintEStackOnChange = true;
					genPrintForEachAddress = true;
					genPauseAfterEachAddress = true;
				}
				i++;
			}

			if (i >= args.length) {
				System.out.println("no filename supplied");
				System.exit(2);
			}
			File f = new File(args[i]);

			if (i + 1 < args.length) {
				try {
					out = new PrintWriter(args[i + 1]);
				} catch (Exception e) {
					System.err.println("error in opening out file:");
					e.printStackTrace();
				}
			}
			if (out == null) {
				// if not set to screen, or a file, make a default filename
				try {
					out = new PrintWriter(makeDefaultOutName(args[i]));
				} catch (Exception e) {
					System.err.println("error in opening out file:");
					e.printStackTrace();
				}
			}
			if (f.exists()) {
				Calendar now = Calendar.getInstance();
				convertFile(f);
				long mili = Calendar.getInstance().getTimeInMillis()
						- now.getTimeInMillis();
				System.out.println("conversion time:" + mili + " ms");
				messages.printMessageCounters();
				out.close();
			} else
				System.out.println("file does not exist");
		}
	}
	
	public static void main(String[] args) {
		new mjc2wsl().run(args);
	}
}