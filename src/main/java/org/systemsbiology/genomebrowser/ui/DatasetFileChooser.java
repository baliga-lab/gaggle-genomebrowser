package org.systemsbiology.genomebrowser.ui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;


/**
 * A subclass of JFileChooser that allows selections of anything that
 * could be a dataset.
 * @author cbare
 */
public class DatasetFileChooser {

	public static JFileChooser getDatasetFileChooser(File workingDirectory) {
		JFileChooser fileChooser = new JFileChooser();

		fileChooser.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.getName().endsWith(".dataset") || f.getName().endsWith(".hbgb");
			}
			public String getDescription() {
				return "Genome Browser Datasets";
			}
		});

		fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		if (workingDirectory != null)
			fileChooser.setCurrentDirectory(workingDirectory);
		else
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		
		return fileChooser;
	}

	public static JFileChooser getNewDatasetFileChooser() {
		JFileChooser fileChooser = new JFileChooser();

		fileChooser.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.getName().endsWith(".dataset") || f.getName().endsWith(".hbgb");

			}
			public String getDescription() {
				return "Genome Browser Datasets and Directories";
			}
		});

		fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

		return fileChooser;
	}

}
