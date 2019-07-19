package com.quemaster.transformations.mjc2wsl;
/*
   Copyright (C) 2014,2015, 2016, 2018, 2019  Doni Pracner
   
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Properties;

import com.quemaster.transformations.TransMessages;

/**
 * This program converts file from compiled MicroJava bytecode to WSL language
 * which is a part of the FermaT Transformation system. MicroJava is a subset
 * used in Compiler Construction courses by Hanspeter Moessenboeck, not
 * "Java ME". 
 * 
 * @author Doni Pracner, http://perun.dmi.rs/pracner http://quemaster.com
 */
public class Mjc2wsl{
	//default version name, used if the file is not found
	private static String versionN = "v1.1.x";

	private String versionFile = "version.properties";
	
	private TransMessages messages = new TransMessages();

	private boolean genPauseAfterEachAddress=false, 
		genPrintForEachAddress = false,
		genPrintEStackOnChange = false;
		
	private boolean genPopPush = true;
	
	private boolean genInlinePrint = false;
	
	private boolean genLocalVars = true;

	// make an array for all local variables OTHERWISE make them separate
	private boolean genLocalsAsArray = true;

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
	
	private Properties versionData;

	private String getVersion() {
		if (versionData == null) {
			versionData = new Properties();
			try {
				versionData.load(getClass().getResourceAsStream(versionFile));
			} catch (IOException | NullPointerException e) {
				// it doesn't matter
				//e.printStackTrace();
			}
		}
		String ver = versionData.getProperty("version");
		if (ver != null)
			return ver;
		else
			return versionN;
	}
	
	private MicroJavaInput mjInput;

	private PrintWriter out = null;

	
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
	
	public String createStandardStart(){
			return createStandardStart(10);
	}

	public String createStandardStart(int numWords){
		StringBuilder ret = new StringBuilder(
			"C:\" This file was automatically converted from microjava bytecode\n"
			+"    using mjc2wsl "+getVersion()+"\n");

		ret.append("   -options:");
		ret.append("\n    popPush        (pp/ht):"+genPopPush);
		ret.append("\n    localVarBlocks (lo/gl):"+genLocalVars);
		ret.append("\n    localsAsArrays (ar/sp):"+genLocalsAsArray);
		ret.append("\n    inlinePrint:           "+genInlinePrint);
		ret.append("\n\";\n");

		ret.append("\nBEGIN");
		ret.append("\nVAR <\n\t");
		if (!genLocalVars){ 
			ret.append("\n\ttempa := 0, tempb :=0, tempres := 0,");
		} else
			ret.append("\n\tmjvm_flag_jump := 0,");

		if (genLocalsAsArray)
			ret.append("mjvm_locals := ARRAY(1,0),");

		ret.append("\n\tmjvm_statics := ARRAY("+numWords+",0),");
		ret.append("\n\tmjvm_arrays := < >,");
		ret.append("\n\tmjvm_objects := < >,");
		ret.append("\n\tmjvm_estack := < >, mjvm_mstack := < > > :");

		return ret.toString();
	}

	public String createAsciiString(){
		StringBuilder ret = new StringBuilder("C:\"char array for ascii code conversions\";");
		ret.append("\nascii := \"????????????????????????????????\"++\n");
		ret.append("\" !\"++Quote++\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\";\n");

		return ret.toString();
	}

	public String createStandardEnd(){
		StringBuilder ret = new StringBuilder("SKIP\nENDVAR\n");
		ret.append("\nWHERE\n");
		
		ret.append("\nFUNCT CHR(num) ==:\n");
		ret.append("\t(@List_To_String(< num >))\n");
		ret.append("END\n");

		if (!genInlinePrint) {
				ret.append("\nPROC Print_MJ(val, format VAR)==\n");
				ret.append("PRINFLUSH(@Format(format, val ))");
				ret.append("\nEND\n");

				ret.append("\nPROC Print_MJ_CHAR(val, format VAR)==\n");
				ret.append("PRINFLUSH(@Format(format, CHR(val)))");
				ret.append("\nEND\n");
		}

		ret.append("\nEND");
		return ret.toString();
	}

