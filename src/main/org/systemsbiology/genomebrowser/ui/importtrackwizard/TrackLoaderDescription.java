package org.systemsbiology.genomebrowser.ui.importtrackwizard;


/**
 * Describes an implementation of the first stage of importing track data,
 * reading features from a source. An example is reading a text file from the
 * local file system in which every line is a feature. In theory, data may
 * instead come from a URL or database, although those are not implemented yet.
 */
public class TrackLoaderDescription {
	public final String name;
	public final String description;
	public final String[] columns;


	public TrackLoaderDescription(String name, String description, String[] columns) {
		this.name = name;
		this.description = description;
		this.columns = columns;
	}
}
