package org.systemsbiology.util;

import java.awt.Component;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import static org.systemsbiology.util.StringUtils.isNullOrEmpty;

import org.apache.log4j.Logger;


/**
 * IO related utility functions.
 *
 * @author cbare
 */
public class FileUtils {
	private static Logger log = Logger.getLogger(FileUtils.class);

	private static final Pattern dos = Pattern.compile("[A-Za-z]:.*");


	/**
	 * Get file extenstion.
	 * @return file extension, empty string if there is none, or null if null passed in.
	 */
	public static String extension(String filename) {
		if (filename==null) return null;
		File file = new File(filename);
		String name = file.getName();
		int i = name.lastIndexOf(".");
		if (i > -1 && i < name.length() - 1) {
			return name.substring(i, name.length());
		}
		return "";
	}

//	public static String extension(String path) {
//		Matcher m = Pattern.compile(".*[/\\][^/\\]*.([^/\\.]*)").matcher(path);
//		if (m.matches()) {
//			return m.group(1);
//		}
//		return "";
//	}

	public static String stripExtension(String filename) {
		if (filename==null) return null;
		int i = filename.lastIndexOf(".");
		if (i > -1) {
			return filename.substring(0, i);
		}
		return filename;
	}

	public static int countLines(File file) throws IOException {
		return countLines(new FileReader(file));
	}

	/**
	 * @param reader The reader will be read to completion.
	 * @return the number of lines in a file
	 */
	public static int countLines(Reader reader) throws IOException {
		BufferedReader r = new BufferedReader(reader);
		int i=0;
		while (r.readLine() != null) {
			i++;
		}
		return i;
	}

	/**
	 * Get an InputStream for a resource to be loaded from the classpath.
	 */
	public static InputStream getInputStreamForResource(String filename, Class<?> requestingClass) throws IOException {
		URI uri;
		try {
			uri = new URI(filename);

			InputStream in = requestingClass.getResourceAsStream(uri.getPath());
			// try making the path relative to the root of the classpath
			if (in == null && !uri.getPath().startsWith("/")) {
				in = requestingClass.getResourceAsStream("/" + uri.getPath());
			}
			if (in == null) {
				throw new IOException("File not found: " + uri);
			}
			return in;

		} catch (URISyntaxException e) {
			throw new IOException(e.getMessage());
		}
	}


	/**
	 * get a reader for a resource to be loaded from the classpath.
	 */
	public static Reader getReaderForResource(String filename, Class<?> requestingClass) throws IOException {
		return new InputStreamReader(getInputStreamForResource(filename, requestingClass));
	}

	public static Reader getReaderForResource(String filename) throws IOException {
		return new InputStreamReader(getInputStreamForResource(filename, FileUtils.class));
	}


	public static URL getUrlForResource(String filename) throws MalformedURLException {
		return getUrlForResource(filename, FileUtils.class);
	}

	public static URL getUrlForResource(String filename, Class<?> requestingClass) throws MalformedURLException {

		// is it a filesystem path?
		File file = new File(filename);
		if (file.exists()) {
			return file.toURI().toURL();
		}

		URI uri;
		try {
			uri = new URI(filename);
		} catch (URISyntaxException e) {
			return requestingClass.getResource(filename);
		}
		if (uri.getScheme()==null)
			return requestingClass.getResource(filename);

		return getUrlForResource(uri, requestingClass);
	}

	public static URL getUrlForResource(URI uri, Class<?> requestingClass) throws MalformedURLException {
		if ("classpath".equals(uri.getScheme())) {
			return requestingClass.getResource(uri.getPath());
		}
		else
			return uri.toURL();
	}

	/**
	 * Delegates to getReaderFor(String filename, Class requestingClass) using FileUtils
	 * as the requestingClass.
	 */
	public static Reader getReaderFor(String filename) throws IOException {
		return getReaderFor(filename, FileUtils.class);
	}

