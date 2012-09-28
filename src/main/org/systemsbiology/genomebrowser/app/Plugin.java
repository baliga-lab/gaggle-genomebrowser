package org.systemsbiology.genomebrowser.app;

/**
 * Interface of a plugin. Register the plugin with app.registerPlugin(myPlugin).
 * SetExternalApi will be called before init.
 */
public interface Plugin extends EventListener {

	public void setExternalApi(ExternalAPI api);
	public void init();
}
