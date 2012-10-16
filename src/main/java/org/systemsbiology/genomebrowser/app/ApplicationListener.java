package org.systemsbiology.genomebrowser.app;

import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.Options;

public interface ApplicationListener {

	public void startup(Options options);
	public void shutdown();

	public void newDataset(Dataset dataset);
	public void sequenceSelected(Sequence sequence);
}
