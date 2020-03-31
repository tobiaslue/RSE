package ch.ethz.rse.integration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.rse.VerificationResult;
import ch.ethz.rse.utils.Configuration;
import ch.ethz.rse.verify.AVerifier;
import ch.ethz.rse.verify.ClassToVerify;
import ch.ethz.rse.verify.Verifier;
import soot.SootClass;
import soot.SootHelper;

/**
 * Test the code on all provided examples
 * 
 * DO NOT MODIFY THIS FILE
 * 
 */
public class AllExamplesIT {

	private static final Logger logger = LoggerFactory.getLogger(AllExamplesIT.class);

	/**
	 * Available time for a single verification
	 */
	private final int TIMEOUT = Configuration.props.getTimeout();

	/**
	 * Running tests asynchronously allows us to enforce the {@link #TIMEOUT}
	 */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	/**
	 * 
	 * @return all available tasks
	 */
	public static List<VerifyTask> get_tasks() throws IOException {
		return VerifyTaskCollector.get_tasks();
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("get_tasks")
	void testExampleClass(VerifyTask example) {
		logger.info("Testing {}", example.toString());
		ClassToVerify tc = example.getTestClass();

		// load analyzed class
		SootClass c = SootHelper.loadClass(tc);

		// prepare execution of verification
		final Future<Boolean> handler = executor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				// verify
				AVerifier v = new Verifier(c);
				return v.check(example.verificationTask);
			}
		});

		// run verification
		VerificationResult actual;
		try {
			boolean isSafe = handler.get(TIMEOUT, TimeUnit.SECONDS);
			actual = new VerificationResult(isSafe);
		} catch (Throwable e) {
			handler.cancel(true);
			logger.error("Error running verification:", e);
			actual = new VerificationResult(e.getClass().getSimpleName());
		} finally {
			executor.shutdownNow();
		}

		// check result
		this.compare(example.toString(), example.expected, actual);
		Assertions.assertEquals(example.expected, actual);
	}

	// WRITING RESULTS

	private static final File resultsFile = new File(Configuration.props.getBasedir() + "/results/results.txt");

	@BeforeAll
	public static void setUpResultsFile() throws IOException {
		FileUtils.writeStringToFile(resultsFile, "", Charset.defaultCharset(), false);
		writeToResultsFile("label comparison expected actual");
	}

	private void compare(String label, VerificationResult expected, VerificationResult actual) {
		assert expected.error == null;
		String cmp = actual.compare(expected);
		String summary = String.format("%-24s %-9s (expected:%-6s,got:%-6s)", label, cmp, expected.toString(),
				actual.toString());
		logger.info(summary);
		String result = String.format("%s %s %s %s", label, cmp, expected.toString(), actual.toString());
		writeToResultsFile(result);
	}

	private static void writeToResultsFile(String line) {
		try {
			FileUtils.writeStringToFile(resultsFile, line + "\n", Charset.defaultCharset(), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
