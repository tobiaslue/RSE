package soot;

import org.junit.jupiter.api.Test;

import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.integration.VerifyTask;

/**
 * Sanity check on classes loaded by soot
 */
public class SootHelperTest {

	@Test
	public void testLoadTest() {
		String packageName = "ch.ethz.rse.integration.tests.Basic_Test_Safe";
		VerifyTask c = new VerifyTask(packageName, VerificationProperty.TRACK_NON_NEGATIVE, true);
		SootHelper.loadClass(c.getTestClass());
	}

}