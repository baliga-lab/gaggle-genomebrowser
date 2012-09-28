package org.systemsbiology.genomebrowser.ui;

public interface CrosshairsListener {
	public void crosshairsAt(int coord);
	public void selectBoxAt(int start, int end);
	public void crosshairsDone();
}
