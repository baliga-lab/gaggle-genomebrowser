package org.systemsbiology.genomebrowser.sqlite;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.BrowserApp;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.Io;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.util.CoordinateMapSelection;
import org.systemsbiology.genomebrowser.io.track.TrackBuilder;
import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.DatasetBuilder;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.genomebrowser.model.TrackImporter;
import org.systemsbiology.util.RunnableProgressReporter;

public class SqliteIo implements Io {
    private static final Logger log = Logger.getLogger(SqliteIo.class);
    public BrowserApp app;
    private SqliteDataSource db;

    public SqliteIo(BrowserApp app) {
        this.app = app;
    }
    private Dataset dataset() { return app.dataset(); }
    private String datasetUrl() { return app.options().datasetUrl; }
    private void setDatasetUrl(String url) { app.options().datasetUrl = url; }

    public Dataset loadDataset(File file) throws Exception {
        RunnableProgressReporter progressReporter = null;
        try {
            db = new SqliteDataSource(file);

            // report progress from a separate thread
            progressReporter = new RunnableProgressReporter(db.getProgress());
            progressReporter.start();

            // send reference to Runnable Progress Reporter in event
            app.publishEvent(new Event(this, "load started", progressReporter));

            Dataset dataset = db.loadDataset();
            setDatasetUrl(file.getAbsolutePath());

            app.publishEvent(new Event(this, "load completed"));
            return dataset;
        }
        catch (Exception e) {
            app.publishEvent(new Event(this, "load failed"));
            throw e;
        }
        finally {
            if (progressReporter!=null) progressReporter.done();
        }
    }

    public Dataset loadDataset(String path) {
        // TODO sqlite IO loadDataset(String path)
        return null;
    }

    public void saveDataset(File file, Dataset dataset) {
        // TODO sqlite IO saveDataset
    }
	
    public void setDatasetFile(File file) {
        log.debug("setDatasetFile ---> file = " + file);
        db = new SqliteDataSource(file);
        setDatasetUrl(file.getAbsolutePath());
    }

    public Dataset newDataset(File file, String name) throws Exception {
        db = new SqliteDataSource(file);
        setDatasetUrl(file.getAbsolutePath());
        return db.newDataset(name);
    }

    public TrackImporter getTrackImporter() {
        return new SqliteTrackImporter(SqliteTrackImporter.getConnectStringForFile(datasetUrl()));
    }

    public TrackBuilder getTrackBuilder(String type) {
        log.debug("getting track builder for track type: " + type);
        if ("quantitative.segment".equals(type)) {
            SqliteQuantitativeSegmentTrackBuilder tb = new SqliteQuantitativeSegmentTrackBuilder();
            tb.setConnectString(SqliteDataSource.getConnectStringForFile(datasetUrl()));
            tb.setDatasetUuid(dataset().getUuid());
            tb.setSqliteDataSource(db);
            return tb;
        }
        else if ("quantitative.segment.matrix".equals(type)) {
            SqliteQuantitativeSegmentMatrixTrackBuilder tb = new SqliteQuantitativeSegmentMatrixTrackBuilder();
            tb.setConnectString(SqliteDataSource.getConnectStringForFile(datasetUrl()));
            tb.setDatasetUuid(dataset().getUuid());
            tb.setSqliteDataSource(db);
            return tb;
        }
        else if ("quantitative.positional".equals(type)) {
            SqliteQuantitativePositionalTrackBuilder tb = new SqliteQuantitativePositionalTrackBuilder();
            tb.setConnectString(SqliteDataSource.getConnectStringForFile(datasetUrl()));
            tb.setDatasetUuid(dataset().getUuid());
            tb.setSqliteDataSource(db);
            return tb;
        }
        else if ("quantitative.positional.p.value".equals(type)) {
            SqliteIntensityPValuePositionalTrackBuilder tb = new SqliteIntensityPValuePositionalTrackBuilder();
            tb.setConnectString(SqliteDataSource.getConnectStringForFile(datasetUrl()));
            tb.setDatasetUuid(dataset().getUuid());
            tb.setSqliteDataSource(db);
            return tb;
        }
        else if ("gene".equals(type)) {
            SqliteGeneTrackBuilder tb = new SqliteGeneTrackBuilder();
            tb.setConnectString(SqliteDataSource.getConnectStringForFile(datasetUrl()));
            tb.setDatasetUuid(dataset().getUuid());
            tb.setSqliteDataSource(db);
            return tb;
        }
        else {
            throw new RuntimeException("Unrecognized track type: \"" + type + "\".");
        }
    }

    public DatasetBuilder getDatasetBuilder(File file) {
        return new SqliteDatasetBuilder(file);
    }

    public TrackSaver getTrackSaver() {
        return new TrackSaver(SqliteDataSource.getConnectStringForFile(datasetUrl()));
    }

    public CoordinateMap findCoordinateMap(String[] names) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        return db.findCoordinateMap(names);
    }

    public List<CoordinateMapSelection> findCoordinateMaps(String[] names) {
        try {
            SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
            return db.findCoordinateMaps(names);
        }
        catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // TODO if db is a member variable, why do I recreate it in these methods?
	
    public CoordinateMap loadCoordinateMap(String table) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        return db.loadCoordinateMap(table);
    }

    public void createCoordinateMapping(UUID datasetUuid, String name, Iterable<NamedFeature> mappings) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        db.createCoordinateMapping(datasetUuid, name, mappings);
    }

    public void writeBookmarks(BookmarkDataSource bookmarks, UUID datasetUuid) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        db.writeBookmarks(bookmarks, datasetUuid);
    }

    public void deleteBookmarks(String name, UUID datasetUuid) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        db.deleteBookmarks(name, datasetUuid);
    }

    public int countBookmarks(String name, UUID datasetUuid) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        return db.countBookmarks(name, datasetUuid);
    }
	
    public List<String> getBookmarkCollectionNames(UUID datasetUuid) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        return db.getBookmarkCollectionNames(datasetUuid);
    }
	
    public BookmarkDataSource loadBookmarks(String name) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        return db.loadBookmarks(name);
    }

    public void deleteTrack(UUID uuid) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        db.deleteTrack(uuid);
    }

    public List<Segment> getTrackCoordinateRange(UUID trackUuid) {
        SqliteDataSource db = new SqliteDataSource(SqliteDataSource.getConnectStringForFile(datasetUrl()));
        return db.getTrackCoordinateRange(trackUuid);
    }
}