	/**
	 * Given a filename or url, returns a reader for the file. Works
	 * for filesystem paths (windows or unix style) and for files in
	 * the classpath (in directories or in jar files). Also works for
	 * urls.
	 * @param filename a file or URI
	 * @param requestingClass the class whose classloader will be used to find files in the classpath.
	 * @return java.io.Reader
	 * @throws IOException
	 */
	public static Reader getReaderFor(String filename, Class<?> requestingClass) throws IOException {
		return new InputStreamReader(getInputStreamFor(filename, requestingClass));
	}


	public static InputStream getInputStreamFor(String filename) throws IOException {
		return getInputStreamFor(filename, FileUtils.class);
	}

	/**
	 * Given a filename or url, returns an InputStream for the file. Works
	 * for filesystem paths (windows or unix style) and for files in
	 * the classpath (in directories or in jar files). Also works for
	 * urls.
	 * @param filename a file or URI
	 * @param requestingClass the class whose classloader will be used to find files in the classpath.
	 * @throws IOException
	 */
	public static InputStream getInputStreamFor(String filename, Class<?> requestingClass) throws IOException {
		filename = filename.trim();

		// try to detect windows file paths
		if (filename.indexOf("\\")>=0 || dos.matcher(filename).matches()) {
			log.info("loading windows file: " + filename);
			return new FileInputStream(filename);
		}

		try {
			// using URLEncoder doesn't quite work, 'cause it ends up encoding the :// part, which is no good.
			// URI uri = new URI(URLEncoder.encode(filename, "UTF-8"));
			URI uri = new URI(filename.replace(" ", "%20"));
			log.info(uri);

			if (uri.getScheme() == null) {
				// first try in classpath
				InputStream in = requestingClass.getResourceAsStream(uri.getPath());
				// try making the path relative to the root of the classpath
				if (in == null && !uri.getPath().startsWith("/")) {
					in = requestingClass.getResourceAsStream("/" + uri.getPath());
				}
				if (in == null) {
					// try as a file system path
					in = new FileInputStream(uri.getPath());
				}
				return in;
			}
			else if ("file".equalsIgnoreCase(uri.getScheme())) {
				return new FileInputStream(uri.getPath());
			}
			else if ("classpath".equalsIgnoreCase(uri.getScheme()) || "jar".equalsIgnoreCase(uri.getScheme())) {
				return getInputStreamForResource(uri.getPath(), requestingClass);
			}
			else if ("http".equalsIgnoreCase(uri.getScheme())) {
				URL url = uri.toURL();
				return url.openStream();
			}
			else {
				throw new IOException("Unknown protocol: " + uri.getScheme());
			}
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}


	public static String readIntoString(File file) throws IOException {
		return readIntoString(new FileReader(file));
	}

	public static String readIntoString(String path) throws IOException {
		return readIntoString(getReaderFor(path));
	}

	public static String readIntoString(Reader reader) throws IOException {
		BufferedReader brdr = new BufferedReader(reader);
		StringBuffer buffer = new StringBuffer();
		int len;
		char[] cbuf = new char[4096];
		while ( (len=brdr.read(cbuf)) > -1 ) {
			buffer.append(cbuf, 0, len);
		}
		return buffer.toString();
	}


	public static URI getBaseUri(String path) throws IOException {
		File file = new File(path);
		if (file.exists()) {
			if (file.isDirectory())
				return file.toURI();
			else
				return file.getParentFile().toURI();
		}

		try {
			URL url = FileUtils.getUrlForResource(path);
			return url.toURI();
		}
		catch (Exception e) {
			log.error(e);
			throw new IOException("Error parsing path: " + path);
		}
	}

	public static Icon getIconOrBlank(String filename) {
		try {
			return getIcon(filename);
		}
		catch (Exception e) {
			log.warn("exception loading " + filename, e);
			return new Icon() {
				public int getIconHeight() {
					return 0;
				}
				public int getIconWidth() {
					return 0;
				}
				public void paintIcon(Component c, Graphics g, int x, int y) {
				}
				
			};
		}
	}

	public static ImageIcon getIcon(String filename) throws IOException {
		return new ImageIcon(FileUtils.class.getResource("/icons/"+filename));		
	}

	/**
	 * Tries to figure out whether the given string represents a remote URL or
	 * a path in the local filesystem.
	 * @return the scheme portion of the URI or "file" if we can't figure anything out. 
	 */
	public static String getUrlScheme(String path) {
		path = path.trim();

		// try to detect windows file paths
		if (path.indexOf("\\")>=0 || dos.matcher(path).matches()) {
			return "file";
		}

		try {
			URI uri = new URI(path.replace(" ", "%20"));
	
			if (uri.getScheme() == null) {
				return "file";
			}
			else
				return uri.getScheme();
		}
		catch (Exception e) {
			return "file";
		}
	}


	/**
	 * Attempt to resolve the given path against the current working directory
	 * and the user's home directory.
	 * @return an existing file or the working directory or the user's home directory
	 */
	public static File resolveRelativePath(String path, File workingDirectory) {
		if (!StringUtils.isNullOrEmpty(path)) {
			File file = new File(path);
			if (file.isAbsolute()) {
				return file;
			}
			if (workingDirectory != null) {
				file = new File(workingDirectory, path);
				if (file.exists())
					return file;
			}
			file = new File(new File(System.getProperty("user.home")), path);
			if (file.exists())
				return file;
		}
		if (workingDirectory != null)
			return workingDirectory;
		else
			return new File(System.getProperty("user.home"));
	}

	/**
	 * Find the platform specific user documents directory.
	 * On most Unix systems, this is the same as user.home, but
	 * we have a few special cases for OS X and the many flavors
	 * of windows.
	 * <pre> 
	 * OS X          /Users/<user>/Documents
	 * Windows XP    C:\Documents and Settings\<user>\My Documents
	 * Vista         C:\Users\<user>\Documents
	 * </pre>
	 * @return
	 */
	public static File findUserDocomentsDirectory() {
		File home = new File(System.getProperty("user.home"));
		String os = System.getProperty("os.name");
		File documents = home;
		if ("Mac OS X".equals(os)) {
			File test = new File(home, "Documents");
			if (test.exists())
				documents = test;
		}
		else if (os.startsWith("Windows")) {
			// most versions of Windows since 2000 use "My Documents", but
			// Vista uses just "Documents". Let's just look for both.
			File test = new File(home, "Documents");
			if (test.exists())
				documents = test;
			else {
				test = new File(home, "My Documents");
				if (test.exists())
					documents = test;
			}
		}
		return documents;
	}

	public static String toValidFilename(String string) {
		string = string.trim();
		if (isNullOrEmpty(string)) return "file";
		String filename = string.replaceAll("[^A-Za-z0-9\\._]", "_");
		filename = filename.replaceAll("_+", "_");
		return filename;
	}

	/**
	 * Determine where to store a local copy of a file from the given URL.
	 */
	public static File urlToLocalFile(URL url, File directory) {
		String host = toValidFilename(url.getHost());
		File dir = new File(directory, host);
		return new File(dir, url.getPath());
	}

	/**
	 * Makes a file unique by appending a number to the end of the base filename
	 * in the form <name>_01.<extension>. Can be used to prevent overwriting
	 * existing files.
	 */
	public static File uniquify(File file) {
		if (!file.exists()) return file;
		
		File parent = file.getParentFile();
		String stem = stripExtension(file.getName());
		String ext  = extension(file.getName());

		int n=0;
		File uniqueFile;
		do {
			n++;
			uniqueFile = new File(parent, String.format("%s_%02d%s", stem, n, ext));
		} while (uniqueFile.exists());

		return uniqueFile;
	}
}
