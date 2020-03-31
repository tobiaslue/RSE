package ch.ethz.rse.integration;

import java.io.IOException;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Collects all available tasks
 * 
 */
public class VerifyTaskCollectorTest {

	/**
	 * check that at least one task was found
	 */
	@Test
	public void checkTasksExist() throws IOException {
		List<VerifyTask> tasks = VerifyTaskCollector.get_tasks();
		MatcherAssert.assertThat(tasks.size(), Matchers.greaterThan(0));
	}

	/**
	 * 
	 * Check that no tasks expects an error
	 */
	@Test
	public void checkTasksHaveNoErrors() throws IOException {
		List<VerifyTask> tasks = VerifyTaskCollector.get_tasks();
		for (VerifyTask t : tasks) {
			Assertions.assertEquals(null, t.expected.error);
		}
	}
}
