package ch.ethz.rse.pointer;

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

	/**
	 * 
	 * @param statement    piece of code running the initializer
	 * @param uniqueNumber unique identifier of the initializer
	 * @param nTracks      argument in the constructor
	 */
	public TrainStationInitializer(JInvokeStmt statement, int uniqueNumber, int nTracks) {
		this.statement = statement;
		this.uniqueNumber = uniqueNumber;
		this.nTracks = nTracks;
	}

	/**
	 * 
	 * @return piece of code running the initializer
	 */
	public JInvokeStmt getStatement() {
		return statement;
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
}