package ch.ethz.rse.pointer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import ch.ethz.rse.utils.Constants;
import ch.qos.logback.core.rolling.TriggeringPolicy;
import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.spark.pag.Node;
import soot.toolkits.graph.UnitGraph;

public class PointsToInitializer {

	private static final Logger logger = LoggerFactory.getLogger(PointsToInitializer.class);

	/**
	 * Internally used points-to analysis
	 */
	private final PointsToAnalysisWrapper pointsTo;

	/**
	 * class for which we are running points-to
	 */
	private final SootClass c;

	/**
	 * Maps abstract object indices to initializers
	 */
	private final Map<Node, TrainStationInitializer> initializers = new HashMap<Node, TrainStationInitializer>();

	/**
	 * All {@link TrainStationInitializer}s, keyed by method
	 */
	private final Multimap<SootMethod, TrainStationInitializer> perMethod = HashMultimap.create();

	public PointsToInitializer(SootClass c) {
		this.c = c;
		logger.debug("Running points-to analysis on " + c.getName());
		this.pointsTo = new PointsToAnalysisWrapper(c);
		logger.debug("Analyzing initializers in " + c.getName());
		this.analyzeAllInitializers();
	}

	private void analyzeAllInitializers() {
		for (SootMethod method : this.c.getMethods()) {

			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}
		
			UnitGraph g = SootHelper.getUnitGraph(method);

			

			for(Unit u : g){
				if(u instanceof JInvokeStmt){
					JInvokeStmt s = (JInvokeStmt) u;
					InvokeExpr e = s.getInvokeExpr();
					if(e.getMethod().getName().equals("<init>")){
						List<ValueBox> boxes = s.getUseBoxes();
						JimpleLocalBox left = null;
						
						for(ValueBox b:boxes){
							if(b instanceof JimpleLocalBox){
								left = (JimpleLocalBox) b;
							}
						}
				
						Local base = (Local) left.getValue();
						Collection<Node> nodes = pointsTo.getNodes(base);
						Node node = nodes.iterator().next();
						Value arg = e.getArg(0);
						int x = ((IntConstant) arg).value;
						TrainStationInitializer t = new TrainStationInitializer(s, left.hashCode(), x);
						perMethod.put(method, t);
						initializers.put(node, t);
					}			
				}
				
			}

			// populate data structures
			// FILL THIS OUT
		}
	}

	public List<TrainStationInitializer> pointsTo(Local base){
		Collection<Node> nodes = pointsTo.getNodes(base);
		List<TrainStationInitializer> stations = new ArrayList<TrainStationInitializer>();
		for(Node node : nodes){
			stations.add(initializers.get(node));
		}
		
		return stations;
	}

	public Collection<TrainStationInitializer> getStationByMethod(SootMethod m){
		Iterator it = perMethod.keySet().iterator();
		Collection<TrainStationInitializer> stations = null;
		while(it.hasNext()){
			SootMethod key = (SootMethod)it.next();
			if(key.equals(m)){
				stations = perMethod.get(key);

			}
		}
		return stations;
	}
}
