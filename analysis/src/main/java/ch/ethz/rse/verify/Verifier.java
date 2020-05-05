package ch.ethz.rse.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocalBox;
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

	}


	@Override
	public boolean checkTrackNonNegative() {
		List<SootMethod> methods = c.getMethods();
		SootMethod m = methods.get(1);
		UnitGraph g = SootHelper.getUnitGraph(m);
		NumericalAnalysis analysis = new NumericalAnalysis(m, g, pointsTo);

		apron.Manager man = analysis.man;
		Environment env = analysis.env;

		for(Unit u : m.getActiveBody().getUnits()){
			if(u instanceof JInvokeStmt){
				InvokeExpr e = ((JInvokeStmt)u).getInvokeExpr();
				Value par = ((ValueBox)e.getUseBoxes().get(0)).getValue();			
				if(e.getMethod().getName().equals("arrive")){
					NumericalStateWrapper before = analysis.getFlowBefore(u);
					try {
						Linexpr1 le = null;
						if(par instanceof IntConstant){
							int x = ((IntConstant)par).value;
							if (x < 0){
								return false;
							}
						} else{
							Linterm1 lt = analysis.getTermOfLocal(par, -1);
							le = new Linexpr1(env, new Linterm1[] {lt}, new MpqScalar(0));
							Lincons1 c = new Lincons1(Lincons1.SUPEQ, le);
							logger.info(e.toString());
							logger.info(c.toString());
							Abstract1 factIn = before.get();
							factIn.meet(man, c);
							if(!factIn.isBottom(man)){
								return false;
							}//arrive(x) 0< 0-x
						}
						
					} catch (ApronException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
		// try {
		// 	Abstract1 e = flowBefore.get();

		// 	logger.info(Boolean.toString(e.satisfy(man, new Lincons1(env, new Lincons0(0, new Linexpr0(0, new MpqScalar(0))))));
		// 	if(flowBefore.get().getBound(man, var[0]).inf().sgn() == 1){
		// 		return true;
		// 	}
		// } catch (ApronException e) {
		// 	// TODO Auto-generated catch block
		// 	e.printStackTrace();
		// }


		return true;
	}

	@Override
	public boolean checkTrackInRange() {
		List<SootMethod> methods = c.getMethods();
		SootMethod m = methods.get(1);
		UnitGraph g = SootHelper.getUnitGraph(m);
		NumericalAnalysis analysis = new NumericalAnalysis(m, g, pointsTo);

		apron.Manager man = analysis.man;
		Environment env = analysis.env;

		for(Unit u : m.getActiveBody().getUnits()){
			NumericalStateWrapper before = analysis.getFlowBefore(u);
			Abstract1 factIn = before.get();
			if(u instanceof JInvokeStmt){
				logger.info("Invoke");
				InvokeExpr e = ((JInvokeStmt)u).getInvokeExpr();
				Value par = ((ValueBox)e.getUseBoxes().get(0)).getValue();
				logger.info(e.getMethod().getName());			
				if(e.getMethod().getName().equals("arrive")){	
					logger.info("arrive");				
					logger.info(e.toString());
					List<ValueBox> boxes = e.getUseBoxes();
					JimpleLocalBox left = null;
						
					for(ValueBox b:boxes){
						if(b instanceof JimpleLocalBox){
							left = (JimpleLocalBox) b;
						}
					}
					Local base = (Local) left.getValue();
					List<TrainStationInitializer> stations = pointsTo.pointsTo(base);
					for(TrainStationInitializer t : stations){
						if(par instanceof Local){
							Linterm1 lint = analysis.getTermOfLocal((Local) par, -1);
							Linexpr1 line = new Linexpr1(env, new Linterm1[] {lint}, new MpqScalar(t.nTracks));
							Lincons1 c = new Lincons1(Lincons1.SUP, line);
							logger.info(c.toString());
							try{
								factIn.meet(man, c);
								if (!factIn.isBottom(man)) {
									return false;
								}
							} catch (Exception ex) {
								logger.info("Cannot Join");
							}
						
						}else if(par instanceof IntConstant){
							int x = ((IntConstant) par).value;
							logger.info(Integer.toString(x) + " " + Integer.toString(t.nTracks));
							if(x > t.nTracks){
								return false;
							}
						}
					}
					
				}
			}
		}
		
		return true;
	}

	@Override
	public boolean checkNoCrash() {
		return true;
	}


}
