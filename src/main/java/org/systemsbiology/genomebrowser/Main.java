package org.systemsbiology.genomebrowser;

import java.util.Map;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Application;
import org.systemsbiology.genomebrowser.app.Configurator;
import org.systemsbiology.genomebrowser.app.conf.*;
import org.systemsbiology.genomebrowser.sqlite.SqliteDataSource;
import org.systemsbiology.util.StringUtils;


/**
 * Entry point for HeebieGB Genome Browser
 */
public class Main {
	private static final Logger log = Logger.getLogger(Main.class);


	public static void main(String[] args) throws Exception {

		try {
			log.info("-------=====<( Starting HeebieGB Genome Browser )>=====-------");
			log.info("java.version:\t" + System.getProperty("java.version"));

			// parse command line args
			OptionsParser optionsParser = new OptionsParser();
			Options options = optionsParser.initializeOptions(args);

			log.info(String.format("HBGB version:\t%s.%s build-date: %s", options.version, options.buildNumber, options.buildDate));
			log.info("-------======================<++>======================-------");

			Map<String, String> dbInfo = SqliteDataSource.getDatabaseInfo();
			// these are displayed in the sysInfo section of the about dialog
			System.getProperties().putAll(dbInfo);
			log.info(dbInfo);

			// create and configure application
			Configurator conf = new Configurator(options);
			conf.setConfStrategy(new DefaultConf());
			Application app = conf.createApplication();

			app.startup();

			if (StringUtils.nullToEmptyString(dbInfo.get("db.driver")).contains("pure")) {
				app.showErrorMessage("Sqlite driver loaded in \"pure\" mode, which is much slower for some operations.");
			}

			if (options.datasetUrl != null) {
				app.loadDataset(options.datasetUrl);
			}
			else if (options.downloadGenome != null) {
				app.downloadGenome(options.downloadGenome);
			}
		}
		catch (Exception e) {
			log.error(e);
			throw e;
		}
		catch (Error e) {
			log.error(e);
			throw e;
		}
	}
}
