package ch.ethz.rse.verify;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.io.FilenameUtils;

/**
 * 
 * Convenience wrapper that captures the relevant information about a class to
 * be verified
 *
 */
public class ClassToVerify {

	private final File classPath;
	private final String packageName;
	private final File classFile;

	/**
	 * 
	 * @param classPath   path to the root of the project containing the class to be
	 *                    verified
	 * @param packageName fully qualified name of class to be verified
	 * @throws FileNotFoundException
	 */
	public ClassToVerify(File classPath, String packageName) throws FileNotFoundException {
		if (!classPath.exists()) {
			throw new FileNotFoundException(classPath.getAbsolutePath());
		}
		this.classPath = classPath;
		this.packageName = packageName;

		String relativePath = packageName.replace(".", "/") + ".class";
		this.classFile = new File(classPath, relativePath);

		if (!this.classFile.exists()) {
			throw new FileNotFoundException(classFile.getAbsolutePath());
		}
	}

	/**
	 * 
	 * @return the unqualified name of the class to be verified
	 */
	public String getName() {
		String name = this.classFile.getName();
		name = FilenameUtils.removeExtension(name);
		return name;
	}

	/**
	 * 
	 * @return path to the root of the project containing the class to be verified
	 */
	public File getClassPath() {
		return this.classPath;
	}

	/**
	 * 
	 * @return fully qualified name of class to be verified
	 */
	public String getPackageName() {
		return this.packageName;
	}

	/**
	 * 
	 * @return .class file to be verified
	 */
	public File getClassFile() {
		return this.classFile;
	}

	@Override
	public String toString() {
		return this.getName();
	}

}
