package ch.ethz.rse.numerical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import apron.Interval;
import apron.Lincons1;
import apron.Linexpr1;
import apron.Linterm1;
import apron.Manager;
import apron.MpqScalar;
import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.integration.VerifyTask;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.qos.logback.core.net.SyslogOutputStream;
import fj.Unit;
import fj.data.List;
import gmp.Mpq;
import jasmin.sym;
import javassist.compiler.ast.IntConst;
import pxb.android.arsc.Value;
import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;
import soot.Type;
import soot.JastAddJ.IntType;
import soot.jimple.ArithmeticConstant;
import soot.jimple.BinopExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;
import soot.jimple.internal.JAddExpr;
import soot.jimple.internal.JSubExpr;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

public class NumericalTest {
    String packageName = "ch.ethz.rse.integration.tests.Numerical_Test";
    VerifyTask t = new VerifyTask(packageName, VerificationProperty.TRACK_NON_NEGATIVE, true);
    SootClass sc = SootHelper.loadClass(t.getTestClass());
    java.util.List<SootMethod> methods = sc.getMethods();
    SootMethod m = methods.get(1);
    UnitGraph g = SootHelper.getUnitGraph(m);
    PointsToInitializer pointsTo = new PointsToInitializer(sc);
    Chain<Local> locals = m.getActiveBody().getLocals();
    NumericalAnalysis analysis = new NumericalAnalysis(m, g, pointsTo, 0);
    Environment env = analysis.env;
    Manager man = analysis.man;

    public IntConstant getIntConsant(int i) {
        int counter = 0;
        for (soot.Unit u : m.getActiveBody().getUnits()) {
            if (u instanceof DefinitionStmt) {
                DefinitionStmt s = (DefinitionStmt) u;
                soot.Value right = s.getRightOp();
                if (right instanceof IntConstant) {
                    if (counter == i) {
                        return (IntConstant) right;
                    }
                    counter++;
                }
            }
        }
        return null;
    }

    public Local getIntLocal(Local start){
        start = locals.getSuccOf(start);
        while(!start.getType().toString().equals("int")){
            start = locals.getSuccOf(start);
        }
        return start;
    }
   
    
	public Lincons1 getConstraintBinOp(soot.Value lhs, soot.Value op1, soot.Value op2, int operation) {
		Linexpr1 e = null;
		Lincons1 c = null;
		Linterm1 lLeft = analysis.getTermOfLocal(lhs, -1);
		Linterm1 lOp1 = null;
		Linterm1 lOp2 = null;
		int x = 0;
		int y = 0;

		if (op1 instanceof Local && op2 instanceof Local) { // Assignment to Binop
			lOp1 = analysis.getTermOfLocal(op1, 1);
			if (op1.equals(op2)) {
				lOp2 = analysis.getTermOfLocal(op2, 2 * operation);
				e = new Linexpr1(env, new Linterm1[] { lLeft, lOp2 }, new MpqScalar(0));
			} else {
				lOp2 = analysis.getTermOfLocal(op2, operation);
				e = new Linexpr1(env, new Linterm1[] { lLeft, lOp1, lOp2 }, new MpqScalar(0));
			}
		} else if (op1 instanceof Local || op2 instanceof Local) {
			if (op1 instanceof Local) {
				lOp1 = analysis.getTermOfLocal(op1, 1);
				x = ((IntConstant) op2).value;
			} else {
				lOp1 = analysis.getTermOfLocal(op2, operation);
				x = operation * ((IntConstant) op1).value;
			}
			e = new Linexpr1(env, new Linterm1[] { lLeft, lOp1 }, new MpqScalar(operation * x));
		} else {
			x = ((IntConstant) op1).value;
			y = ((IntConstant) op2).value;
			e = new Linexpr1(env, new Linterm1[] { lLeft }, new MpqScalar(x + operation * y));
		}

		c = new Lincons1(Lincons1.EQ, e);
		return c;
	}

	
    @Test
    public void testHandleDef() throws ApronException {
        Local l1 = locals.getFirst();
        Local l2 = getIntLocal(l1);
        Local l3 = getIntLocal(l2);
        Local l4 = getIntLocal(l3);
        Linterm1 t2 = new Linterm1(l2.toString(), new MpqScalar(1));
        Linterm1 t3 = new Linterm1(l3.toString(), new MpqScalar(1));
        Linterm1 t4 = new Linterm1(l4.toString(), new MpqScalar(1));
        IntConstant cons1 = getIntConsant(0);
        System.out.println(cons1.toString());
        IntConstant cons2 = getIntConsant(1);
        System.out.println(cons2.toString());



        Lincons1 c1 = analysis.getConstraintConditional(l3, cons2, Lincons1.EQ);
        Lincons1 c2 = getConstraintBinOp(l2, l3, l4, 1);
        Abstract1 fact = new Abstract1(man, new Lincons1[]{c2});
        //fact.meet(man, c1);

        Linexpr1 e2 = null;


        //x = 5
        NumericalStateWrapper wrapper = new NumericalStateWrapper(man, fact);
        System.out.println(wrapper.get().toString());
        analysis.handleDef(wrapper, l3, cons2); 
        t2 = new Linterm1(l2.toString(), new MpqScalar(-1));
        Linexpr1 e1 = new Linexpr1(env, new Linterm1[]{t3}, new MpqScalar(-cons2.value));
        c1 = new Lincons1(Lincons1.EQ, e1);
        Abstract1 expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());

