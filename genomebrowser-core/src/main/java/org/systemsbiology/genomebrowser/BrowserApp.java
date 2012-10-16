package org.systemsbiology.genomebrowser;

import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.model.Dataset;

/*
 * An interface to eliminate user interface dependencies.
 */
public interface BrowserApp {
    void publishEvent(Event event);
    Options options();
    Dataset dataset();
}