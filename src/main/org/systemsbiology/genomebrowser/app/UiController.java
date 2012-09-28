package org.systemsbiology.genomebrowser.app;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import org.systemsbiology.genomebrowser.model.Segment;


public interface UiController {

	public boolean confirm(String message, String title);
	public void insertMenu(String title, Action[] actions);
	public void addToolbar(String title, JToolBar toolbar, Action action);
	public void setVisibleToolbar(String title, boolean visible);
	public void showErrorMessage(String message, Throwable t);
	public void showErrorMessage(String string);
	public void bringToFront();
	public void minimize();
	public void refresh();
	public JFrame getMainWindow();
	public Segment getVisibleSegment();
}
