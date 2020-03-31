package ch.ethz.rse.integration;

import java.io.File;
import java.io.FileNotFoundException;

import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.VerificationResult;
import ch.ethz.rse.utils.Configuration;
import ch.ethz.rse.verify.ClassToVerify;

/**
 * Convenience wrapper that describes a specific verification task
 * 
 * DO NOT MODIFY THIS FILE
 */
public class VerifyTask implements Comparable<VerifyTask> {

	private final String packageName;
	private final ClassToVerify tc;
	public final VerificationProperty verificationTask;
	public final VerificationResult expected;

	public VerifyTask(String packageName, VerificationProperty verificationTask, boolean expectedIsSafe) {
		try {
			this.packageName = packageName;
			String basedir = Configuration.props.getBasedir();
			File classPath = new File(basedir + "/target/test-classes");
			this.tc = new ClassToVerify(classPath, packageName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Did you compile your tests, e.g., using `mvn test-compile`?", e);
		}
		this.verificationTask = verificationTask;
		this.expected = new VerificationResult(expectedIsSafe);
	}

	public ClassToVerify getTestClass() {
		return this.tc;
	}

	@Override
	public String toString() {
		return this.tc.getName() + ":" + this.verificationTask.toString();
	}

	@Override
	public int compareTo(VerifyTask w) {
		// comparison function to allow sorting
		int cmp = this.packageName.compareTo(w.packageName);
		if (cmp == 0) {
			return this.verificationTask.compareTo(w.verificationTask);
		} else {
			return cmp;
		}
	}

}
