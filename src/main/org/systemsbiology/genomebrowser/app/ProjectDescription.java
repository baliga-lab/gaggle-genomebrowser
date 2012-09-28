/**
 * 
 */
package org.systemsbiology.genomebrowser.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.genomebrowser.model.Topology;

import static org.systemsbiology.util.StringUtils.isNullOrEmpty;


/**
 * Holds parameters necessary to create a new project (dataset). The user
 * can make selections in NewProjectDialog. These parameters are consumed
 * by NewProjectBuilder in a manner specific to each data source.
 */
public class ProjectDescription {
	public static String LOCAL_DATA_LABEL = "My own data";
	public static String DEFAULT_PROJECT_NAME = "New Project";
	private String organism;
	private File file;
	private String dataSource;
	private String projectName;
	private boolean removeUnassembledFragments = true;
	private boolean defaultProjectName;
	private boolean defaultFile;
	private boolean defaultDataSource;
	private boolean recognizedSpecies;
	private List<SequenceDescription> userSpecifiedSequences = new ArrayList<SequenceDescription>();
	private File genomeFile;


	public ProjectDescription() {}

	public ProjectDescription(String organism) {
		this.setOrganism(organism);
	}

	public ProjectDescription(String organism, String projectName, File file, String dataSource) {
		this.setOrganism(organism);
		this.setProjectName(projectName);
		this.setFile(file);
		this.setDataSource(dataSource);
	}

	/**
	 * Overwrite the member variable of this ProjectDescription with non-null
	 * values from another ProjectDescription.
	 */
	public void transfer(ProjectDescription description) {
		if (description.getOrganism() != null) this.setOrganism(description.getOrganism());
		if (description.getFile() != null) this.setFile(description.getFile());
		if (description.getDataSource() != null) this.setDataSource(description.getDataSource());
		if (description.getProjectName() != null) this.setProjectName(description.getProjectName());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("(ProjectDescription: ");
		sb.append("organism=").append(String.valueOf(getOrganism()));
		sb.append(", name=").append(String.valueOf(getProjectName()));
		sb.append(", file=").append(String.valueOf(getFile()));
		sb.append(", dataSource=").append(String.valueOf(getDataSource()));
		if (genomeFile!=null)
			sb.append(", genomeFile=").append(String.valueOf(genomeFile.getName()));
		sb.append(")");
		return sb.toString();
	}

	public void setOrganism(String organism) {
		this.organism = organism;
	}

	public String getOrganism() {
		return organism;
	}

	public void setFile(String filename) {
		if (isNullOrEmpty(filename)) return;
		setFile(new File(filename));
	}

	public void setFile(File file) {
		if (file==null || !file.equals(this.file)) {
			this.file = file;
			defaultFile = false;
		}
	}

	public File getFile() {
		return file;
	}

	public void setDataSource(String dataSource) {
		if (!dataSource.equals(this.dataSource)) {
			this.dataSource = dataSource;
			defaultDataSource = false;
		}
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setProjectName(String projectName) {
		if (isNullOrEmpty(projectName))
			this.projectName = DEFAULT_PROJECT_NAME;
		if (!projectName.equals(this.projectName)) {
			this.projectName = projectName;
			defaultProjectName = false;
		}
	}

	public String getProjectName() {
		return projectName;
	}

	public void addSequence(String name, int length, Topology topology) {
		userSpecifiedSequences.add(new SequenceDescription(name, length, topology));
	}

	public void clearSequences() {
		userSpecifiedSequences.clear();
	}

	public List<SequenceDescription> getSequenceDescriptions() {
		return userSpecifiedSequences;
	}

	// attributes may include:
	// user
	// creation date
	// refseq or accession number
	// taxid
	// clade
	// domain
	// UCSC db name
	// organism common name


	public boolean defaultProjectName() {
		return defaultProjectName;
	}

	public boolean defaultFile() {
		return defaultFile;
	}

	public boolean defaultDataSource() {
		return defaultDataSource;
	}

	public void setDefaultProjectName(boolean defaultProjectName) {
		this.defaultProjectName = defaultProjectName;
	}

	public void setDefaultFile(boolean defaultFile) {
		this.defaultFile = defaultFile;
	}

	public void setDefaultDataSource(boolean defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	public void setRemoveUnassembledFragments(boolean removeUnassembledFragments) {
		this.removeUnassembledFragments = removeUnassembledFragments;
	}
	
	public boolean getRemoveUnassembledFragments() {
		return this.removeUnassembledFragments;
	}

	public static class SequenceDescription {
		public final String name;
		public final int length;
		public final Topology topology;

		public SequenceDescription(String name, int length, Topology topology) {
			this.name = name;
			this.length = length;
			this.topology = topology;
		}

		@Override
		public String toString() {
			return "(" + name + ", " + length + ", " + topology + ")";
		}
	}

	public File getGenomeFile() {
		return genomeFile;
	}

	public void setGenomeFile(File genomeFile) {
		this.genomeFile = genomeFile;
	}

	public void resetDefaults() {
		if (defaultProjectName) projectName = null;
		if (defaultFile) file = null;
		if (defaultDataSource) dataSource = null;
	}

	public void setSequences(List<SequenceDescription> sequences) {
		this.userSpecifiedSequences.clear();
		this.userSpecifiedSequences.addAll(sequences);
	}

	public void setRecognizedSpecies(boolean recognizedSpecies) {
		this.recognizedSpecies = recognizedSpecies;
	}

	public boolean isRecognizedSpecies() {
		return recognizedSpecies;
	}
}