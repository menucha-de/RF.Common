package havis.device.rf.common.util;

import havis.device.rf.common.Environment;
import havis.device.rf.exception.ImplementationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * A utility class for simple file operations needed throughout the application.
 * 
 */

public class FileUtils {

	/**
	 * Reads and returns the contents of a (UTF-8) text file.
	 * 
	 * @param fileName
	 *            the name (and path) of the file to be read
	 * @return the contents of the file as string
	 * @throws IOException
	 *             if anything goes wrong, e.g. file not found or not readable
	 */
	public static String readTextFile(String fileName) throws IOException {

		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(
				new File(fileName)), Charset.forName("UTF-8"))) {

			final int bufLen = 1024 * 4; // 4 kB

			int charsRead = 0;
			char[] charBuf = new char[bufLen];

			StringBuffer sb = new StringBuffer();
			while ((charsRead = isr.read(charBuf, 0, charBuf.length)) > -1)
				sb.append(charBuf, 0, charsRead);

			return sb.toString();
		}
	}

	/**
	 * Reads an input stream and returns its data as string.
	 * 
	 * @param inputStream
	 *            the input stream to read from
	 * @return the string contents of the input stream
	 * @throws IOException
	 *             if anything goes wrong
	 */
	public static String readTextFile(InputStream inputStream)
			throws IOException {

		try (InputStreamReader isr = new InputStreamReader(inputStream)) {

			final int bufLen = 1024 * 4; // 4 kB

			int charsRead = 0;
			char[] charBuf = new char[bufLen];

			StringBuffer sb = new StringBuffer();
			while ((charsRead = isr.read(charBuf, 0, charBuf.length)) > -1)
				sb.append(charBuf, 0, charsRead);

			return sb.toString();
		}
	}

	/**
	 * Writes the contents of a (UTF-8) string to text file.
	 * 
	 * @param file
	 *            the file to be written
	 * @param content
	 *            the contents of the file as string
	 * @throws IOException
	 *             if anything goes wrong, e.g. file not found or not writable
	 */
	public static void writeTextFile(File file, String content)
			throws IOException {
		try (OutputStreamWriter osw = new OutputStreamWriter(
				new FileOutputStream(file),
				Charset.forName("UTF-8"))) {
			osw.write(content);
		}
	}

	/**
	 * Reads a JSON string from a file and deserializes it to an object of class
	 * T
	 * 
	 * @param fileName
	 *            the absolute path to the file to read
	 * @param type
	 *            the class of T
	 * @return an object of class T
	 * @throws JsonParseException
	 *             when something during JSON deserialization goes wrong
	 * @throws JsonMappingException
	 *             when something during JSON deserialization goes wrong
	 * @throws IOException
	 *             when the file specified cannot be read
	 */
	public static <T> T deserialize(String fileName, Class<T> type)
			throws JsonParseException, JsonMappingException, IOException {
		JsonSerializer jsonizer = new JsonSerializer(type);
		return jsonizer.deserialize(readTextFile(fileName));
	}

	/**
	 * Reads a JSON string from an input stream and deserializes it to an object
	 * of class T
	 * 
	 * @param inStream
	 *            the input stream to read from
	 * @param type
	 *            the class of T
	 * @return an object of class T
	 * @throws JsonParseException
	 *             when something during JSON deserialization goes wrong
	 * @throws JsonMappingException
	 *             when something during JSON deserialization goes wrong
	 * @throws IOException
	 *             when the file specified cannot be read
	 */
	public static <T> T deserialize(InputStream inStream, Class<T> type)
			throws JsonParseException, JsonMappingException, IOException {
		JsonSerializer jsonizer = new JsonSerializer(type);
		return jsonizer.deserialize(inStream, type);
	}

	/**
	 * Serializes a given object as JSON to a spefified file
	 * 
	 * @param file
	 *            the file to be written
	 * @param object
	 *            an object of class T
	 * @throws JsonParseException
	 *             when something during JSON serialization goes wrong
	 * @throws JsonMappingException
	 *             when something during JSON serialization goes wrong
	 * @throws IOException
	 *             when the file specified cannot be written
	 * @throws ImplementationException
	 *             when accessing the Environment (properties) fails
	 */
	public static <T> void serialize(File file, T object)
			throws JsonParseException, JsonMappingException, IOException,
			ImplementationException {
		JsonSerializer jsonizer = new JsonSerializer(object.getClass());
		jsonizer.setPrettyPrint(Environment.SERIALIZER_PRETTY_PRINT);
		writeTextFile(file, jsonizer.serialize(object));
	}

	/**
	 * Copies a file.
	 * 
	 * @param sourceFile
	 *            File instance of the source file
	 * @param destinationFile
	 *            File instance of the destination file
	 * @param overwrite
	 *            true if destination file may be overwritten
	 * @param createDestinationDir
	 *            true if destination path (including all sub-paths) is supposed
	 *            to be created if not exists
	 * @throws IOException
	 *             if anything goes wrong
	 */
	public static void copyFile(File sourceFile, File destinationFile,
			boolean overwrite, boolean createDestinationDir) throws IOException {
		copyFile(sourceFile.getAbsolutePath(),
				destinationFile.getAbsolutePath(), overwrite,
				createDestinationDir);
	}

	/**
	 * Copies a file.
	 * 
	 * @param sourceFile
	 *            file name (and path) of the source file
	 * @param destinationFile
	 *            file name (and/or path) of the destination file
	 * @param overwrite
	 *            true if destination file may be overwritten
	 * @param createDestinationDir
	 *            true if destination path (including all sub-paths) is supposed
	 *            to be created if not exists
	 * @throws IOException
	 *             if anything goes wrong
	 */
	public static void copyFile(String sourceFile, String destinationFile,
			boolean overwrite, boolean createDestinationDir) throws IOException {
		Path src = FileSystems.getDefault().getPath(sourceFile);
		Path dst = FileSystems.getDefault().getPath(destinationFile);

		if (createDestinationDir)
			createFullPath(destinationFile);

		CopyOption[] opts = overwrite ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING }
				: new CopyOption[] {};

		Files.copy(src, dst, opts);
	}

	/**
	 * Moves a file.
	 * 
	 * @param sourceFile
	 *            file name (and path) of the source file
	 * @param destinationFile
	 *            file name (and/or path) of the destination file
	 * @param overwrite
	 *            true if destination file may be overwritten
	 * @param createDestinationDir
	 *            true if destination path (including all sub-paths) is supposed
	 *            to be created if not exists
	 * @throws IOException
	 *             if anything goes wrong
	 */
	public static void moveFile(String sourceFile, String destinationFile,
			boolean overwrite, boolean createDestinationDir) throws IOException {
		Path src = FileSystems.getDefault().getPath(sourceFile);
		Path dst = FileSystems.getDefault().getPath(destinationFile);

		if (createDestinationDir)
			createFullPath(destinationFile);

		CopyOption[] opts = overwrite ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING }
				: new CopyOption[] {};

		Files.move(src, dst, opts);
	}

	/**
	 * Creates a directory-tree (directory including all sub-paths)
	 * 
	 * @param fileOrPath
	 *            file or path of the directory to be created. If fileOrdPath
	 *            ends with a file name, it will be truncated.
	 * @throws IOException
	 *             if anything goes wrong
	 */
	public static void createFullPath(String fileOrPath) throws IOException {
		String destDir = getPathName(fileOrPath);
		File f = new File(destDir);
		if (!f.exists())
			if (!f.mkdirs())
				throw new IOException(String.format(
						"Failed to create directory: %s", destDir));
	}

	/**
	 * Extracts the file name from a full path.
	 * 
	 * @param fileAndPath
	 *            a path and a file
	 * @return the extracted file name
	 */
	public static String getFileName(String fileAndPath) {
		String pathName = getPathName(fileAndPath);

		// if fileAndPath was only a path, return empty string
		if (pathName.equals(fileAndPath)
				|| pathName.equals(fileAndPath + File.separator))
			return "";

		// else remove the pathName from fileAndPath to determine the file
		return fileAndPath.replace(getPathName(fileAndPath), "");
	}

	/**
	 * Extracts the path name from a full path and file (i.e. truncates the
	 * filename).
	 * 
	 * @param fileAndPath
	 *            a path and a file
	 * @return the extracted file path name
	 */
	public static String getPathName(String fileAndPath) {

		String res =
		/*
		 * regex matches one or more chars that is no File.separator followed
		 * the pattern one '.' followed by any char (this pattern may repeat, to
		 * match files with multiple extensions). The regex matches file names
		 * and the end of the fileAndPath string.
		 */
		fileAndPath.replaceAll("[^"
				+ (File.separatorChar == '\\' ? "\\\\" : File.separator)
				+ "]+(\\..+)+$", "");

		if (res.endsWith(File.separator))
			return res;
		else
			return res + File.separator;
	}

	/**
	 * Forms a path string from multiple parts (ensuring the right amount of
	 * separators being placed in between)
	 * 
	 * @param path
	 *            the first part of the path
	 * @param more
	 *            further parts to be appended
	 * @return the path as string
	 */
	public static String path(String path, String... more) {
		return FileSystems.getDefault().getPath(path, more).toString();
	}
}
