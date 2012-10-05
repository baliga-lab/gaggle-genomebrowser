package org.systemsbiology.ncbi.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.systemsbiology.ncbi.ui.NcbiQueryDialog;

public class DownloadGenomeAction extends AbstractAction {

    private NcbiQueryDialog ncbiQueryDialog;

    public DownloadGenomeAction(NcbiQueryDialog ncbiQueryDialog) {
        super("Download");
        this.ncbiQueryDialog = ncbiQueryDialog;
        putValue(Action.SHORT_DESCRIPTION, "Download genes from NCBI.");
    }

    public void actionPerformed(ActionEvent e) {
        ncbiQueryDialog.downloadGenome();
    }
}
