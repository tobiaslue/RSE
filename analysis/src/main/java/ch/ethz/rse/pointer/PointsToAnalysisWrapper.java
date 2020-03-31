package ch.ethz.rse.pointer;

import java.util.Collection;
import java.util.LinkedList;

import soot.Local;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;

public class PointsToAnalysisWrapper {

	private final PointsToAnalysis pointsToAnalysis;

	public PointsToAnalysisWrapper(SootClass c) {
		// fetch results from previously ran points-to-analysis
		this.pointsToAnalysis = Scene.v().getPointsToAnalysis();
	}

	/**
	 * 
	 * @param base
	 * @return a list of nodes that base could point to
	 */
	public Collection<Node> getNodes(Local base) {
		PointsToSetInternal pts = (PointsToSetInternal) this.pointsToAnalysis.reachingObjects(base);
		P2SetCollector c = new P2SetCollector();
		pts.forall(c);
		return c.getNodes();
	}

}

/**
 * Ugly hack to obtain set from {@link P2SetVisitor}
 */
class P2SetCollector extends P2SetVisitor {

	private final Collection<Node> nodes = new LinkedList<Node>();

	@Override
	public void visit(Node node) {
		nodes.add(node);
	}

	public Collection<Node> getNodes() {
		return this.nodes;
	}
}
