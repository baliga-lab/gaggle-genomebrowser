package org.systemsbiology.genomebrowser.app;

import java.io.File;

import org.systemsbiology.ucscgb.UCSCGB;
import org.systemsbiology.util.FileUtils;
import static org.systemsbiology.util.StringUtils.isNullOrEmpty;
import static org.systemsbiology.genomebrowser.app.ProjectDescription.DEFAULT_PROJECT_NAME;


/**
 * Helps the user by setting defaults for a newly created project where the
 * genome data will be automatically imported (from UCSC for now and maybe
 * NCBI or other sources later). Used by NewProjectDialog. Defaults can be
 * overridden in NewProjectOptionsDialog.
 * 
 * @see org.systemsbiology.genomebrowser.ui.NewProjectDialog
 * @see org.systemsbiology.genomebrowser.ui.NewProjectDialog.NewProjectOptionsDialog
 * @author cbare
 */
public class ProjectDefaults {
	private Options options;
	private UCSCGB ucscgb = new UCSCGB();


	public ProjectDefaults(Options options) {
		this.options = options;
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	/**
	 * Take a partially populated ProjectDescription and apply defaults to
	 * unpopulated members.
	 */
	public void apply(ProjectDescription desc) {
		if (isNullOrEmpty(desc.getOrganism())) {
			desc.setProjectName(DEFAULT_PROJECT_NAME);
			desc.setDefaultProjectName(true);
			desc.setRecognizedSpecies(false);
			desc.setDataSource(ProjectDescription.LOCAL_DATA_LABEL);
			desc.setFile((File)null);
			desc.setDefaultFile(true);
		}
		else {
	
			// by default, project is named after organism
			if (isNullOrEmpty(desc.getProjectName()) || DEFAULT_PROJECT_NAME.equals(desc.getProjectName())) {
				desc.setProjectName(desc.getOrganism());
				desc.setDefaultProjectName(true);
			}
	
			// default data source (for now) is UCSC
			if (isNullOrEmpty(desc.getDataSource())) {
				if (ucscgb.hasSpecies(desc.getOrganism())) {
					desc.setDataSource("UCSC");
					desc.setDefaultDataSource(true);
					desc.setRecognizedSpecies(true);
				}
				else {
					desc.setRecognizedSpecies(false);
					desc.setDataSource(ProjectDescription.LOCAL_DATA_LABEL);
					desc.setDefaultDataSource(true);
				}
			}
	
			// construct a default filename
			if (desc.getFile()==null) {
				desc.setFile(getUniqDefaultFile(desc.getOrganism()));
				desc.setDefaultFile(true);
			}
		}
	}

	/**
	 * Convert a species name into a valid filename by replacing all
	 * non-alphanumeric characters with an underscore. Puts files in
	 * the dataDirectory specified in the options object. Makes sure
	 * we don't put multiple datasets in the same DB by using a unique
	 * filename, appending digits, if necessary.
	 */
	public File getUniqDefaultFile(String organism) {
		return FileUtils.uniquify(getDefaultFile(organism));
	}

	/**
	 * Convert a species name into a valid filename by replacing all
	 * non-alphanumeric characters with an underscore. Puts files in
	 * the dataDirectory specified in the options object.
	 */
	public File getDefaultFile(String organism) {
		return new File(options.dataDirectory, FileUtils.toValidFilename(organism)+".hbgb");
	}
}
