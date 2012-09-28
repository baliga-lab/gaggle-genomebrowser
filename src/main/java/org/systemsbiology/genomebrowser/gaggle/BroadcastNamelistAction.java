package org.systemsbiology.genomebrowser.gaggle;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.systemsbiology.gaggle.core.Boss;
import org.systemsbiology.gaggle.geese.common.GaggleConnectionListener;



public class BroadcastNamelistAction extends AbstractAction implements GaggleConnectionListener {
	GenomeBrowserGoose goose;

	public BroadcastNamelistAction(GenomeBrowserGoose goose) {
		super("Broadcast Namelist");
		this.goose = goose;
		putValue(Action.SHORT_DESCRIPTION, "Broadcast selected data to target goose.");
	}


	public void actionPerformed(ActionEvent e) {
		goose.broadcast();
	}

	public void setConnected(boolean connected, Boss boss) {
		setEnabled(connected);
	}
}
