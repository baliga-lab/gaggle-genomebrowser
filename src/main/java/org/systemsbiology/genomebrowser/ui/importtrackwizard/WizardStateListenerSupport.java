package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.systemsbiology.genomebrowser.event.Event;

public class WizardStateListenerSupport {
	Set<WizardStateListener> listeners = new CopyOnWriteArraySet<WizardStateListener>();

	public void addWizardStateListener(WizardStateListener listener) {
		listeners.add(listener);
	}

	public void removeWizardStateListener(WizardStateListener listener) {
		listeners.remove(listener);
	}

	public void fireEvent(Event event) {
		for (WizardStateListener listener: listeners) {
			listener.stateChange(event);
		}
	}
}
