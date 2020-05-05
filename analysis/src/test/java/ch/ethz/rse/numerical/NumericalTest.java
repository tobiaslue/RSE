package ch.ethz.rse.numerical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import apron.Abstract1;
import apron.ApronException;
import apron.Interval;
import apron.Lincons1;
import apron.Linterm1;
import apron.MpqScalar;
import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.integration.VerifyTask;
import ch.ethz.rse.pointer.PointsToInitializer;
import fj.Unit;
import fj.data.List;
import javassist.compiler.ast.IntConst;
import pxb.android.arsc.Value;
import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;
import soot.Type;
import soot.JastAddJ.IntType;
import soot.jimple.ArithmeticConstant;
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

public class NumericalTest {
    String packageName = "ch.ethz.rse.integration.tests.A_Numerical_Test";
    VerifyTask t = new VerifyTask(packageName, VerificationProperty.TRACK_NON_NEGATIVE, true);
    SootClass sc = SootHelper.loadClass(t.getTestClass());
    java.util.List<SootMethod> methods = sc.getMethods();
    SootMethod m = methods.get(1);
    UnitGraph g = SootHelper.getUnitGraph(m);
    PointsToInitializer pointsTo = new PointsToInitializer(sc);
    Chain<Local> locals = m.getActiveBody().getLocals();
    NumericalAnalysis analysis = new NumericalAnalysis(m, g, pointsTo);


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

    @Test
    public void test_getTermOfLocal() {
        Local l = locals.getFirst();
        String name = l.getName();
        Linterm1 term = analysis.getTermOfLocal(l, 1);

        assertEquals(new Linterm1(name, new MpqScalar(1)), term);
        assertNotEquals(new Linterm1(name, new MpqScalar(2)), term);
    }

    @Test
    public void test_getConstraint() throws ApronException {
        Local l1 = locals.getFirst();
        Local l2 = locals.getSuccOf(l1);
        Local l3 = locals.getSuccOf(l2);
        Local l4 = locals.getSuccOf(l3);
        String name2 = l2.getName();
        String name3 = l3.getName();
        String name4 = l4.getName();
        IntConstant cons1 = getIntConsant(0);
        IntConstant cons2 = getIntConsant(1);
       
        
        int operator = Lincons1.EQ; 
        Lincons1 c = analysis.getConstraintConditional(l2, l3, operator);
        assertEquals(" 1" + name2 + " -1" + name3 + " = 0", c.toString());

        operator = Lincons1.SUPEQ;
        c = analysis.getConstraintConditional(l2, cons1, operator);
        assertEquals(" 1" + name2 + " -" + cons1.toString() + " >= 0", c.toString());

        operator = Lincons1.SUP;
        c = analysis.getConstraintConditional(cons1, l3, operator);
        assertEquals(" -1" + name3 + " +" + cons1.toString() + " > 0", c.toString());
        
        operator = Lincons1.DISEQ;
        c = analysis.getConstraintConditional(cons1, cons2, operator);
        assertEquals(" " + (cons1.value-cons2.value) + " <> 0", c.toString());

        operator = Lincons1.EQ;
        //a = b
        c = analysis.getConstraintUnOp(l3, l2);
        assertEquals(" 1" + name2 + " -1" + name3 + " = 0", c.toString());
        //a = b + c
        c = analysis.getConstraintBinOp(l2, l3, l4, 1);
        assertEquals(" -1" + name2 + " +1" + name3 + " +1" + name4 + " = 0" , c.toString());
        //a = b - c
        c = analysis.getConstraintBinOp(l3, l4, l2, -1);
        assertEquals(" -1" + name2 + " -1" + name3 + " +1" + name4 + " = 0" , c.toString());
        //b = a + a
        c = analysis.getConstraintBinOp(l4, l3, l3, 1);
        assertEquals(" 2" + name3 + " -1" + name4 + " = 0", c.toString());
        //a = b + 1
        c = analysis.getConstraintBinOp(l4, l2, cons1, 1);
        assertEquals(" 1" + name2 + " -1" + name4 + " +" + cons1.value + " = 0", c.toString());
        //a = b - 1
        c = analysis.getConstraintBinOp(l4, l2, cons1, -1);
        assertEquals(" 1" + name2 + " -1" + name4 + " -" + cons1.value+ " = 0", c.toString());
        //a = 1 - b
        c = analysis.getConstraintBinOp(l2, cons2, l3, -1);
        assertEquals(" -1" + name2 + " -1" + name3 + " +" + cons2.value + " = 0", c.toString());
        //a = 1 + b
        c = analysis.getConstraintBinOp(l2, cons2, l3, 1);
        assertEquals(" -1" + name2 + " +1" + name3 + " +" + cons2.value + " = 0", c.toString());
        //a = 1 + 2
        c = analysis.getConstraintBinOp(l2, cons2, cons1, 1);
        assertEquals(" -1" + name2 + " +" + (cons1.value + cons2.value) + " = 0", c.toString());

        String[] vars = {name4};
        Interval[] intervals = {new Interval(-10, 10)};
        Abstract1 factIn = new Abstract1(analysis.man, analysis.env, vars, intervals);
        //a = b * c
        c = analysis.getConstraintMul(l2, l3, l4, factIn);
        assertEquals(" -1" + name2 + " +[-10,10]" + name4 + " = 0", c.toString());
        //a = b * 1
        c = analysis.getConstraintMul(l2, l3, cons1, factIn);
        assertEquals(" -1" + name2 + " +" + cons1.value + name3 + " = 0", c.toString());
        //a = 1 * b
        c = analysis.getConstraintMul(l3, cons2, l4, factIn);
        assertEquals(" -1" + name3 + " +" + cons2.value + name4 + " = 0", c.toString());
        //a = 1 * b
        c = analysis.getConstraintMul(l3, cons2, cons1, factIn);
        assertEquals(" -1" + name3 + " +" + (cons2.value * cons1.value) + " = 0", c.toString());
    }
}