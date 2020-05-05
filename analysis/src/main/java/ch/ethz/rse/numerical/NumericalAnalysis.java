package ch.ethz.rse.numerical;

import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

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
import soot.dava.toolkits.base.renamer.infoGatheringAnalysis;
import soot.jimple.AddExpr;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
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
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;

public class NumericalAnalysis extends ForwardBranchedFlowAnalysis<NumericalStateWrapper> {

	private static final Logger logger = LoggerFactory.getLogger(NumericalAnalysis.class);

	private final SootMethod method;

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
		logger.debug("in merge: " + succNode);

		logger.debug("join: ");
		NumericalStateWrapper w3_new = w1.join(w2);
		w3.set(w3_new.get());

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
		Lincons1 c = null;//new Lincons1(env);
		if (op1 instanceof Local && op2 instanceof Local) {
			Linterm1 t1 = getTermOfLocal(op1, 1);
			Linterm1 t2 = getTermOfLocal(op2, -1);
			for (Var i : env.getIntVars()){
				logger.info(i.toString());
			}
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
		if(e != null){
			c = new Lincons1(operator, e);

		}
		return c;
	}

	public Lincons1 getConstraintUnOp(Value lhs, Value op) {
		Linexpr1 e = null;
		Lincons1 c = null;
		Linterm1 lLeft = getTermOfLocal(lhs, -1);
		Linterm1 lOp = null;
		if (op instanceof IntConstant) {
			int value = ((IntConstant) op).value;
			e = new Linexpr1(env, new Linterm1[] { lLeft }, new MpqScalar(value));
		} else if (op instanceof Local) {
			lOp = getTermOfLocal(op, 1);
			e = new Linexpr1(env, new Linterm1[] { lLeft, lOp }, new MpqScalar(0));
		}
		c = new Lincons1(Lincons1.EQ, e);
		return c;
	}

	public Lincons1 getConstraintBinOp(Value lhs, Value op1, Value op2, int operation){
		Linexpr1 e = null;
		Lincons1 c = null;
		Linterm1 lLeft = getTermOfLocal(lhs, -1);
		Linterm1 lOp1 = null;
		Linterm1 lOp2 = null;
		int x = 0;
		int y = 0;

		if(op1 instanceof Local && op2 instanceof Local){ //Assignment to Binop
			lOp1 = getTermOfLocal(op1, 1);
			if(op1.equals(op2)){
				lOp2 = getTermOfLocal(op2, 2*operation);
				e = new Linexpr1(env, new Linterm1[] {lLeft, lOp2}, new MpqScalar(0));
			} else {
				lOp2 = getTermOfLocal(op2, operation);
				e = new Linexpr1(env, new Linterm1[] {lLeft, lOp1, lOp2}, new MpqScalar(0));
			}
		} else if(op1 instanceof Local || op2 instanceof Local){
			if(op1 instanceof Local){
				lOp1 = getTermOfLocal(op1, 1);
				x = ((IntConstant)op2).value;
			} else{
				lOp1 = getTermOfLocal(op2, operation);
				x = operation*((IntConstant)op1).value;
			}
			e = new Linexpr1(env, new Linterm1[] {lLeft, lOp1}, new MpqScalar(operation*x));
		} else{
			x = ((IntConstant)op1).value;
			y = ((IntConstant)op2).value;
			e = new Linexpr1(env, new Linterm1[] {lLeft}, new MpqScalar(x+operation*y));	
		}

		c = new Lincons1(Lincons1.EQ,e);
		return c;
	}

	public Lincons1 getConstraintMul(Value lhs, Value op1, Value op2, Abstract1 factIn)
			throws ApronException {
		Linexpr1 e = null;
		Lincons1 c = null;
		Linterm1 lLeft = getTermOfLocal(lhs, -1);
		Interval lOp1 = null;
		Linterm1 lOp2 = null;
		int x = 0; 
		int y = 0;
			
		if(op1 instanceof Local && op2 instanceof Local){
			lOp1 = factIn.getBound(man, ((Local)op2).getName());
			lOp2 = new Linterm1(((Local)op2).getName(), lOp1);
			e = new Linexpr1(env, new Linterm1[] {lLeft, lOp2}, new MpqScalar(0));
		}else if(op1 instanceof Local || op2 instanceof Local){
			if(op1 instanceof Local){
				x = ((IntConstant)op2).value;
				lOp2 = getTermOfLocal(op1, x);
			} else{
				x = ((IntConstant)op1).value;
				lOp2 = getTermOfLocal(op2, x);
			}
			e = new Linexpr1(env, new Linterm1[] {lLeft, lOp2}, new MpqScalar(0));
		}else{
			x = ((IntConstant)op1).value;
			y = ((IntConstant)op2).value;
			e = new Linexpr1(env, new Linterm1[] {lLeft}, new MpqScalar(x*y));
		}
		
		c = new Lincons1(Lincons1.EQ, e);
		return c;
	}

	@Override
	protected void flowThrough(NumericalStateWrapper inWrapper, Unit op, List<NumericalStateWrapper> fallOutWrappers,
			List<NumericalStateWrapper> branchOutWrappers) {
		logger.debug(inWrapper + " " + op + " => ?");
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
				} else {
					// handle assignment
					handleDef(fallOutWrapper, left, right);
				}

			} else if (s instanceof JIfStmt) {
				// handle if
				// FILL THIS OUT
				Abstract1 factIn = inWrapper.get();
				Value condition = ((JIfStmt) s).getCondition();
				BinopExpr condExp = (BinopExpr) condition;
				Value op1 = condExp.getOp1();
				Value op2 = condExp.getOp2();

				// In case something unexpected happens, we keep the incoming fact
				fallOutWrapper.set(inWrapper.get());
				branchOutWrapper.set(inWrapper.get());

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

	public void handleInvoke(JInvokeStmt jInvStmt, NumericalStateWrapper fallOutWrapper) throws ApronException {
		// FILL THIS OUT
		Interval interval = new Interval();
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
	private void handleDef(NumericalStateWrapper outWrapper, Value left, Value right) throws ApronException {
		// FILL THIS OUT
		Abstract1 factIn = outWrapper.get();
		Lincons1 c = null;

		if(right instanceof IntConstant || right instanceof Local){
			c = getConstraintUnOp(left, right);
			factIn.meet(man, c);
		}else if(right instanceof BinopExpr){
			BinopExpr binop = (BinopExpr) right;
			Value op1 = binop.getOp1();
			Value op2 = binop.getOp2();
			if(right instanceof JAddExpr){
				c = getConstraintBinOp(left, op1, op2, 1);
			} else if(right instanceof JSubExpr){
				c = getConstraintBinOp(left, op1, op2, -1);
			} else if(right instanceof JMulExpr){
				c = getConstraintMul(left, op1, op2, factIn);
			}
			factIn.meet(man, c);
		}else {
			logger.info("Other def");
			Interval interval = new Interval();
			interval.setTop();
			outWrapper.set(new Abstract1(man, env, new String[] {((Local)left).getName()}, new Interval[]{interval}));
		}

		
		outWrapper.set(factIn);
	}
}
