package ch.ethz.rse.numerical;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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
import apron.Linexpr1;
import apron.Linterm1;
import apron.Manager;
import apron.MpqScalar;
import apron.Polka;
import apron.Scalar;
import apron.Tcons1;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import apron.Texpr1Node;
import apron.Texpr1VarNode;
import apron.Var;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.pointer.TrainStationInitializer;
import ch.ethz.rse.utils.Constants;
import ch.ethz.rse.verify.EnvironmentGenerator;
import ch.qos.logback.core.subst.Token.Type;
import polyglot.ast.Branch;
import soot.ArrayType;
import soot.DoubleType;
import soot.IntegerType;
import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.dava.toolkits.base.renamer.infoGatheringAnalysis;
import soot.jimple.AddExpr;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.MulExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.JAddExpr;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JDivExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JMulExpr;
import soot.jimple.internal.JNeExpr;
import soot.jimple.internal.JSubExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;

public class NumericalAnalysis extends ForwardBranchedFlowAnalysis<NumericalStateWrapper> {

	private static final Logger logger = LoggerFactory.getLogger(NumericalAnalysis.class);

	private final SootMethod method;

	public Multimap<TrainStationInitializer, JInvokeStmt> stationInvoke = HashMultimap.create();
	public Map<JInvokeStmt, Abstract1> invokeAbstract = new HashMap<JInvokeStmt, Abstract1>();
	public Map<JInvokeStmt, Value> invokeValue = new HashMap<JInvokeStmt, Value>();

	private final PointsToInitializer pointsTo;

	/**
	 * number of times this loop head was encountered during analysis
	 */
	private HashMap<Unit, IntegerWrapper> loopHeads = new HashMap<Unit, IntegerWrapper>();
	/**
	 * Previously seen abstract state for each loop head
	 */
	private HashMap<Unit, NumericalStateWrapper> loopHeadState = new HashMap<Unit, NumericalStateWrapper>();

	/**
	 * Numerical abstract domain to use for analysis: COnvex polyhedra
	 */
	public final Manager man = new Polka(true);

	public final Environment env;

	/**
	 * We apply widening after updating the state at a given merge point for the
	 * {@link WIDENING_THRESHOLD}th time
	 */
	private static final int WIDENING_THRESHOLD = 6;

	/**
	 * 
	 * @param method   method to analyze
	 * @param g        control flow graph of the method
	 * @param pointsTo result of points-to analysis
	 */
	public NumericalAnalysis(SootMethod method, UnitGraph g, PointsToInitializer pointsTo) {
		super(g);
		this.method = method;
		this.pointsTo = pointsTo;

		this.env = new EnvironmentGenerator(method, this.pointsTo).getEnvironment();

		// initialize counts for loop heads
		for (Loop l : new LoopNestTree(g.getBody())) {
			loopHeads.put(l.getHead(), new IntegerWrapper(0));
		}

		// perform analysis by calling into super-class
		logger.info("Analyzing {} in {}", method.getName(), method.getDeclaringClass().getName());
		
		doAnalysis();
	}

	// Use this constructor for tests....
	public NumericalAnalysis(SootMethod method, UnitGraph g, PointsToInitializer pointsTo, int i) {
		super(g);

		this.method = method;
		this.pointsTo = pointsTo;

		this.env = new EnvironmentGenerator(method, this.pointsTo).getEnvironment();

		// initialize counts for loop heads
		for (Loop l : new LoopNestTree(g.getBody())) {
			loopHeads.put(l.getHead(), new IntegerWrapper(0));
		}

		// perform analysis by calling into super-class

	}