	private String createStartVar(String... vars){
		if (genLocalVars) {
			StringBuilder ret = new StringBuilder("VAR < ");
			ret.append(vars[0] + " := 0");
			for (int i = 1; i < vars.length; i++)
				ret.append(", " + vars[i] + " := 0");
			ret.append(" > : ");

			return ret.toString();
		}
		return "";
	}
	
	private String createEndVar(){
		if (genLocalVars)
			return "ENDVAR;";
		else
			return "";
	}
	
	private String createLocal(int i) {
		// arrays start at 1 in WSL, so we need an offset
		if (genLocalsAsArray)
			return "mjvm_locals[" + (i + 1) + "]";
		else
			return "mjvm_locals_" + i;
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
	
	// generalised stack operations
	
	private String createToStack(String stack, String var){
		if (genPopPush)
				return 	"PUSH("+stack+"," + var + ");";
		else
				return stack + " := <" + var + " > ++ " + stack +";";
	}

	private String createFromStack(String stack, String var){
		if (genPopPush)
			return "POP("+ var + ", "+stack+");";
		else
			return var + ":= HEAD("+stack+"); "+stack+" := TAIL("+stack+");";
	}
//Expression stack
	
	private String createToEStack(int i) {
		return createToEStack(i+"");
	}

	private String createToEStack(String i) {
		String res = createToStack("mjvm_estack", i);
		if (genPrintEStackOnChange)
			res += "PRINT(\"eStack\",mjvm_estack);";
		return res;
	}

	private String createFromEStack(String st) {
		String res = createFromStack("mjvm_estack",st);
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
		return createToMStack(i+"");
	}

	private String createToMStack(String i) {
		return createToStack("mjvm_mstack", i);
	}

	private String createFromMStack(String st) {
		return createFromStack("mjvm_mstack", st);
	}

	public void convertStream(InputStream ins) throws Exception{
		mjInput = new MicroJavaInput(ins);

		prl(createStandardStart(mjInput.getNumberOfWords()));
		prl("SKIP;\n ACTIONS a" + (14 + mjInput.getMainAdr()) + " :");

		// the number of Locals for procedures; need to remember it for exits
		int numberOfLocals = 0;

		int op = mjInput.get();
		while (op >= 0) {
			messages.message("Processing "+mjInput.describeOpCode(op)+ " at " + mjInput.getCounter(), TransMessages.M_DEB);
			prl(" a" + mjInput.getCounter() + " ==");
			if (originalInComments)
				prl(createComment(mjInput.describeOpCode(op), C_OC));
			if (genPrintForEachAddress) {
				prl("PRINT(\"a" + mjInput.getCounter() + "\");");
				if (genPauseAfterEachAddress)
					prl("@Read_Line_Proc(VAR debug_disposable_string, Standard_Input_Port);");
			}
			switch (op) {
			case load: {
				prl(createToEStack(createLocal(mjInput.get())));
				break;
			}
			case load_0:
			case load_1:
			case load_2:
			case load_3: {
				prl(createStartVar("tempa"));
				prl("tempa :="+createLocal(op - load_0)+";");
				prl(createToEStack("tempa"));
				prl(createEndVar());
				break;
			}
			case store: {
				prl(createFromEStack(createLocal(mjInput.get())));
				break;
			}
			case store_0:
			case store_1:
			case store_2:
			case store_3: {
				prl(createStartVar("tempa"));
				prl(createFromEStack("tempa"));
				prl(createLocal(op - store_0)+" := tempa;");
				prl(createEndVar());
				break;
			}

			case getstatic: {
				prl(createToEStack(createStatic(mjInput.get2())));
				break;
			}
			case putstatic: {
				prl(createFromEStack(createStatic(mjInput.get2())));
				break;
			}

			case getfield: {
				int f = mjInput.get2();
				prl(createStartVar("tempa"));
				prl(createTopEStack());
				prl(createToEStack(createObject("tempa") + "[" + (f + 1) + "]"));
				prl(createEndVar());
				break;
			}
			case putfield: {
				int f = mjInput.get2();
				prl(createStartVar("tempa", "tempb"));
				prl(createTopTwoEStack());				
				prl(createObject("tempb") + "[" + (f + 1) + "]:=tempa;");
				prl(createEndVar());
				break;
			}

			case const_: {
				prl(createToEStack(mjInput.get4()));
				break;
			}

			case const_m1: {
				prl(createToEStack(-1));
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
				prl(createStartVar("tempa", "tempb", "tempres"));
				prl(createTopTwoEStack());
				prl("tempres := tempb + tempa;");
				prl(createToEStack("tempres"));
				prl(createEndVar());
				break;
			}
			case sub: {
				prl(createStartVar("tempa", "tempb", "tempres"));
				prl(createTopTwoEStack());
				prl("tempres := tempb - tempa;");
				prl(createToEStack("tempres"));
				prl(createEndVar());
				break;
			}
			case mul: {
				prl(createStartVar("tempa", "tempb", "tempres"));
				prl(createTopTwoEStack());
				prl("tempres := tempb * tempa;");
				prl(createToEStack("tempres"));
				prl(createEndVar());
				break;
			}
			case div: {
				prl(createStartVar("tempa", "tempb", "tempres"));
				prl(createTopTwoEStack());
				prl("IF tempa = 0 THEN ERROR(\"division by zero\") FI;");
				prl("tempres := tempb DIV tempa;");
				prl(createToEStack("tempres"));
				prl(createEndVar());
				break;
			}
			case rem: {
				prl(createStartVar("tempa", "tempb", "tempres"));
				prl(createTopTwoEStack());
				prl("IF tempa = 0 THEN ERROR(\"division by zero\") FI;");
				prl("tempres := tempb MOD tempa;");
				prl(createToEStack("tempres"));
				prl(createEndVar());
				break;
			}

			case neg: {
				prl(createStartVar("tempa"));
				prl(createTopEStack());
				prl(createToEStack("-tempa"));
				prl(createEndVar());				
				break;
			}

			case shl: {
				prl(createStartVar("tempa", "tempb"));
				prl(createTopTwoEStack());
				prl("VAR <tempres :=tempb, i:=1 >:");
				prl("\tFOR i:=1 TO tempa STEP 1 DO tempres := tempres * 2 OD;");
				prl(createToEStack("tempres"));
				prl("ENDVAR;");
				prl(createEndVar());
				break;
			}
			case shr: {
				prl(createStartVar("tempa", "tempb"));
				prl(createTopTwoEStack());
				prl("VAR <tempres :=tempb, i:=1 >:");
				prl("\tFOR i:=1 TO tempa STEP 1 DO tempres := tempres DIV 2 OD;");
				prl(createToEStack("tempres"));
				prl("ENDVAR;");
				prl(createEndVar());
				break;
			}

			case inc: {
				int b1 = mjInput.get(), b2 = mjInput.get();
				prl(createLocal(b1) + " := " + createLocal(b1) + " + " + b2 + ";");
				break;
			}

			case new_: {
				int size = mjInput.get2();
				// TODO maybe objects and arrays should be in the same list?
				prl("mjvm_objects := mjvm_objects ++ < ARRAY(" + size
						+ ",0) >;");
				prl(createToEStack("LENGTH(mjvm_objects)"));
				break;
			}
			case newarray: {
				mjInput.get();// 0 - bytes, 1 - words; ignore for now
				// TODO take into consideration 0/1
				prl(createStartVar("tempa"));
				prl(createTopEStack());
				prl("mjvm_arrays := mjvm_arrays ++ < ARRAY(tempa,0) >;");
				prl(createToEStack("LENGTH(mjvm_arrays)"));
				prl(createEndVar());
				break;
			}

			case aload:
			case baload: {
				prl(createStartVar("tempa", "tempb"));
				prl(createTopTwoEStack());
				prl(createToEStack(createArray("tempb") + "[tempa+1]"));
				prl(createEndVar());
				break;
			}
			case astore:
			case bastore: {
				prl(createStartVar("tempa", "tempb", "tempres"));
				prl(createFromEStack("tempres"));
				prl(createTopTwoEStack());
				prl("mjvm_arrays[tempb][tempa+1]:=tempres;");
				prl(createEndVar());
				break;
			}
			case arraylength: {
				prl(createStartVar("tempa", "tempb"));
				prl(createTopEStack());
				prl("tempb := LENGTH("+ createArray("tempa") + ");");
				prl(createToEStack("tempb"));
				prl(createEndVar());
				break;
			}

			case dup: {
				prl(createStartVar("tempa", "tempb"));
				prl(createTopEStack());
				prl(createToEStack("tempa"));
				prl(createToEStack("tempa"));
				prl(createEndVar());
				break;
			}
			case dup2: {
				prl(createStartVar("tempa", "tempb"));
				prl(createTopTwoEStack());
				prl(createToEStack("tempb"));
				prl(createToEStack("tempa"));
				prl(createToEStack("tempb"));
				prl(createToEStack("tempa"));
				prl(createEndVar());
				break;
			}

			case pop: {
				prl(createPopEStack());
				break;
			}

			case jmp: {
				prl("CALL a" + (mjInput.getCounter() + mjInput.get2()) + ";");
				break;
			}

			case jeq:
			case jne:
			case jlt:
			case jle:
			case jgt:
			case jge: {
				if (genLocalVars) {
						prl(createStartVar("tempa", "tempb"));
						prl(createTopTwoEStack());
						prl("IF tempb " + mjInput.getRelationFor(op)
								+ " tempa THEN mjvm_flag_jump := 1"
								+ " ELSE mjvm_flag_jump := 0"
								+ " FI;");
						prl(createEndVar());
						prl("IF mjvm_flag_jump = 1 THEN CALL a"
								+ (mjInput.getCounter() + mjInput.get2())
								+ " ELSE CALL a" + (mjInput.getCounter() + 1)
								+ " FI;");
				} else {
						prl(createTopTwoEStack());
						prl("IF tempb " + mjInput.getRelationFor(op)
								+ " tempa THEN CALL a"
								+ (mjInput.getCounter() + mjInput.get2())
								+ " ELSE CALL a" + (mjInput.getCounter() + 1)
								+ " FI;");
				}
				break;
			}

			case call: {
				prl("CALL a" + (mjInput.getCounter() + mjInput.get2()) + ";");
				break;
			}

			case return_: {
				// we let the actions return
				// there is nothing to clean up
				prl("SKIP\n END\n b" + mjInput.getCounter() + " ==");
				break;
			}
			case enter: {
				int parameters = mjInput.get();

				numberOfLocals = mjInput.get();
				if (genLocalsAsArray) {
					prl(createToMStack("mjvm_locals"));
					prl("mjvm_locals := ARRAY(" + numberOfLocals + ",0);");
				} else {
					// TODO maybe we should generate VAR block for these somewhere
					for (int i = 0; i < numberOfLocals; i++) {
						prl(createToMStack(createLocal(i)));
					}
				}

				for (int i = parameters - 1; i >= 0; i--)
					prl(createFromEStack(createLocal(i)));
				break;
			}
			case exit: {
				if (genLocalsAsArray) {
					prl(createFromMStack("mjvm_locals"));
				} else {
					// there are as many locals as defined in the last enter
					for (int i = numberOfLocals -1; i >= 0; i--)
						prl(createFromMStack(createLocal(i)));
				}
				break;
			}

			// read, print
			case bread: {
				// TODO maybe we'll need a buffer for multi chars!
				prl(createStartVar("tempa"));
				prl("@Read_Line_Proc(VAR tempa, Standard_Input_Port);");
				prl("tempa := @String_To_List(tempa)[1];");
				prl(createToEStack("tempa"));
				prl(createEndVar());
				break;
			}
			case read: {
				prl(createStartVar("tempa"));
				prl("@Read_Line_Proc(VAR tempa, Standard_Input_Port);");
				prl("tempa := @String_To_Num(tempa);");
				prl(createToEStack("tempa"));
				prl(createEndVar());
				break;
			}

			// the prints
			case bprint: {
				prl(createStartVar("tempa", "tempb"));
				prl(createTopTwoEStack());
				if (genInlinePrint){
					prl(createComment("print spacing and transformation",C_SPEC));
					prl("PRINFLUSH(@Format(tempa, @List_To_String(< tempb >)));");
				} else
					prl("Print_MJ_CHAR(tempb,tempa);");
				prl(createEndVar());
				break;
			}
			case print: {
				prl(createStartVar("tempa", "tempb"));

				prl(createTopTwoEStack());
				if (genInlinePrint){
					prl(createComment("print spacing",C_SPEC));
					prl("PRINFLUSH(@Format(tempa,tempb));");
				}
				else
					prl("Print_MJ(tempb,tempa);");
				prl(createEndVar());
				break;
			}

			case trap: {
				prl("ERROR(\"Runtime error: trap(" + mjInput.get() + ")\");");
				break;
			}

			default:
				prl(createComment("unknown op error: " + op, C_ERR));
				messages.message("unknown op error: " + op, TransMessages.M_ERR);
				break;
			}

			boolean wasJump = mjInput.isJumpCode(op);
			op = mjInput.get();
			if (op >= 0)
				if (wasJump)
					prl("SKIP\n END");
				else
					prl("CALL a" + mjInput.getCounter() + "\n END");
		}
		prl("SKIP\n END\nENDACTIONS;\n");
		pr(createStandardEnd());
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
		printHelpDirectives();
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

	public void printHelpDirectives(){
		System.out.println("Alternatives for code generation (* are the defaults):");
		System.out.print(genPopPush?'*':' ');
		System.out.println(" --genPopPush generate POP/PUSH instead of TAIL/HEAD (pp)");
		System.out.print(!genPopPush?'*':' ');
		System.out.println(" --genHeadTail generate TAIL/HEAD instead of POP/PUSH (ht)");
		System.out.println();
		System.out.print(genInlinePrint?'*':' ');
		System.out.println(" --genInlinePrint generate prints directly instead of procedure calls");
		System.out.print(!genInlinePrint?'*':' ');
		System.out.println(" --genProcedurePrint generate prints as custom procedure calls");
		System.out.println();
		System.out.print(genLocalVars?'*':' ');
		System.out.println(" --genLocalVars generate local VAR block for temp variables (lo)");
		System.out.print(!genLocalVars?'*':' ');
		System.out.println(" --genGlobalVars do NOT generate local VAR block for temp variables (gl)");
		System.out.println();
		System.out.print(genLocalsAsArray?'*':' ');
		System.out.println(" --genLocalsAsArray generate local variables as an array (ar)");
		System.out.print(!genLocalsAsArray?'*':' ');
		System.out.println(" --genLocalsSeparate generate local variables as separate entities (sp)");
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
		System.out.println("MicroJava bytecode to WSL converter " + getVersion()
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
			while (i < args.length && args[i].length() > 0 && args[i].charAt(0) == '-') {
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
				} else if (args[i].compareToIgnoreCase("--genPopPush") == 0) {
					genPopPush = true;
				} else if (args[i].compareToIgnoreCase("--genInlinePrint") == 0) {
					genInlinePrint = true;
				} else if (args[i].compareToIgnoreCase("--genHeadTail") == 0) {
					genPopPush = false;
				} else if (args[i].compareToIgnoreCase("--genProcedurePrint") == 0) {
					genInlinePrint = false;
				} else if (args[i].compareToIgnoreCase("--genLocalVars") == 0) {
					genLocalVars = true;
				} else if (args[i].compareToIgnoreCase("--genGlobalVars") == 0) {
					genLocalVars = false;
				} else if (args[i].compareToIgnoreCase("--genLocalsAsArray") == 0) {
					genLocalsAsArray = true;
				} else if (args[i].compareToIgnoreCase("--genLocalsSeparate") == 0) {
					genLocalsAsArray = false;
				} else {
					System.err.println("unknown option, refusing to continue: "+args[i]);
					System.exit(1);
				}
				i++;
			}

			if (i >= args.length) {
				System.err.println("no filename supplied");
				System.exit(2);
			}

			Path p = FileSystems.getDefault().getPath(args[i]);
			if (!Files.isRegularFile(p)){
				System.err.println("input file does not exist");
				System.exit(1);
			}

			if (i + 1 < args.length) {
				try {
					out = new PrintWriter(args[i + 1]);
				} catch (Exception e) {
					System.err.println("error in opening out file:");
					e.printStackTrace();
				}
				if (i + 2 < args.length) {
					messages.message("additional parameters ignored, starting from:'"+args[i+2]+"'", TransMessages.M_ERR);
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
			Calendar now = Calendar.getInstance();
			try {
				convertStream(Files.newInputStream(p));
			} catch (Exception e) {
				System.err.println("Failed converting the stream");
				e.printStackTrace();
			}
			long mili = Calendar.getInstance().getTimeInMillis()
					- now.getTimeInMillis();
			messages.message("conversion time:" + mili + " ms");
			messages.printMessageCounters();
			out.close();
		}
	}
	
	public static void main(String[] args) {
		new Mjc2wsl().run(args);
	}


}
