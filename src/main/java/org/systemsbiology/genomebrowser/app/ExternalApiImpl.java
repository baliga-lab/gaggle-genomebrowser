package org.systemsbiology.genomebrowser.app;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.gaggle.CoordinateMapSelection;
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
import org.systemsbiology.genomebrowser.util.TrackUtils;


public class ExternalApiImpl implements ExternalAPI {
	Application app;


	public ExternalApiImpl(Application app) {
		this.app = app;
	}

	public void addEventListener(EventListener listener) {
		app.addEventListener(listener);
	}

	public void publishEvent(Event event) {
		app.publishEvent(event);
	}


	public Dataset getDataset() {
		return app.getDataset();
	}

	public void updateTrack(Track<Feature> track) {
		app.updateTrack(track);
	}

	public Options getOptions() {
		return app.options;
	}

	public Segment getVisibleSegment() {
		return app.getUi().getVisibleSegment();
	}

	public String getSpecies() {
		String species = app.getDataset().getAttributes().getString("species");
		if (species==null)
			species = app.getDataset().getAttributes().getString("organism");
		if (species==null)
			species = "unknown";
		return species;
	}


	public Collection<Feature> getSelectedFeatures() {
		return app.selections.getSelectedFeatures();
	}

	public void selectFeaturesByName(List<String> keywords) {
		app.search.search(keywords);
		app.selections.selectFeatures(app.search.getResults(), false);
	}

	public void showErrorMessage(String message, Exception e) {
		publishEvent(new Event(this, "error", new Exception(message, e)));
	}

	public void showMessage(String message) {
		publishEvent(new Event(this, "message", message));
	}


	public void addMenu(String title, Action[] actions) {
		app.getUi().insertMenu(title, actions);
	}

	public void addToolbar(String title, JToolBar toolbar, Action action) {
		app.getUi().addToolbar(title, toolbar, action);
	}

	public void setVisibleToolbar(String title, boolean visible) {
		app.getUi().setVisibleToolbar(title, visible);
	}

	// TODO getMainWindow potentially allows clients to violate thread containment of mainWindow?
	// used when receiving matrix broadcasts
	public JFrame getMainWindow() {
		return app.getUi().getMainWindow();
	}

	public void bringToFront() {
		app.getUi().bringToFront();
	}

	public void minimize() {
		app.getUi().minimize();
	}

	public void requestShutdown() {
		app.shutdown(0);
	}

	public void addTrack(Track<? extends Feature> track) {
		app.getDataset().addTrack(track);
		refresh();
	}

//	public CoordinateMap findCoordinateMap(String[] names) {
//		return app.findCoordinateMap(names);
//	}

	public List<CoordinateMapSelection> findCoordinateMaps(String[] names) {
		return app.findCoordinateMaps(names);
	}

	public CoordinateMap loadCoordinateMap(String table) {
		return app.loadCoordinateMap(table);
	}

	public TrackImporter getTrackImporter() {
		return app.io.getTrackImporter();
	}

	public void createCoordinateMapping(UUID datasetUuid, String name, Iterable<NamedFeature> mappings) {
		app.io.createCoordinateMapping(datasetUuid, name, mappings);
	}

	public UUID getDatasetUuid() {
		return app.getDataset().getUuid();
	}

	public void refresh() {
		app.trackManager.refresh();
		app.getUi().refresh();
	}

	public List<Segment> getSelectedSegments() {
		return app.selections.getSegments();
	}

	public Segment getSelectedSegment() {
		return app.selections.getSingleSelection();
	}

	public List<String> getTrackTypes() {
		return app.trackManager.getTrackRendererRegistry().getTrackTypes();
	}

	public TrackBuilder getTrackBuilder(String type) {
		return app.io.getTrackBuilder(type);
	}

	public Strand getSelectionStrandHint() {
		return app.selections.getStrandHint();
	}

	public BookmarkDataSource getOrCreateBookmarkDataSource(String name) {
		return app.bookmarkCatalog.findOrCreate(name);
	}

	public BookmarkDataSource getSelectedBookmarkDataSource() {
		return app.bookmarkCatalog.getSelected();
	}

	public List<GeneFeature> getGenesIn(Sequence sequence, Strand strand, int start, int end) {
		return TrackUtils.findGenesIn(app.getDataset(), sequence, strand, start, end);
	}

}
