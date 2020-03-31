package ch.ethz.rse.pointer;

import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Iterators;

import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.integration.VerifyTask;
import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.jimple.spark.pag.Node;

/**
 * Sanity checks on points-to-analysis
 */
public class PointsToAnalysisWrapperTest {

	@Test
	public void testPointer() {
		String packageName = "ch.ethz.rse.integration.tests.Basic_Test_Safe";
		VerifyTask t = new VerifyTask(packageName, VerificationProperty.TRACK_NON_NEGATIVE, true);
		SootClass sc = SootHelper.loadClass(t.getTestClass());
		// run points-to analysis
		PointsToAnalysisWrapper w = new PointsToAnalysisWrapper(sc);

		// check that pointer indeed points to an abstract object
		Body b = sc.getMethodByName("m1").retrieveActiveBody();
		Local s = Iterators.get(b.getLocals().iterator(), 1);
		Collection<Node> pointsTo = w.getNodes(s);
		Assertions.assertEquals(1, pointsTo.size());
	}

}