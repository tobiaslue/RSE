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

				for (JInvokeStmt stmt : invokes) {
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
		// go through list of invokes for every train station
		// if new invoke may cause crash => return false
		for (SootMethod m : numericalAnalysis.keySet()) {
			NumericalAnalysis analysis = numericalAnalysis.get(m);
			apron.Manager man = analysis.man;
			Environment env = analysis.env;

			Collection<TrainStationInitializer> stations = pointsTo.getStationByMethod(m);
			Multimap<TrainStationInitializer, JInvokeStmt> stationInvoke = analysis.stationInvoke;
			Map<JInvokeStmt, Abstract1> invokeAbstract = analysis.invokeAbstract;
			Map<JInvokeStmt, Value> invokeValue = analysis.invokeValue;

			for (TrainStationInitializer s : stations) {
				Collection<JInvokeStmt> invokes = s.getInvokes();

				for (JInvokeStmt stmt : invokes) {
					Value par = invokeValue.get(stmt);

					if (par instanceof Local) {
						Linterm1 lint = analysis.getTermOfLocal((Local) par, 1);
						Linexpr1 line = new Linexpr1(env, new Linterm1[] { lint }, new MpqScalar(-s.nTracks + 1));
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
						if (x > s.nTracks-1) {
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

		for (SootMethod m : numericalAnalysis.keySet()) {
			NumericalAnalysis analysis = numericalAnalysis.get(m);
			apron.Manager man = analysis.man;
			Environment env = analysis.env;

			Collection<TrainStationInitializer> stations = pointsTo.getStationByMethod(m);
			Map<JInvokeStmt, Abstract1> invokeAbstract = analysis.invokeAbstract;
			Map<JInvokeStmt, Value> invokeValue = analysis.invokeValue;

			for (TrainStationInitializer station : stations) {
				List<JInvokeStmt> invokes = station.getInvokes();

				int length = invokes.size();
				for (int i = 0; i < length - 1; i++) {
					JInvokeStmt jOld = invokes.get(i);
					JInvokeStmt jNew = invokes.get(i + 1);

					Abstract1 oldFact = invokeAbstract.get(jOld);
					Abstract1 newFact = invokeAbstract.get(jNew);
					Value newValue = invokeValue.get(jNew);



					if (newValue instanceof IntConstant) {
						int x = ((IntConstant) newValue).value;
						Linterm1 lt = new Linterm1(station.getVar(), new MpqScalar(-1));
						Linexpr1 e = new Linexpr1(env, new Linterm1[] { lt }, new MpqScalar(x));
						Lincons1 c = new Lincons1(Lincons1.EQ, e);
						try {
							newFact.forget(man, station.getVar(), false);
							oldFact.meet(man, newFact);
							oldFact.meet(man, c);
							// logger.info("Old Fact " + oldFact.toString());
							// logger.info("New Value " + newValue.toString());
							// logger.info("Meet " + oldFact.toString());
							if (!oldFact.isBottom(man)) {
								return false;
							}
						} catch (ApronException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

					} else if (newValue instanceof Local) {
						try {
							Interval interval = newFact.getBound(man, newValue.toString());
							logger.info("Interval " + interval.toString());
							Linterm1 lt = new Linterm1(station.getVar(), new MpqScalar(-1));
							Linexpr1 e = new Linexpr1(env, new Linterm1[]{lt}, interval);	
							Lincons1 c = new Lincons1(Lincons1.EQ, e);
							newFact.forget(man, station.getVar(), false);
							oldFact.meet(man, newFact);
							oldFact.meet(man, c);
							logger.info("Old Fact " + oldFact.toString());
							logger.info("New Value " + newValue.toString());
							logger.info("C " + c.toString());
							logger.info("Meet " + oldFact.toString());
							if (!oldFact.isBottom(man)) {
								return false;
							}
						} catch (ApronException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}

				}
			}

		}
		return true;
	}

}
