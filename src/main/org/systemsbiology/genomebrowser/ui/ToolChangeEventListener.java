package org.systemsbiology.genomebrowser.ui;

/**
 * Receive notification when the user selects
 * a new mouse tool.
 * 
 * @author cbare
 */
public interface ToolChangeEventListener {
	
	public void setCursorTool(CursorTool tool);
}
