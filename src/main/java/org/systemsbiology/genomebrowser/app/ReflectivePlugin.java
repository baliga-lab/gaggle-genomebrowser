package org.systemsbiology.genomebrowser.app;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.event.Event;


/**
 * Wrap a plugin and access its plugin methods through reflection. This removes
 * (or makes implicit) the dependency of the plugin on genome browser classes.
 * Not sure if this is really useful.
 */
public class ReflectivePlugin implements Plugin {
	private static final Logger log = Logger.getLogger(ReflectivePlugin.class);
	private final Object plugin;

	public ReflectivePlugin(Object plugin) {
		this.plugin = plugin;
	}

	public String getName() {
		try {
			Method getName = plugin.getClass().getMethod("getName");
			if (getName.getReturnType() == String.class)
				return (String)getName.invoke(plugin);
		}
		catch (NoSuchMethodError e) {}
		catch (Exception e) {
			log.warn(e);
		}
		return plugin.getClass().getName();
	}

	public void init() {
		try {
			Method init = plugin.getClass().getMethod("init");
			init.invoke(plugin);
		}
		catch (Exception e) {
			log.warn(e);
		}
	}

	public void setExternalApi(ExternalAPI api) {
		try {
			Method setExternalApi = plugin.getClass().getMethod("setExternalApi", ExternalAPI.class);
			setExternalApi.invoke(plugin, api);
		}
		catch (Exception e) {
			log.warn(e);
		}
	}

	public void receiveEvent(Event event) {
		try {
			Method setExternalApi = plugin.getClass().getMethod("receiveEvent", Event.class);
			setExternalApi.invoke(plugin, event);
		}
		catch (Exception e) {
			log.warn(e);
		}
	}
}
