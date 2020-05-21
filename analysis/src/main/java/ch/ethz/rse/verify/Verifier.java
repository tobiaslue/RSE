package ch.ethz.rse.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import apron.Interval;
import apron.Lincons0;
import apron.Lincons1;
import apron.Linexpr0;
import apron.Linexpr1;
import apron.Linterm1;
import apron.MpqScalar;
import apron.Tcons1;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import apron.Texpr1Node;
import apron.Texpr1VarNode;
import apron.Var;
import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.numerical.NumericalAnalysis;
import ch.ethz.rse.numerical.NumericalStateWrapper;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.pointer.TrainStationInitializer;
import ch.ethz.rse.utils.Constants;
import javassist.bytecode.Descriptor.Iterator;
import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.JastAddJ.Body;
import soot.coffi.CFG;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.spark.ondemand.genericutil.MultiMap;
import soot.toolkits.exceptions.ThrowableSet.Manager;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

/**
 * Main class handling verification
 * 
 */
public class Verifier extends AVerifier {

	private static final Logger logger = LoggerFactory.getLogger(Verifier.class);

	private final SootClass c;
	private final PointsToInitializer pointsTo;
	private final Map<SootMethod, NumericalAnalysis> numericalAnalysis = new HashMap<SootMethod, NumericalAnalysis>();

	/**
	 * 
	 * @param c class to verify
	 */
	public Verifier(SootClass c) {
		logger.debug("Analyzing {}", c.getName());

		this.c = c;
		// pointer analysis
		this.pointsTo = new PointsToInitializer(this.c);
		// numerical analysis
		this.runNumericalAnalysis();
	}

