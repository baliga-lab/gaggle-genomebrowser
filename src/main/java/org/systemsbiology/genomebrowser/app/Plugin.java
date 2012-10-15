package org.systemsbiology.genomebrowser.app;

import org.systemsbiology.genomebrowser.event.EventListener;

/**
 * Interface of a plugin. Register the plugin with app.registerPlugin(myPlugin).
 * SetExternalApi will be called before init.
 */
public interface Plugin extends EventListener {

	public void setExternalApi(ExternalAPI api);
	public void init();
}
