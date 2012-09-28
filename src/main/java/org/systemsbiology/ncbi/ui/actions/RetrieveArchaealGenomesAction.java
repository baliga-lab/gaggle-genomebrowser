package org.systemsbiology.ncbi.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.systemsbiology.ncbi.ui.NcbiQueryDialog;


public class RetrieveArchaealGenomesAction extends AbstractAction {
	NcbiQueryDialog ncbiQueryDialog;


	public RetrieveArchaealGenomesAction(NcbiQueryDialog ncbiQueryDialog) {
		super("All Archaea");
		this.ncbiQueryDialog = ncbiQueryDialog;
		putValue(Action.SHORT_DESCRIPTION, "Select from all sequenced archaeal genomes.");
	}

	public RetrieveArchaealGenomesAction(Icon icon, NcbiQueryDialog ncbiQueryDialog) {
		super("All Archaea", icon);
		this.ncbiQueryDialog = ncbiQueryDialog;
	}

	public void actionPerformed(ActionEvent e) {
		ncbiQueryDialog.retrieveArchaealGenomes();
	}

}
