package org.systemsbiology.util;

public class Debug {

	public static void printStackTrace() {
		try {
			throw new Exception();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
