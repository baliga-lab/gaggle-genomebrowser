package org.systemsbiology.genomebrowser.app;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.util.CoordinateMapSelection;
import org.systemsbiology.genomebrowser.io.track.TrackBuilder;
import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.genomebrowser.ui.importtrackwizard.TrackImporter;
import org.systemsbiology.genomebrowser.event.EventListener;
import org.systemsbiology.genomebrowser.event.EventSupport;
import org.systemsbiology.genomebrowser.event.Event;

// TODO should all access to components go through ExternalAPI? just plugins?
// TODO ability to add a TrackRenderer as a plugin


/**
 * Components and plug-ins access application-wide functionality through
 * the external API interface.
 */
public interface ExternalAPI {

	// events
	public void addEventListener(EventListener listener);
	public void publishEvent(Event event);

	// UI
	public void addToolbar(String title, JToolBar toolbar, Action action);
	public void setVisibleToolbar(String title, boolean visible);
	public void addMenu(String title, Action[] actions);
	public void bringToFront();
	public void minimize();

	// application state
	public Options getOptions();
	public Dataset getDataset();
	public Segment getVisibleSegment();
	public String getSpecies();
	public void updateTrack(Track<Feature> track);

	// selections
	public Collection<Feature> getSelectedFeatures();
	public void selectFeaturesByName(List<String> names);
	public List<Segment> getSelectedSegments();
	public Segment getSelectedSegment();
	public Strand getSelectionStrandHint();
	public List<GeneFeature> getGenesIn(Sequence sequence, Strand strand, int start, int end);

	// bookmarks
	public BookmarkDataSource getOrCreateBookmarkDataSource(String name);
	public BookmarkDataSource getSelectedBookmarkDataSource();

	// dataset
	// do we want plugins to be able to mutate the Dataset directly?
	public UUID getDatasetUuid();

	// data tracks
	public void addTrack(Track<? extends Feature> track);
	public TrackImporter getTrackImporter();
	public TrackBuilder getTrackBuilder(String type);
	
	public List<String> getTrackTypes();

	// coordinate maps
	//public CoordinateMap findCoordinateMap(String[] names);
	public List<CoordinateMapSelection> findCoordinateMaps(String[] rowTitles);
	public CoordinateMap loadCoordinateMap(String table);
	public void createCoordinateMapping(UUID datasetUuid, String name, Iterable<NamedFeature> mappings);

	// utility
	public void showErrorMessage(String message, Exception e);
	public void showMessage(String string);
	public void requestShutdown();
	public void refresh();
	public JFrame getMainWindow();

}
