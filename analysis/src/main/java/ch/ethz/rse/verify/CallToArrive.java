package ch.ethz.rse.verify;

import ch.ethz.rse.numerical.NumericalAnalysis;
import ch.ethz.rse.numerical.NumericalStateWrapper;
import soot.SootMethod;
import soot.jimple.internal.JVirtualInvokeExpr;

/**
 * Convenience wrapper that stores information about a specific call to arrive
 */
public class CallToArrive {

	public final SootMethod method;
	public final JVirtualInvokeExpr invokeExpr;
	public final NumericalAnalysis analysis;
	public final NumericalStateWrapper state;

	public CallToArrive(SootMethod method, JVirtualInvokeExpr invokeExpr, NumericalAnalysis analysis,
			NumericalStateWrapper state) {
		this.method = method;
		this.invokeExpr = invokeExpr;
		this.analysis = analysis;
		this.state = state;
	}
}
