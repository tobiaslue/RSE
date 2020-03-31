package ch.ethz.rse.integration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.utils.Constants;

/**
 * Collects all available tasks
 * 
 * DO NOT MODIFY THIS FILE
 */
public class VerifyTaskCollector {

	private static final Logger logger = LoggerFactory.getLogger(VerifyTaskCollector.class);

	/**
	 * Test files are ignore if they contain this string
	 */
	private static final String DISABLED = "DISABLED";

	/**
	 * Properties in this set are not checked
	 */
	private static final Set<VerificationProperty> IGNORED = Sets.newHashSet();

	/**
	 * Search for tests in this package
	 */
	private static final String testPackage = "ch.ethz.rse.integration.tests";

	/**
	 * 
	 * @return a list of all tasks obtained from {@link #testPackage}
	 */
	public static List<VerifyTask> get_tasks() throws IOException {
		String examples_path = System.getProperty("user.dir") + "/src/test/java/" + testPackage.replace(".", "/");
		File examples_dir = new File(examples_path);
		List<VerifyTask> tasks = new LinkedList<VerifyTask>();

		for (File f : examples_dir.listFiles()) {
			String content = Files.asCharSource(f, Charsets.UTF_8).read();

			String className = FilenameUtils.removeExtension(f.getName());
			String packageName = testPackage + "." + className;
			for (VerificationProperty p : VerificationProperty.values()) {
				VerifyTask t = null;
				if (content.contains(p + " " + Constants.safe)) {
					t = new VerifyTask(packageName, p, true);
				} else if (content.contains(p + " " + Constants.unsafe)) {
					t = new VerifyTask(packageName, p, false);
				}
				if (t != null && !isDisabled(t, f, content)) {
					tasks.add(t);
				}
			}
		}

		Collections.sort(tasks);

		assert tasks.size() > 0;

		logger.debug("Collected {} tests from {}", tasks.size(), examples_path);
		return tasks;
	}

	private static boolean isDisabled(VerifyTask t, File f, String content) {
		if (content.contains(DISABLED)) {
			logger.warn("Skipping tests in {}", f);
			return true;
		} else if (IGNORED.contains(t.verificationTask)) {
			logger.warn("Skipping {} in {}", t.verificationTask, f);
			return true;
		}
		return false;
	}

}
