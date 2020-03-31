package ch.ethz.rse.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * Wrapper around file utilities (needed for testing read/write restrictions)
 *
 */
public class FileUtilsWrapper {

	public static List<String> readFileInList(String fileName) {
		try {
			return Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void writeStringToFile(File f, String s) {
		try {
			FileUtils.writeStringToFile(f, s, Charset.defaultCharset(), false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}