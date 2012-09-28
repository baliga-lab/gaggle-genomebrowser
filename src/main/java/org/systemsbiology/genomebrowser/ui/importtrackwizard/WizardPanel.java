package org.systemsbiology.genomebrowser.ui.importtrackwizard;

public interface WizardPanel {
	void onLoad();
	void onUnload();
	boolean getEnableNext();
	boolean getEnableBack();
	boolean getEnableDone();

	// added so that we can set focus when a WizardPanel becomes visible
	void windowGainedFocus();
}
