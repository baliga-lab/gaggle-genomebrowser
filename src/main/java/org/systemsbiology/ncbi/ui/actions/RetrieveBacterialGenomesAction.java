package org.systemsbiology.ncbi.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.systemsbiology.ncbi.ui.NcbiQueryDialog;

public class RetrieveBacterialGenomesAction extends AbstractAction {
    private NcbiQueryDialog ncbiQueryDialog;

    public RetrieveBacterialGenomesAction(NcbiQueryDialog ncbiQueryDialog) {
        super("All Bacteria");
        this.ncbiQueryDialog = ncbiQueryDialog;
        putValue(Action.SHORT_DESCRIPTION, "Select from all sequenced bacterial genomes.");
    }

    public RetrieveBacterialGenomesAction(Icon icon, NcbiQueryDialog ncbiQueryDialog) {
        super("All Bacteria", icon);
        this.ncbiQueryDialog = ncbiQueryDialog;
    }

    public void actionPerformed(ActionEvent e) {
        ncbiQueryDialog.retrieveBacterialGenomes();
    }
}
