package org.systemsbiology.genomebrowser.ui;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;


/**
 * Mac OS X specific stuff. Handle quit and about menu items. This class imports
 * apple specific classes that aren't included in the distribution of the
 * Genome Browser. So, this class shouldn't be loaded unless we're on the apple
 * JVM.
 * 
 * @author cbare
 */
public class MacOsxApplication implements ApplicationListener {
	UI app;
	
	MacOsxApplication(UI app) {
		this.app = app;
		Application application = Application.getApplication();
		application.setEnabledPreferencesMenu(true);
		application.addApplicationListener(this);
	}

	public void handleAbout(ApplicationEvent event) {
		app.showAbout();
		event.setHandled(true);
	}

	public void handleOpenApplication(ApplicationEvent event) {}

	public void handleOpenFile(ApplicationEvent event) {}

	public void handlePreferences(ApplicationEvent event) {}

	public void handlePrintFile(ApplicationEvent event) {}

	public void handleQuit(ApplicationEvent event) {
		app.exit(0);
		// calling setHandle(false) cancels quiting.
		// If we get here, the user must have canceled.
		event.setHandled(false);
	}

	public void handleReOpenApplication(ApplicationEvent event) {}

}
