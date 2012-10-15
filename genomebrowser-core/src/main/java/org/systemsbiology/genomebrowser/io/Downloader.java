package org.systemsbiology.genomebrowser.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.systemsbiology.util.Progress;

public class Downloader {
	private static final Logger log = Logger.getLogger(Downloader.class);
	private Progress progress = new Progress();


	public Progress getProgress() {
		return progress;
	}

	
	/**
	 * Downloads a dataset from a URL to a local file in the data directory.
	 */
//	public File download(URL url, File dataDirectory) throws IOException {
//		File file = toCachedLocalFile(url, dataDirectory);
//		downloadToFile(url, file);
//		return file;
//	}

	/**
	 * Downloads a dataset from a URL to a local file.
	 * @param url remote URL of dataset
	 * @param file local file where dataset will be stored
	 */
	public void download(URL url, File file) throws IOException {
		BufferedInputStream instream = null;
		BufferedOutputStream outstream = null;
		try {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			URLConnection conn = url.openConnection();
			
			progress.setExpected(conn.getContentLength());
			log.info("download length = " + progress.getExpected());
			conn.setConnectTimeout(8000);
			conn.setReadTimeout(5000);
			instream = new BufferedInputStream(conn.getInputStream());
			outstream = new BufferedOutputStream(new FileOutputStream(file));
			byte[] buffer = new byte[4096];
			int len;
			while ((len = instream.read(buffer)) > -1) {
				outstream.write(buffer, 0, len);
				progress.add(len);
			}
			outstream.flush();
		}
		finally {
			try {
				if (outstream!=null) {
					outstream.close();
				}
			}
			catch (Exception e) {
				log.warn(e);
			}
			
			try {
				if (instream!=null) {
					instream.close();
				}
			}
			catch (Exception e) {
				log.warn(e);
			}
		}
	}
}
