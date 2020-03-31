package ch.ethz.rse.security;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessControlException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.ethz.rse.utils.Configuration;
import ch.ethz.rse.utils.FileUtilsWrapper;

/**
 * Multiple tests checking that the security manager blocks access to critical
 * resources.
 */
public class SecurityManagerTest {

	private final static String basedir = Configuration.props.getBasedir();
	private final static String codeFile = basedir
			+ "/src/test/java/ch/ethz/rse/integration/tests/Basic_Test_Safe.java";
	private final String resultsFile = basedir + "/results/results.txt";

	@BeforeAll
	public static void beforeMethod() {
		// only check when the security manager is enabled
		org.junit.Assume.assumeTrue(Configuration.props.isSecurityManagerEnabled());
	}

	@Test
	public void readForbiddenFileMain() {
		Assertions.assertThrows(AccessControlException.class, () -> {
			// block access to the source files
			FileUtilsWrapper.readFileInList(codeFile);
		});
	}

	@Test
	public void readAllowedFileTest() throws IOException {
		// reading directly in testing code is allowed
		Files.readAllLines(Paths.get(codeFile), StandardCharsets.UTF_8);
	}

	@Test
	public void writeForbiddenResults() {
		Assertions.assertThrows(AccessControlException.class, () -> {
			// block access to the results file
			FileUtilsWrapper.writeStringToFile(new File(resultsFile), "");
		});
	}

	@Test
	public void writeAllowedResults() throws IOException {
		// writing to results directly in testing code is allowed
		FileUtils.writeStringToFile(new File(resultsFile), "", Charset.defaultCharset(), false);
	}
}
