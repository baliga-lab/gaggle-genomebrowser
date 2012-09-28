package org.systemsbiology.ncbi.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.systemsbiology.ncbi.ui.NcbiQueryDialog;


public class RetrieveProkaryoticGenomesAction extends AbstractAction {
	NcbiQueryDialog ncbiQueryDialog;


	public RetrieveProkaryoticGenomesAction(NcbiQueryDialog ncbiQueryDialog) {
		super("All Prokaryotes");
		this.ncbiQueryDialog = ncbiQueryDialog;
		putValue(Action.SHORT_DESCRIPTION, "Select from all sequenced prokaryotic genomes.");
	}

	public RetrieveProkaryoticGenomesAction(Icon icon, NcbiQueryDialog ncbiQueryDialog) {
		super("Prokaryotes", icon);
		this.ncbiQueryDialog = ncbiQueryDialog;
	}

	public void actionPerformed(ActionEvent e) {
		ncbiQueryDialog.retrieveAllProkaryoticGenomes();
	}

}
