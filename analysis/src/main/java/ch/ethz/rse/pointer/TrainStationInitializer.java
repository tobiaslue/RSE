package ch.ethz.rse.pointer;

import java.util.LinkedList;
import java.util.List;

import soot.jimple.internal.JInvokeStmt;

/**
 * 
 * Contains information about the initializer of a TrainStation object
 *
 */
public class TrainStationInitializer {

	/**
	 * statement that performs the initialization
	 */
	private final JInvokeStmt statement;

	/**
	 * Unique identifier of the initializer
	 */
	private final int uniqueNumber;

	/**
	 * argument in the constructor
	 */
	public final int nTracks;

	private final String var;

	private List<JInvokeStmt> invokes = new LinkedList<JInvokeStmt>();
	/**
	 * 
	 * @param statement    piece of code running the initializer
	 * @param uniqueNumber unique identifier of the initializer
	 * @param nTracks      argument in the constructor
	 */
	public TrainStationInitializer(JInvokeStmt statement, int uniqueNumber, int nTracks, String var) {
		this.statement = statement;
		this.uniqueNumber = uniqueNumber;
		this.nTracks = nTracks;
		this.var = var;
	}

	/**
	 * 
	 * @return piece of code running the initializer
	 */
	public JInvokeStmt getStatement() {
		return statement;
	}

	public String getVar(){
		return var;
	}

	/**
	 * 
	 * @return unique identifier of the initializer
	 */
	private int getUniqueNumber() {
		return this.uniqueNumber;
	}

	/**
	 * 
	 * @return unique label of this initializer
	 */
	public String getUniqueLabel() {
		return "AbstractObject" + this.getUniqueNumber() + ".occupied";
	}

	public void addInvoke(JInvokeStmt s){
		invokes.add(s);
	}

	public List<JInvokeStmt> getInvokes(){
		return invokes;
	}
}