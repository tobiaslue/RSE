package ch.ethz.rse.utils;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Loads properties from the properties file. Needed to provide configuration
 * details.
 * 
 * DO NOT MODIFY THIS FILE
 *
 */
public class Configuration {

	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

	/**
	 * Publicly available singleton
	 */
	public static Configuration props = new Configuration();

	/**
	 * File to load properties from
	 */
	private final String propertiesFile = "properties.config";

	/**
	 * Properties loaded from {@link #propertiesFile}
	 */
	private final Properties prop = new Properties();

	private Configuration() {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFile);
		if (is == null) {
			logger.error("Error loading {}: File not present", propertiesFile);
			throw new RuntimeException("File not found:" + propertiesFile);
		}
		try {
			this.prop.load(is);
		} catch (Exception e) {
			logger.error("Error loading {}:{}", propertiesFile, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @return java home directory to use in soot
	 */
	public String getSootJavaHome() {
		return this.prop.getProperty("SOOT_JAVA_HOME");
	}

	/**
	 * 
	 * @return directory containing the java sources
	 */
	public String getBasedir() {
		return this.prop.getProperty("BASEDIR");
	}

	/**
	 * 
	 * @return true if we are in grading mode
	 */
	private boolean isGrading() {
		String grading = System.getenv("grading");
		if (grading == null) {
			return false;
		} else {
			return grading.equals("true");
		}
	}

	/**
	 * 
	 * @return true if the security manager is restricting access to certain
	 *         resources
	 */
	public boolean isSecurityManagerEnabled() {
		return this.isGrading();
	}

	/**
	 * 
	 * @return runtime available to solve the verification task
	 */
	public int getTimeout() {
		if (this.isGrading()) {
			return 10;
		} else {
			return 5 * 60;
		}
	}

}