	/**
	 * Report unhandled instructions, types, cases, etc.
	 * 
	 * @param task description of current task
	 * @param what
	 */
	public static void unhandled(String task, Object what, boolean raiseException) {
		String description = task + ": Can't handle " + what.toString() + " of type " + what.getClass().getName();

		if (raiseException) {
			throw new UnsupportedOperationException(description);
		} else {
			logger.error(description);

			// print stack trace
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stackTrace.length; i++) {
				logger.error(stackTrace[i].toString());
			}
		}
	}

	@Override
	protected void copy(NumericalStateWrapper source, NumericalStateWrapper dest) {
		source.copyInto(dest);
	}

	@Override
	protected NumericalStateWrapper newInitialFlow() {
		// should be bottom (only entry flows are not bottom originally)
		return NumericalStateWrapper.bottom(man, env);
	}

	@Override
	protected NumericalStateWrapper entryInitialFlow() {
		// state of entry points into function
		NumericalStateWrapper ret = NumericalStateWrapper.top(man, env);

		return ret;
	}

	@Override
	protected void merge(Unit succNode, NumericalStateWrapper w1, NumericalStateWrapper w2, NumericalStateWrapper w3) {
		IntegerWrapper count = loopHeads.get(succNode);
		
		if (count != null) {
			if (count.value >= WIDENING_THRESHOLD) {
				count.value++;
				w3.set(w1.widen(w2).get());
			} else {
				count.value++;
				w3.set(w1.join(w2).get());
			}
		} else {
			w3.set(w1.join(w2).get());
		}
	}

	@Override
	protected void merge(NumericalStateWrapper src1, NumericalStateWrapper src2, NumericalStateWrapper trg) {
		// this method is never called, we are using the other merge instead
		throw new UnsupportedOperationException();
	}

	public Linterm1 getTermOfLocal(Value v, int sign) {
		String op1Var = ((Local) v).getName();
		Linterm1 t = new Linterm1(op1Var, new MpqScalar(sign));
		return t;
	}

	public Lincons1 getConstraintConditional(Value op1, Value op2, int operator) {
		Linexpr1 e = null;
		Lincons1 c = null;// new Lincons1(env);
		if (op1 instanceof Local && op2 instanceof Local) {
			Linterm1 t1 = getTermOfLocal(op1, 1);
			Linterm1 t2 = getTermOfLocal(op2, -1);
			e = new Linexpr1(env, new Linterm1[] { t1, t2 }, new MpqScalar(0));
		} else if (op1 instanceof Local || op2 instanceof Local) {
			Linterm1 t = null;
			int x = 0;
			logger.info(op1.toString());
			logger.info(op2.toString());
			if (op1 instanceof Local) {
				t = getTermOfLocal(op1, 1);
				x = -((IntConstant) op2).value;
			} else {
				t = getTermOfLocal(op2, -1);
				x = ((IntConstant) op1).value;
			}
			e = new Linexpr1(env, new Linterm1[] { t }, new MpqScalar(x));
		} else {
			int x = ((IntConstant) op1).value;
			int y = ((IntConstant) op2).value;
			e = new Linexpr1(env, new Linterm1[] {}, new MpqScalar(x - y));
		}
		if (e != null) {
			c = new Lincons1(operator, e);

		}
		return c;
	}

	

	@Override
	protected void flowThrough(NumericalStateWrapper inWrapper, Unit op, List<NumericalStateWrapper> fallOutWrappers,
			List<NumericalStateWrapper> branchOutWrappers) {
		logger.debug(inWrapper + " " + op + " => ?");
		// for(Var v: env.getIntVars()){
		// 	logger.info(v.toString());
		// }
		Stmt s = (Stmt) op;
		// wrapper for state after running op, assuming we move to the next statement
		assert fallOutWrappers.size() <= 1;
		NumericalStateWrapper fallOutWrapper = null;
		if (fallOutWrappers.size() == 1) {
			fallOutWrapper = fallOutWrappers.get(0);
			inWrapper.copyInto(fallOutWrapper);
		}

		// wrapper for state after running op, assuming we follow a jump
		assert branchOutWrappers.size() <= 1;
		NumericalStateWrapper branchOutWrapper = null;
		if (branchOutWrappers.size() == 1) {
			branchOutWrapper = branchOutWrappers.get(0);
			inWrapper.copyInto(branchOutWrapper);
		}

		try {

			if (s instanceof DefinitionStmt) {
				// handle assignment
				DefinitionStmt sd = (DefinitionStmt) s;
				Value left = sd.getLeftOp();
				Value right = sd.getRightOp();

				// We are not handling these cases:
				if (!(left instanceof JimpleLocal)) {
					unhandled("Assignment to non-local variable", left, true);
				} else if (left instanceof JArrayRef) {
					unhandled("Assignment to a non-local array variable", left, true);
				} else if (left.getType() instanceof ArrayType) {
					unhandled("Assignment to Array", left, true);
				} else if (left.getType() instanceof DoubleType) {
					unhandled("Assignment to double", left, true);
				} else if (left instanceof JInstanceFieldRef) {
					unhandled("Assignment to field", left, true);
				}

				if (left.getType() instanceof RefType) {
					// assignments to references are handled by pointer analysis
					// no action necessary

					if(left.toString().contains("$r")){
						Abstract1 fact = fallOutWrapper.get();
						Linexpr1 e = new Linexpr1(env, new Linterm1[]{}, new MpqScalar(-1));
						fact.assign(man, left.toString(), e, fact);
						fallOutWrapper.set(fact);
					}
				} else {
					// handle assignment
					handleDef(fallOutWrapper, left, right);
				}

			} else if (s instanceof JIfStmt) {
				// handle if
				// FILL THIS OUT
				logger.info("If Statement");
				Abstract1 factIn = inWrapper.get();
				Value condition = ((JIfStmt) s).getCondition();
				BinopExpr condExp = (BinopExpr) condition;
				Value op1 = condExp.getOp1();
				Value op2 = condExp.getOp2();

				Lincons1 cBranch = null;
				Lincons1 cFall = null;

				if (condition instanceof JEqExpr) {
					cBranch = getConstraintConditional(op1, op2, Lincons1.EQ);
					cFall = getConstraintConditional(op1, op2, Lincons1.DISEQ);
				} else if (condition instanceof JGeExpr) {
					cBranch = getConstraintConditional(op1, op2, Lincons1.SUPEQ);
					cFall = getConstraintConditional(op2, op1, Lincons1.SUP);
				} else if (condition instanceof JGtExpr) {
					cBranch = getConstraintConditional(op1, op2, Lincons1.SUP);
					cFall = getConstraintConditional(op2, op1, Lincons1.SUPEQ);
				} else if (condition instanceof JLeExpr) {
					cBranch = getConstraintConditional(op2, op1, Lincons1.SUPEQ);
					cFall = getConstraintConditional(op1, op2, Lincons1.SUP);
				} else if (condition instanceof JLtExpr) {
					cBranch = getConstraintConditional(op2, op1, Lincons1.SUP);
					cFall = getConstraintConditional(op1, op2, Lincons1.SUPEQ);
				} else if (condition instanceof JNeExpr) {
					cBranch = getConstraintConditional(op1, op2, Lincons1.DISEQ);
					cFall = getConstraintConditional(op1, op2, Lincons1.EQ);
				} else {
					logger.info("Unhandled Condition in if Statement");
				}

				Abstract1 branchFact = factIn.meetCopy(man, cBranch);
				Abstract1 fallFact = factIn.meetCopy(man, cFall);

				branchOutWrapper.set(branchFact);
				fallOutWrapper.set(fallFact);

			} else if (s instanceof JInvokeStmt && ((JInvokeStmt) s).getInvokeExpr() instanceof JVirtualInvokeExpr) {
				// handle invocations
				JInvokeStmt jInvStmt = (JInvokeStmt) s;
				handleInvoke(jInvStmt, fallOutWrapper);
			}
			// log outcome
			if (fallOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[fallout] " + fallOutWrapper);
			}
			if (branchOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[branchout] " + branchOutWrapper);
			}

		} catch (ApronException e) {
			throw new RuntimeException(e);
		}
	}

	public int scalarToInt(Scalar x){
		double[] r = {0};
		x.toDouble(r, 1);
		return (int) r[0];
	}

	public Interval joinIntervals(Interval i1, Interval i2){
		int max = 0;
		int min = 0;
		if(scalarToInt(i1.sup) > scalarToInt(i2.sup)){
			max = scalarToInt(i1.sup);
		} else{
			max = scalarToInt(i2.sup);
		}

		if(scalarToInt(i1.inf) < scalarToInt(i2.inf)){
			min = scalarToInt(i1.inf);
		} else{
			min = scalarToInt(i2.inf);
		}

		Interval i = new Interval(min, max);
		return i;
	}

	public void handleInvoke(JInvokeStmt jInvStmt1, NumericalStateWrapper fallOutWrapper) throws ApronException {
	
		JInvokeStmt jInvStmt = (JInvokeStmt) jInvStmt1.clone();


		InvokeExpr e = jInvStmt.getInvokeExpr();
		Value par = e.getUseBoxes().get(0).getValue();

		List<ValueBox> boxes = e.getUseBoxes();
		JimpleLocalBox left = null;
		for (ValueBox b : boxes) {
			if (b instanceof JimpleLocalBox) {
				left = (JimpleLocalBox) b;
			}
		}

		Local base = (Local) left.getValue();
		List<TrainStationInitializer> stations = pointsTo.pointsTo(base);
		logger.info(stations.toString());
		
		for (TrainStationInitializer t : stations) {
			stationInvoke.put(t, jInvStmt);
			if(par instanceof IntConstant){
				Abstract1 fact = fallOutWrapper.get();
				int x = ((IntConstant) par).value;
				Interval i = new Interval(x, x);
				Interval stationValues = fact.getBound(man, t.getVar());
				Interval newStationValues = joinIntervals(i, stationValues);
				Linterm1 l = new Linterm1(t.getVar(), new MpqScalar(-1));
				Linexpr1 ex = new Linexpr1(env, new Linterm1[]{l}, newStationValues);
				Lincons1 c = new Lincons1(Lincons1.EQ, ex);
				fact.forget(man, t.getVar(), false);
				fact.meet(man, new Lincons1[]{c});
				fallOutWrapper.set(fact);
				t.addInvoke(jInvStmt);
			}else if(par instanceof Local){
				Abstract1 fact = fallOutWrapper.get();
				Interval i = fact.getBound(man, par.toString());
				Interval stationValues = fact.getBound(man, t.getVar());
				Interval newStationValues = joinIntervals(i, stationValues);
				Linterm1 l = new Linterm1(t.getVar(), new MpqScalar(-1));
				Linexpr1 ex = new Linexpr1(env, new Linterm1[]{l}, newStationValues);
				Lincons1 c = new Lincons1(Lincons1.EQ, ex);
				fact.forget(man, t.getVar(), false);
				fact.meet(man, new Lincons1[]{c});
				fallOutWrapper.set(fact);
				t.addInvoke(jInvStmt);
			}
		}

		invokeAbstract.put(jInvStmt, fallOutWrapper.copy().get());
		invokeValue.put(jInvStmt, par);

	}

	public Abstract1 getFact(Abstract1 fact, Local lhs, Value op1, Value op2, int operation) {
		String varLeft = lhs.getName();
		try {
			Abstract1 factOut = new Abstract1(man, env);
			if (op1 instanceof Local && op2 instanceof Local) {
				Local op1Local = (Local) op1;
				Local op2Local = (Local) op2;
				if (op1Local.equals(lhs) && op2Local.equals(lhs)) {
					Linterm1 t = null;
					Linexpr1 e = null;
					if (operation == 1) {
						t = new Linterm1(op1Local.getName(), new MpqScalar(1, 2));
						e = new Linexpr1(env, new Linterm1[] { t }, new MpqScalar(0));
						factOut = fact.substituteCopy(man, varLeft, e, factOut);
					} else {
						t = new Linterm1(op1Local.getName(), new MpqScalar(0));
						e = new Linexpr1(env, new Linterm1[] { t }, new MpqScalar(0));
						factOut = fact.forgetCopy(man, varLeft, false);
						factOut.assign(man, varLeft, e, factOut);
					}
					logger.info(lhs.toString() + " = " + op1.toString() + " " + Integer.toString(operation) + " "
							+ op2.toString());
				} else if (op1Local.equals(lhs) || op2Local.equals(lhs)) {
					Linterm1 t1 = new Linterm1(op1Local.getName(), new MpqScalar(1));
					Linterm1 t2 = new Linterm1(op2Local.getName(), new MpqScalar(operation));
					Linexpr1 e = new Linexpr1(env, new Linterm1[] { t1, t2 }, new MpqScalar(0));
					factOut = fact.substituteCopy(man, varLeft, e, factOut);
				} else {
					Linterm1 t1 = new Linterm1(op1Local.getName(), new MpqScalar(1));
					Linterm1 t2 = new Linterm1(op2Local.getName(), new MpqScalar(operation));
					Linexpr1 e = new Linexpr1(env, new Linterm1[] { t1, t2 }, new MpqScalar(0));
					factOut = fact.forgetCopy(man, varLeft, false);
					factOut.assign(man, varLeft, e, factOut);
					logger.info(lhs.toString() + " = " + op1.toString() + " " + Integer.toString(operation) + " "
							+ op2.toString());
				}
			} else if (op1 instanceof Local || op2 instanceof Local) {
				Local l = null;
				int x = 0;
				if (op1 instanceof Local) {
					l = (Local) op1;
					x = ((IntConstant) op2).value;
				} else {
					l = (Local) op2;
					x = ((IntConstant) op1).value;
				}
				if (l.equals(lhs)) {
					Linterm1 t = new Linterm1(l.getName(), new MpqScalar(1));
					Linexpr1 e = new Linexpr1(env, new Linterm1[] { t }, new MpqScalar(-1 * operation * x));
					factOut = fact.substituteCopy(man, varLeft, e, factOut);
					logger.info(lhs.toString() + " = " + op1.toString() + " + " + op2.toString());
				} else {
					Linterm1 t = new Linterm1(l.getName(), new MpqScalar(1));
					Linexpr1 e = new Linexpr1(env, new Linterm1[] { t }, new MpqScalar(operation * x));
					factOut = fact.forgetCopy(man, varLeft, false);
					factOut.assign(man, varLeft, e, factOut);
					logger.info(lhs.toString() + " = " + op1.toString() + " + " + op2.toString());
				}
			} else {
				int x = ((IntConstant) op1).value;
				int y = ((IntConstant) op2).value;
				Linexpr1 e = new Linexpr1(env, new Linterm1[] {}, new MpqScalar(x + operation * y));
				factOut = fact.forgetCopy(man, varLeft, false);
				factOut.assign(man, varLeft, e, factOut);
			}
			return factOut;
		} catch (ApronException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Abstract1 getFactMul(Abstract1 fact, Local lhs, Value op1, Value op2) {
		String varLeft = lhs.getName();
		try {
			logger.info("TRY");
			Abstract1 factOut = new Abstract1(man, env);
			if (op1 instanceof Local && op2 instanceof Local) {
				logger.info("2 local");

				Local op1Local = (Local) op1;
				Local op2Local = (Local) op2;

				if (op1Local.equals(lhs) || op2Local.equals(lhs)) {
					Interval i = fact.getBound(man, op1Local.getName());
					Linterm1 t = new Linterm1(op2Local.getName(), i);
					Linexpr1 e = new Linexpr1(env, new Linterm1[] { t }, new MpqScalar(0));
					factOut = fact.substituteCopy(man, varLeft, e, factOut);
				} else {
					logger.info("not equal");

					Interval i = fact.getBound(man, op1Local.getName());
					logger.info("interval");

					Linterm1 t = new Linterm1(op2Local.getName(), i);
					logger.info("linterm");

					Linterm1 leftTerm = new Linterm1(varLeft, new MpqScalar(-1));

					Linexpr1 e = new Linexpr1(env, new Linterm1[] { t, leftTerm }, new MpqScalar(0));
					
					Lincons1 c = new Lincons1(Lincons1.EQ, e);
					logger.info("linexpr");
					factOut = fact.forgetCopy(man, varLeft, false);
					logger.info("forget");

					factOut.meet(man, c);
					logger.info("assign");

				}
			} else if (op1 instanceof Local || op2 instanceof Local) {
				Local l = null;
				int x = 0;
				if (op1 instanceof Local) {
					l = (Local) op1;
					x = ((IntConstant) op2).value;
				} else {
					l = (Local) op2;
					x = ((IntConstant) op1).value;
				}
				if (l.equals(lhs)) {
					Linterm1 t = new Linterm1(l.getName(), new MpqScalar(x));
					Linexpr1 e = new Linexpr1(env, new Linterm1[] { t }, new MpqScalar(0));
					factOut = fact.substituteCopy(man, varLeft, e, factOut);
				} else {
					Linterm1 t = new Linterm1(l.getName(), new MpqScalar(x));
					Linexpr1 e = new Linexpr1(env, new Linterm1[] { t, }, new MpqScalar(0));
					logger.info("exp: " + e.toString());
					factOut = fact.forgetCopy(man, varLeft, false);
					logger.info(factOut.toString());
					factOut.assign(man, varLeft, e, factOut);
					logger.info(factOut.toString());

				}
			} else {
				int x = ((IntConstant) op1).value;
				int y = ((IntConstant) op2).value;
				Linexpr1 e = new Linexpr1(env, new Linterm1[] {}, new MpqScalar(x * y));
				factOut = fact.forgetCopy(man, varLeft, false);
				factOut.assign(man, varLeft, e, factOut);
			}
			return factOut;
		} catch (ApronException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 *
	 * handle assignment
	 *
	 * @param in
	 * @param left
	 * @param right
	 * @return state of in after assignment
	 */
	public void handleDef(NumericalStateWrapper outWrapper, Value left, Value right) throws ApronException {
		// FILL THIS OUT
		logger.info("Def");
		Abstract1 fact = outWrapper.get();
		Abstract1 factIn = new Abstract1(man, env);
		Lincons1 c = null;
		String varLeft = ((Local) left).getName();
		Local lhs = (Local) left;

		if (right instanceof IntConstant) {
			int x = ((IntConstant) right).value;
			Linexpr1 e = new Linexpr1(env, new Linterm1[] {}, new MpqScalar(x));
			logger.info(fact.toString());
			factIn = fact.forgetCopy(man, varLeft, false);
			logger.info(fact.toString());
			logger.info(factIn.toString());
			factIn.assign(man, varLeft, e, factIn);
			logger.info(fact.toString());
			logger.info(factIn.toString());

		} else if (right instanceof Local) {
			String varRight = ((Local) right).getName();
			Linterm1 t = new Linterm1(varRight, new MpqScalar(1));
			Linexpr1 e = new Linexpr1(env, new Linterm1[] { t }, new MpqScalar(0));
			logger.info(fact.toString());
			factIn = fact.forgetCopy(man, varLeft, false);
			factIn.assign(man, varLeft, e, factIn);
			logger.info(left.toString() + " = " + right.toString());
			logger.info(factIn.toString());

		} else if (right instanceof BinopExpr) {
			logger.info("BINOP");
			BinopExpr binop = (BinopExpr) right;
			Value op1 = binop.getOp1();
			Value op2 = binop.getOp2();

			if (right instanceof JAddExpr) {
				logger.info(fact.toString());
				factIn = getFact(fact, lhs, op1, op2, 1);
				logger.info(factIn.toString());
			} else if (right instanceof JSubExpr) {
				logger.info(fact.toString());
				factIn = getFact(fact, lhs, op1, op2, -1);
				logger.info(factIn.toString());
			} else if (right instanceof JMulExpr) {
				logger.info("MUL");
				factIn = getFactMul(fact, lhs, op1, op2);
			}
		} else {
			logger.info("Other def");
			Interval interval = new Interval();
			interval.setTop();
			factIn = new Abstract1(man, env, new String[] { ((Local) left).getName() }, new Interval[] { interval });
		}

		outWrapper.set(factIn);
	}
}
