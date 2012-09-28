package org.systemsbiology.genomebrowser.app;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

// unused

/**
 * Keep a list of plugins. Is this better just handled in the Application object?
 */
public class PluginManager {
	private static final Logger log = Logger.getLogger(PluginManager.class);
	List<Plugin> plugins = new ArrayList<Plugin>();
	ExternalAPI api;

	public void setExternalApi(ExternalAPI api) {
		this.api = api;
	}

	public void registerPlugin(Plugin plugin) {
		plugin.setExternalApi(api);
		plugins.add(plugin);
		log.info("registered plugin: " + plugin.getClass().getName());
	}

	public void registerPlugin(Class<? extends Plugin> pluginClass) {
		try {
			registerPlugin(pluginClass.newInstance());
		}
		catch (Exception e) {
			log.error("Failed to create instance of plugin " + pluginClass.getName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public void registerPlugin(String pluginClassName) {
		try {
			registerPlugin((Class<? extends Plugin>)Class.forName(pluginClassName));
		}
		catch (Exception e) {
			log.error("Failed to create instance of plugin " + pluginClassName, e);
		}
	}
}
