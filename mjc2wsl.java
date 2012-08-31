import java.io.*;
import java.util.*;

/**
 * This program converts file from compiled MicroJava bytecode (a subset used
 * in Compiler Construction courses by H. Moessenboeck, not "Java ME")
 * to WSL language which is a part of the FermaT Transformation system.
 *
 * @author Doni Pracner, http://perun.dmi.rs/pracner http://quemaster.com
 */
public class mjc2wsl{
	public static String versionN = "0.1";
		
	//regular comments from the original file
	//OC when original code is inserted in the file, next to the translations
	//SPEC special messages from the translator
	//ERR error messages from the transnlator
	public static final char C_REG = ' ', C_OC = '#', C_SPEC = '&', C_ERR = '!';

	
		/** instruction code */
		public static final int 
		load        =  1,
		load_0      =  2,
		load_1      =  3,
		load_2      =  4,
		load_3      =  5,
		store       =  6,
		store_n     =  7,
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
		jcc         = 43,
		call        = 49,
		return_     = 50,
		enter       = 51,
		exit        = 52,
		read        = 53,
		print       = 54,
		bread       = 55,
		bprint      = 56,
		trap		= 57;

	public String getStandardStart(){
		String ret ="C:\" This file automatically converted from microjava bytecode\";\n"
			+"C:\" with mjc2wsl v "+versionN+"\";\n";
		ret +="VAR < tempa := 0, tempb := 0, tempres :=0,\n"+
		"	stack := < >, t_e_m_p := 0 > :\n";
		
		return ret;
	}

	public String getStandardEnd(){
		return "SKIP\nENDVAR";
	}
		
	private InputStream mainIn;
	private PrintStream out = System.out;
	
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
			try{
					res = mainIn.read();
					if (res>=0) res = res<<24>>>24;
			}catch (IOException ex){
				ex.printStackTrace();
			}
			return res;
	}
	
	private int get2() {
		return (get()*256 + get())<<16>>16;
	}
	
	private int get4() {
		return (get2()<<16) + (get2()<<16>>>16);
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
	public static String createComment(String str, char type){
		return "C:\""+type+str.replace("\"", "''")+"\";";
	}
	
	private String cmdToStack(int i){
			return "stack := <"+i+" > ++ stack;";
	}
	
	private String cmdToStack(String i){
			return "stack := <"+i+" > ++ stack;";
	}
	
	private String cmdFromStack(String st){
			return st+" := HEAD(stack); stack := TAIL(stack);";
	}
	
	private String getTopTwo(){
		 return cmdFromStack("tempa")+cmdFromStack("tempb");
	}
	
	private String getTop(){
		 return cmdFromStack("tempa");
	}
	
	public void convertStream(InputStream ins){
		mainIn = ins;
		//skip start TODO make better
		for (int i=0;i<14;i++) get();
		
		prl(getStandardStart());
		
		int op = get();
		while (op>=0){
				switch(op) {
					case load: {
						prl(cmdToStack(get()));
						break;
					}
					case load_0: {
						prl(cmdToStack(0));
						break;
					}			
					case const_: {
						prl(cmdToStack(get4()));
						break;
					}
					case const_0: {
						prl(cmdToStack(0));
						break;
					}
					case const_1: {
						prl(cmdToStack(1));
						break;
					}
					case const_2: {
						prl(cmdToStack(2));
						break;
					}
					case const_3: {
						prl(cmdToStack(3));
						break;
					}
					case const_4: {
						prl(cmdToStack(4));
						break;
					}
					
					case const_5: {
						prl(cmdToStack(5));
						break;
					}
					
					case div: {
						prl(getTopTwo());
						prl("tempr = tempa / tempb;");
						prl(cmdToStack("tempr"));
						break;
					}
					
					case enter: {
						prl(createComment("enter"));
						get();get();
						break;
					}

					//the prints
					case bprint: {
									prl(getTop());
									prl("PRINT(tempa);");
									break;
					}
					case print: {
									//TODO need to make it a char
									prl(getTop());
									prl("PRINT(tempa);");
									break;
					}

					default: prl(createComment("unknown op error: "+op,C_ERR)); break;
				}
				op = get();
		}
		prl(getStandardEnd());
		
	}
		
	public void convertFile(File f){
			try{
					convertStream(new FileInputStream(f));
			}catch (Exception ex){
					ex.printStackTrace();
			}
	}
	
	public void run(String[] args){
		if (args.length == 0){
			System.out.println("MicroJava bytecode to WSL converter. v "+versionN+", by Doni Pracner");
			System.out.println("usage:\n\t mjc2wsl  filename");
		}else{
			File f = new File(args[0]);
			if (f.exists()){
				Calendar now = Calendar.getInstance();
				convertFile(f);
				long mili = Calendar.getInstance().getTimeInMillis() - now.getTimeInMillis();
				System.out.println("conversion time:"+mili+" ms");
			}else 
				System.out.println("file does not exist");			
		}
	}
	
	public static void main(String[] args){
			new mjc2wsl().run(args);
	}
}