import java.io.PrintStream;


/**
 * Handle the messages that the translator (or someone else) will
 * give to this class depending on the print level.
 *
 */
public class TransMessages {
	private int printLevel;
	public int[] messageCounters;
	public static final int M_DEB = 0;
	public static final int M_WAR = 1;
	public static final int M_ERR = 2;
	public static final int M_QUIET = 3;
	private PrintStream outStream;

	public TransMessages() {
		this.setPrintLevel(M_ERR);
		this.messageCounters = new int[TransMessages.M_QUIET];
	}

	void message(String mes, int level){
			if (level>=getPrintLevel()) {
				outStream = System.out;
				outStream.println(mes);
			}
			messageCounters[level]++;
	}

	void printMessageCounters(PrintStream out){
			out.println("total errors:"+messageCounters[TransMessages.M_ERR]+" warnings:"+messageCounters[TransMessages.M_WAR]);
	}

	void printMessageCounters(){
			printMessageCounters(outStream);
	}

	public int getPrintLevel() {
		return printLevel;
	}

	public void setPrintLevel(int printLevel) {
		this.printLevel = printLevel;
	}
}