	private void runNumericalAnalysis() {
		List<SootMethod> methods = c.getMethods();
		for (SootMethod m : methods) {
			if (m.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			UnitGraph g = SootHelper.getUnitGraph(m);
			NumericalAnalysis analysis = new NumericalAnalysis(m, g, pointsTo);
			numericalAnalysis.put(m, analysis);
		}
	}

	@Override
	public boolean checkTrackNonNegative() {
		for (SootMethod m : numericalAnalysis.keySet()) {
			NumericalAnalysis analysis = numericalAnalysis.get(m);
			apron.Manager man = analysis.man;
			Environment env = analysis.env;

			Collection<TrainStationInitializer> stations = pointsTo.getStationByMethod(m);
			Multimap<TrainStationInitializer, JInvokeStmt> stationInvoke = analysis.stationInvoke;
			Map<JInvokeStmt, Abstract1> invokeAbstract = analysis.invokeAbstract;
			Map<JInvokeStmt, Value> invokeValue = analysis.invokeValue;

			for (TrainStationInitializer s : stations) {
				Collection<JInvokeStmt> invokes = stationInvoke.get(s);

				for(JInvokeStmt stmt : invokes){
					Value par = invokeValue.get(stmt);

					try {
						Linexpr1 le = null;
						if (par instanceof IntConstant) {
							int x = ((IntConstant) par).value;
							if (x < 0) {
								return false;
							}
						} else {
							Linterm1 lt = analysis.getTermOfLocal(par, -1);
							le = new Linexpr1(env, new Linterm1[] { lt }, new MpqScalar(0));
							Lincons1 c = new Lincons1(Lincons1.SUP, le);
							Abstract1 factIn = invokeAbstract.get(stmt);
							factIn.meet(man, c);
							if (!factIn.isBottom(man)) {
								return false;
							}
						}
					} catch (ApronException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean checkTrackInRange() {
		for(SootMethod m : numericalAnalysis.keySet()){
			NumericalAnalysis analysis = numericalAnalysis.get(m);
			apron.Manager man = analysis.man;
			Environment env = analysis.env;
		
			Collection<TrainStationInitializer> stations = pointsTo.getStationByMethod(m);
			Multimap<TrainStationInitializer, JInvokeStmt> stationInvoke = analysis.stationInvoke;
			Map<JInvokeStmt, Abstract1> invokeAbstract = analysis.invokeAbstract;
			Map<JInvokeStmt, Value> invokeValue = analysis.invokeValue;

			for(TrainStationInitializer s : stations){
				Collection<JInvokeStmt> invokes = stationInvoke.get(s);

				for(JInvokeStmt stmt : invokes){
					Value par = invokeValue.get(stmt);
					
					if (par instanceof Local) {
						Linterm1 lint = analysis.getTermOfLocal((Local) par, 1);
						Linexpr1 line = new Linexpr1(env, new Linterm1[] { lint }, new MpqScalar(-s.nTracks));
						Lincons1 c = new Lincons1(Lincons1.SUP, line);
						logger.info(c.toString());
						try {
							Abstract1 factIn = invokeAbstract.get(stmt);
							factIn.meet(man, c);
							logger.info(factIn.toString());
							if (!factIn.isBottom(man)) {
								return false;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (par instanceof IntConstant) {
						int x = ((IntConstant) par).value;
						if (x > s.nTracks) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean checkNoCrash() {
		List<SootMethod> methods = c.getMethods();
		SootMethod m = methods.get(1);
		UnitGraph g = SootHelper.getUnitGraph(m);
		NumericalAnalysis analysis = new NumericalAnalysis(m, g, pointsTo);

		apron.Manager man = analysis.man;
		Environment env = analysis.env;

		// Multimap<TrainStationInitializer, InvokeExpr> stationInvoke=
		// HashMultimap.create();
		// Map<InvokeExpr, Abstract1> invokeAbstract = new HashMap<InvokeExpr,
		// Abstract1>();
		// Map<InvokeExpr, Value> invokeValue = new HashMap<InvokeExpr, Value>();

		Multimap<TrainStationInitializer, JInvokeStmt> stationInvoke = analysis.stationInvoke;
		Map<JInvokeStmt, Abstract1> invokeAbstract = analysis.invokeAbstract;
		Map<JInvokeStmt, Value> invokeValue = analysis.invokeValue;

		// for (Unit u : m.getActiveBody().getUnits()) {
		// NumericalStateWrapper before = analysis.getFlowBefore(u);
		// Abstract1 factIn = before.get();
		// if (u instanceof JInvokeStmt) {
		// InvokeExpr e = ((JInvokeStmt) u).getInvokeExpr();
		// Value par = ((ValueBox) e.getUseBoxes().get(0)).getValue();
		// logger.info(e.getMethod().getName());
		// if (e.getMethod().getName().equals("arrive")) {
		// logger.info("arrive");
		// logger.info(e.toString());
		// List<ValueBox> boxes = e.getUseBoxes();
		// JimpleLocalBox left = null;

		// for (ValueBox b : boxes) {
		// if (b instanceof JimpleLocalBox) {
		// left = (JimpleLocalBox) b;
		// }
		// }
		// Local base = (Local) left.getValue();
		// List<TrainStationInitializer> stations = pointsTo.pointsTo(base);
		// logger.info(stations.toString());
		// for (TrainStationInitializer t : stations) {
		// stationInvoke.put(t, e);
		// invokeAbstract.put(e, before.get());
		// invokeValue.put(e, par);
		// }

		// }
		// }
		// }

		Collection<TrainStationInitializer> stations = pointsTo.getStationByMethod(m);
		logger.info(stationInvoke.toString());
		logger.info(invokeAbstract.toString());
		logger.info(invokeValue.toString());
		if (stations.isEmpty()) {
			return true;
		}
		for (TrainStationInitializer s : stations) {
			Collection<JInvokeStmt> invokes = stationInvoke.get(s);

			logger.info(invokes.toString());

			Linterm1 tv = null;
			Linterm1 tu = null;
			Linexpr1 e = null;
			Lincons1 c = null;
			int x = 0;
			int y = 0;

			for (JInvokeStmt i1 : invokes) {
				for (JInvokeStmt i2 : invokes) {
					if (i1.equals(i2)) {
						logger.info("euqal");
						continue;
					}

					Value v = invokeValue.get(i1);
					Value u = invokeValue.get(i2);
					if (v instanceof Local && u instanceof Local) {
						tv = analysis.getTermOfLocal(v, 1);
						tu = analysis.getTermOfLocal(u, -1);
						e = new Linexpr1(env, new Linterm1[] { tv, tu }, new MpqScalar(0));
						c = new Lincons1(Lincons1.EQ, e);

						Abstract1 fact1 = invokeAbstract.get(i1);
						Abstract1 fact2 = invokeAbstract.get(i2);
						logger.info(v.toString() + " " + fact1.toString());
						logger.info(u.toString() + " " + fact2.toString());

						try {
							Abstract1 join = fact1.meetCopy(man, fact2);
							logger.info(join.toString());

							if (v.equals(u)) {
								logger.info("v equals u");
								if (!join.isBottom(man)) {
									return false;
								}
							} else {
								join.meet(man, c);
								logger.info(join.toString());

								if (!join.isBottom(man)) {
									return false;
								}
							}

						} catch (ApronException e1) {
							// TODO Auto-generated catch block
							logger.info("cannot join");
							e1.printStackTrace();
						}

					} else if (v instanceof Local || u instanceof Local) {
						if (v instanceof Local) {
							tv = analysis.getTermOfLocal(v, 1);
							x = ((IntConstant) u).value;
						} else {
							tv = analysis.getTermOfLocal(u, 1);
							x = ((IntConstant) v).value;
						}
						e = new Linexpr1(env, new Linterm1[] { tv }, new MpqScalar(-x));
						c = new Lincons1(Lincons1.EQ, e);

						Abstract1 fact1 = invokeAbstract.get(i1);
						Abstract1 fact2 = invokeAbstract.get(i2);

						logger.info(fact1.toString());
						logger.info(fact2.toString());

						try {
							Abstract1 join = fact1.meetCopy(man, fact2);
							logger.info(join.toString());

							join.meet(man, c);
							logger.info(join.toString());

							if (!join.isBottom(man)) {
								return false;
							}
						} catch (ApronException e1) {
							// TODO Auto-generated catch block
							logger.info("cannot join");
							e1.printStackTrace();
						}
					} else {
						Abstract1 fact1 = invokeAbstract.get(i1);
						Abstract1 fact2 = invokeAbstract.get(i2);
						x = ((IntConstant) v).value;
						y = ((IntConstant) u).value;

						logger.info(fact1.toString());
						logger.info(fact2.toString());

						try {
							Abstract1 join = fact1.meetCopy(man, fact2);
							logger.info(join.toString());

							if (!join.isBottom(man)) {
								if (x == y) {
									return false;
								}
							}
						} catch (ApronException e1) {
							// TODO Auto-generated catch block
							logger.info("cannot join");
							e1.printStackTrace();
						}

					}

				}
			}
		}
		return true;
	}

}
