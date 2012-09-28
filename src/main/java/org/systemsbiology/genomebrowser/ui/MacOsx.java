package org.systemsbiology.genomebrowser.ui;

import org.apache.log4j.Logger;

/**
 * Configures the special stuff in the Apple JVM.
 * @author cbare
 */
public class MacOsx {
	private static final Logger log = Logger.getLogger(MacOsx.class);

	public static void setSystemProperties() {
		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Genome Browser");
		}
		catch (Exception e) {
			log.warn("Exception while setting OSX specific system properties");
			e.printStackTrace();
		}
	}

	public static boolean isOSX() {
		return (System.getProperty("mrj.version") != null);
	}

	public static void createApplicationListener(UI app) {
		try {
			new MacOsxApplication(app);
		}
		catch (Exception e) {
			log.warn("Exception while creating OSX application listener");
			e.printStackTrace();
		}
		
	}
}
