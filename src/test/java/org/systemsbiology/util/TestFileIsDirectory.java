package org.systemsbiology.util;

import java.io.File;

import org.junit.Test;


public class TestFileIsDirectory {

	@Test
	public void test() {
		File file = new File("/Users/cbare/xyzpdq");
		System.out.println(file + ".isDirectory() = " + file.isDirectory());
	}
}