        //x = y
        wrapper = new NumericalStateWrapper(man, fact);
        System.out.println(wrapper.get().toString());
        analysis.handleDef(wrapper, l3, l4);
        t3 = new Linterm1(l3.toString(), new MpqScalar(-1));
        t4 = new Linterm1(l4.toString(), new MpqScalar(1));
        e1 = new Linexpr1(env, new Linterm1[]{t3, t4}, new MpqScalar(0));
        c1 = new Lincons1(Lincons1.EQ, e1);
        expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());

        //x = x+1
        wrapper = new NumericalStateWrapper(man, fact);
        System.out.println(wrapper.get().toString());
        BinopExpr binop = new JAddExpr((soot.Value) l2, (soot.Value) cons1);
        analysis.handleDef(wrapper, l2, binop);
        t2 = new Linterm1(l2.toString(), new MpqScalar(-1));
        t3 = new Linterm1(l3.toString(), new MpqScalar(1));
        t4 = new Linterm1(l4.toString(), new MpqScalar(1));
        e1 = new Linexpr1(env, new Linterm1[]{t2, t3, t4}, new MpqScalar(1));
        c1 = new Lincons1(Lincons1.EQ, e1);
        expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());

        //x = y+1
        wrapper = new NumericalStateWrapper(man, fact);
        System.out.println(wrapper.get().toString());
        binop = new JAddExpr((soot.Value) l3, (soot.Value) cons1);
        analysis.handleDef(wrapper, l2, binop);
        t2 = new Linterm1(l2.toString(), new MpqScalar(-1));
        t3 = new Linterm1(l3.toString(), new MpqScalar(1));
        e1 = new Linexpr1(env, new Linterm1[]{t2, t3}, new MpqScalar(cons1.value));
        c1 = new Lincons1(Lincons1.EQ, e1);
        expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());

        //y = x - z
        wrapper = new NumericalStateWrapper(man, fact);
        System.out.println(wrapper.get().toString());
        binop = new JSubExpr((soot.Value) l4, (soot.Value) l2);
        analysis.handleDef(wrapper, l3, binop);
        t2 = new Linterm1(l2.toString(), new MpqScalar(-1));
        t3 = new Linterm1(l3.toString(), new MpqScalar(-1));
        t4 = new Linterm1(l4.toString(), new MpqScalar(1));
        e1 = new Linexpr1(env, new Linterm1[]{t2, t3, t4}, new MpqScalar(0));
        c1 = new Lincons1(Lincons1.EQ, e1);
        expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());

        //x = x+x
        wrapper = new NumericalStateWrapper(man, fact);
        System.out.println(wrapper.get().toString());
        binop = new JAddExpr((soot.Value) l2, (soot.Value) l2);
        analysis.handleDef(wrapper, l2, binop);
        t4 = new Linterm1(l4.toString(), new MpqScalar(1));
        t3 = new Linterm1(l3.toString(), new MpqScalar(1));
        t2 = new Linterm1(l2.toString(), new MpqScalar(-1, 2));
        e1 = new Linexpr1(env, new Linterm1[]{t2, t3, t4}, new MpqScalar(0));
        System.out.println(e1.toString());
        c1 = new Lincons1(Lincons1.EQ, e1);
        expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());

        //x = x-x
        wrapper = new NumericalStateWrapper(man, fact);
        System.out.println(wrapper.get().toString());
        binop = new JSubExpr((soot.Value) l2, (soot.Value) l2);
        analysis.handleDef(wrapper, l2, binop);
        t2 = new Linterm1(l2.toString(), new MpqScalar(1));
        e1 = new Linexpr1(env, new Linterm1[]{t2}, new MpqScalar(0));
        System.out.println(e1.toString());
        c1 = new Lincons1(Lincons1.EQ, e1);
        expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());


        //c: x-1 = 0
        //x = 5
        Lincons1 c3 = analysis.getConstraintConditional(l3, cons2, Lincons1.EQ);
        Abstract1 fact2 = new Abstract1(man, new Lincons1[]{c3});
        wrapper = new NumericalStateWrapper(man, fact2);
        System.out.println(wrapper.get().toString());
        analysis.handleDef(wrapper, l3, (soot.Value) cons1);
        t3 = new Linterm1(l3.toString(), new MpqScalar(1));
        e1 = new Linexpr1(env, new Linterm1[]{t3}, new MpqScalar(-cons1.value));
        c1 = new Lincons1(Lincons1.EQ, e1);
        expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());

       
        //c: x-1 = 0
        //x = y
        c3 = analysis.getConstraintConditional(l3, cons2, Lincons1.EQ);
        fact2 = new Abstract1(man, new Lincons1[]{c3});
        wrapper = new NumericalStateWrapper(man, fact2);
        System.out.println(wrapper.get().toString());
        analysis.handleDef(wrapper, l3, l2);
        t3 = new Linterm1(l3.toString(), new MpqScalar(1));
        t2 = new Linterm1(l2.toString(), new MpqScalar(-1));
        e1 = new Linexpr1(env, new Linterm1[]{t2, t3}, new MpqScalar(0));
        c1 = new Lincons1(Lincons1.EQ, e1);
        expectedFact = new Abstract1(man, new Lincons1[]{c1});
        assertEquals(expectedFact.toString(), wrapper.get().toString());

        //c: x-1 = 0
        //y = x
        c3 = analysis.getConstraintConditional(l3, cons2, Lincons1.EQ);
        fact2 = new Abstract1(man, new Lincons1[]{c3});
        wrapper = new NumericalStateWrapper(man, fact2);
        System.out.println(wrapper.get().toString());
        analysis.handleDef(wrapper, l2, l3);
        t3 = new Linterm1(l3.toString(), new MpqScalar(1));
        e1 = new Linexpr1(env, new Linterm1[]{t3}, new MpqScalar(-cons2.value));
        c1 = new Lincons1(Lincons1.EQ, e1);
        t2 = new Linterm1(l2.toString(), new MpqScalar(1));
        e2 = new Linexpr1(env, new Linterm1[]{t2}, new MpqScalar(-cons2.value));
        c2 = new Lincons1(Lincons1.EQ, e2);
        expectedFact = new Abstract1(man, new Lincons1[]{c1, c2});
        assertEquals(expectedFact.toString(), wrapper.get().toString());
    }

    // @Test
    // public void test() throws ApronException {
    //     Local l1 = locals.getFirst();
    //     Local l2 = getIntLocal(l1);
    //     Local l3 = getIntLocal(l2);
    //     Local l4 = getIntLocal(l3);

    //     Linterm1 t1 = new Linterm1(l2.toString(), new MpqScalar(1));
    //     Linterm1 t2 = new Linterm1(l3.toString(), new MpqScalar(1));
    //     Linterm1 t4 = new Linterm1(l4.toString(), new MpqScalar(-7));

    //     Linexpr1 e = new Linexpr1(analysis.env, new Linterm1[]{t1}, new MpqScalar(-5));

    //     Lincons1 c = new Lincons1(Lincons1.EQ, e);
    //     Abstract1 a = new Abstract1(analysis.man, new Lincons1[]{c});
    //     System.out.println(a.toString());

    //     Linexpr1 e1 = new Linexpr1(analysis.env, new Linterm1[]{}, new MpqScalar(6));
    //     java.lang.String s = l2.toString();

    //     Abstract1 b = new Abstract1(analysis.man, analysis.env);
    //     b = a.assignCopy(analysis.man, s, e1, b);//use this for x = 4
    //     System.out.println(a.toString());
    //     System.out.println(b.toString());

    //     Abstract1 c1 = new Abstract1(analysis.man, analysis.env);
    //     c1 = a.substituteCopy(analysis.man, s, e1, a);
    //     System.out.println(c1.toString());

    //     Linexpr1 e2 = new Linexpr1(analysis.env, new Linterm1[]{t1}, new MpqScalar(-5));
    //     Lincons1 c2 = new Lincons1(Lincons1.EQ, e2);
        
    //     Abstract1 a2 = new Abstract1(analysis.man, new Lincons1[]{c2});
    //     Linexpr1 e3 = new Linexpr1(analysis.env, new Linterm1[]{t1}, new MpqScalar(1));

    //     System.out.println(c2.toString());
    //     Abstract1 c4 = new Abstract1(analysis.man, analysis.env);

    //     c4 = a2.substituteCopy(analysis.man, l2.toString(), e3, c4); //use this for x = x + 1
    //     System.out.println(c4.toString());


    // }


    // @Test
    // public void test_analysis.getTermOfLocal() {
    //     Local l = locals.getFirst();
    //     String name = l.getName();
    //     Linterm1 term = analysis.getTermOfLocal(l, 1);

    //     assertEquals(new Linterm1(name, new MpqScalar(1)), term);
    //     assertNotEquals(new Linterm1(name, new MpqScalar(2)), term);
    // }

    // @Test
    // public void test_getConstraint() throws ApronException {
    //     Local l1 = locals.getFirst();
    //     Local l2 = getIntLocal(l1);
    //     Local l3 = getIntLocal(l2);
    //     Local l4 = getIntLocal(l3);
    //     String name2 = l2.getName();
    //     String name3 = l3.getName();
    //     String name4 = l4.getName();
    //     IntConstant cons1 = getIntConsant(0);
    //     IntConstant cons2 = getIntConsant(1);
       
    //     int operator = Lincons1.EQ; 
    //     Lincons1 c = analysis.getConstraintConditional(l2, l3, operator);
    //     assertEquals(" 1" + name2 + " -1" + name3 + " = 0", c.toString());

    //     operator = Lincons1.SUPEQ;
    //     c = analysis.getConstraintConditional(l2, cons1, operator);
    //     assertEquals(" 1" + name2 + " -" + cons1.toString() + " >= 0", c.toString());

    //     operator = Lincons1.SUP;
    //     c = analysis.getConstraintConditional(cons1, l3, operator);
    //     assertEquals(" -1" + name3 + " +" + cons1.toString() + " > 0", c.toString());
        
    //     operator = Lincons1.DISEQ;
    //     c = analysis.getConstraintConditional(cons1, cons2, operator);
    //     assertEquals(" " + (cons1.value-cons2.value) + " <> 0", c.toString());

    //     operator = Lincons1.EQ;
    //     //a = b
    //     c = analysis.getConstraintUnOp(l3, l2);
    //     assertEquals(" 1" + name2 + " -1" + name3 + " = 0", c.toString());
    //     //a = b + c
    //     c = analysis.getConstraintBinOp(l2, l3, l4, 1);
    //     assertEquals(" -1" + name2 + " +1" + name3 + " +1" + name4 + " = 0" , c.toString());
    //     //a = b - c
    //     c = analysis.getConstraintBinOp(l3, l4, l2, -1);
    //     assertEquals(" -1" + name2 + " -1" + name3 + " +1" + name4 + " = 0" , c.toString());
    //     //b = a + a
    //     c = analysis.getConstraintBinOp(l4, l3, l3, 1);
    //     assertEquals(" 2" + name3 + " -1" + name4 + " = 0", c.toString());
    //     //a = b + 1
    //     c = analysis.getConstraintBinOp(l4, l2, cons1, 1);
    //     assertEquals(" 1" + name2 + " -1" + name4 + " +" + cons1.value + " = 0", c.toString());
    //     //a = b - 1
    //     c = analysis.getConstraintBinOp(l4, l2, cons1, -1);
    //     assertEquals(" 1" + name2 + " -1" + name4 + " -" + cons1.value+ " = 0", c.toString());
    //     //a = 1 - b
    //     c = analysis.getConstraintBinOp(l2, cons2, l3, -1);
    //     assertEquals(" -1" + name2 + " -1" + name3 + " +" + cons2.value + " = 0", c.toString());
    //     //a = 1 + b
    //     c = analysis.getConstraintBinOp(l2, cons2, l3, 1);
    //     assertEquals(" -1" + name2 + " +1" + name3 + " +" + cons2.value + " = 0", c.toString());
    //     //a = 1 + 2
    //     c = analysis.getConstraintBinOp(l2, cons2, cons1, 1);
    //     assertEquals(" -1" + name2 + " +" + (cons1.value + cons2.value) + " = 0", c.toString());

    //     String[] vars = {name4};
    //     Interval[] intervals = {new Interval(-10, 10)};
    //     Abstract1 factIn = new Abstract1(analysis.man, analysis.env, vars, intervals);
    //     //a = b * c
    //     c = analysis.getConstraintMul(l2, l3, l4, factIn);
    //     assertEquals(" -1" + name2 + " +[-10,10]" + name4 + " = 0", c.toString());
    //     //a = b * 1
    //     c = analysis.getConstraintMul(l2, l3, cons1, factIn);
    //     assertEquals(" -1" + name2 + " +" + cons1.value + name3 + " = 0", c.toString());
    //     //a = 1 * b
    //     c = analysis.getConstraintMul(l3, cons2, l4, factIn);
    //     assertEquals(" -1" + name3 + " +" + cons2.value + name4 + " = 0", c.toString());
    //     //a = 1 * b
    //     c = analysis.getConstraintMul(l3, cons2, cons1, factIn);
    //     assertEquals(" -1" + name3 + " +" + (cons2.value * cons1.value) + " = 0", c.toString());
    //      }
}