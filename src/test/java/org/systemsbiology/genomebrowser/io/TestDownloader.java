package org.systemsbiology.genomebrowser.io;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.systemsbiology.util.FileUtils;

import junit.framework.Assert;


public class TestDownloader {

	@Test
	public void testDownloader() throws IOException {
		URL url = new URL("http://gaggle.systemsbiology.net/cbare/test_download_small.txt");
		System.out.println(url.getPath());
//		File dir = new File(FileUtils.findUserDocomentsDirectory(), "hbgb");
		File file = File.createTempFile("test_download_small_", ".txt");
		Downloader downloader = new Downloader();
		downloader.download(url, file);
		Assert.assertTrue(file.exists());
		Assert.assertTrue(file.length() > 0);

		String contents = FileUtils.readIntoString(file);
		Assert.assertEquals("This is just junk. It's for testing the genome browser's ability to do downloads. Don't pay it any attention.", contents);
	}

//	@Test
//	public void testPathToUrl() throws Exception {
//		String path = "file:/Users/cbare/.profile";
//		File file = new File(new URI(path));
//		System.out.println("file = " + file);
//		System.out.println("file.exists() = " + file.exists());
//		Assert.assertTrue(file.exists());
//	}
}
