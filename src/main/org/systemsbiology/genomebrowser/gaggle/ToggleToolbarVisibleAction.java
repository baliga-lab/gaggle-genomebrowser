package org.systemsbiology.genomebrowser.gaggle;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import org.systemsbiology.genomebrowser.app.ExternalAPI;
import org.systemsbiology.util.FileUtils;



public class ToggleToolbarVisibleAction extends AbstractAction {
	ExternalAPI api;
	GaggleToolbar toolbar;
	
	/**
	 * Accelerator key for toggling gaggle tool bar
	 */
	static KeyStroke toggleToolbarKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() + InputEvent.SHIFT_MASK);

	
	public ToggleToolbarVisibleAction(GaggleToolbar toolbar, ExternalAPI api) {
		super("Show Gaggle Toolbar");
		this.api = api;
		this.toolbar = toolbar;
		putValue(Action.SHORT_DESCRIPTION, "Show or hide the Gaggle Toolbar.");
		putValue(Action.ACCELERATOR_KEY, toggleToolbarKeyStroke);
		putValue(Action.SMALL_ICON, FileUtils.getIconOrBlank("gaggle.png"));
	}

	public void actionPerformed(ActionEvent e) {
		boolean visible = !toolbar.isVisible();
		api.setVisibleToolbar(GaggleToolbar.TITLE, visible);
		putValue(Action.NAME, getName(visible));
	}

	private String getName(boolean visible) {
		if (visible) {
			return "Hide Gaggle Toolbar";
		}
		else {
			return "Show Gaggle Toolbar";
		}
	}
}
