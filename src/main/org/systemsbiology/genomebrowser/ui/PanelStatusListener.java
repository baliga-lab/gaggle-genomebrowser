package org.systemsbiology.genomebrowser.ui;


/**
 * Notify listeners that the panel concerned has opened or closed. This is
 * used to implement the toggling behavior of ToggleBookmarkPanelAction.
 * @author cbare
 */
public interface PanelStatusListener {
	public enum Status {Open, Closed};

	// add source here?
	public void statusChanged(Status status);
}
