package org.systemsbiology.genomebrowser.app;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Sequence;

class ApplicationEventSupport {
	private Set<ApplicationListener> listeners = new CopyOnWriteArraySet<ApplicationListener>();

	public void addApplicationListener(ApplicationListener listener) {
		listeners.add(listener);
	}

	public void removeApplicationListener(ApplicationListener listener) {
		listeners.remove(listener);
	}

	public void fireStartupEvent(Options options) {
		for (ApplicationListener listener : listeners) {
			listener.startup(options);
		}
	}

	public void fireShutdownEvent() {
		for (ApplicationListener listener : listeners) {
			listener.shutdown();
		}
	}

	public void fireSequenceSelected(Sequence seq) {
		for (ApplicationListener listener : listeners) {
			listener.sequenceSelected(seq);
		}
	}

	public void fireNewDatasetEvent(Dataset dataset) {
		for (ApplicationListener listener : listeners) {
			listener.newDataset(dataset);
		}
	}
}
