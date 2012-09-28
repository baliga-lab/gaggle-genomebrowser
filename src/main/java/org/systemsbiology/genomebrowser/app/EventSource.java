package org.systemsbiology.genomebrowser.app;

// might use this to tag components that produce events
// the underscore is an attempt to flag dependency methods?

public interface EventSource {
	public void _setEventSupport(EventSupport eventSupport);
}
