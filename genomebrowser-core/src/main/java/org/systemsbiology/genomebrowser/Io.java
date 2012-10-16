package org.systemsbiology.genomebrowser;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.util.CoordinateMapSelection;
import org.systemsbiology.genomebrowser.io.track.TrackBuilder;
import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.DatasetBuilder;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.genomebrowser.sqlite.TrackSaver;
import org.systemsbiology.genomebrowser.model.TrackImporter;

/**
 * Interface to hide dataset CRUD behind.
 * @author cbare
 */
public interface Io {
	public Dataset loadDataset(File file) throws Exception;
	public Dataset loadDataset(String path) throws Exception;
	public void saveDataset(File file, Dataset dataset) throws Exception;
	public TrackImporter getTrackImporter();
	public Dataset newDataset(File file, String name) throws Exception;
	public DatasetBuilder getDatasetBuilder(File file);

	// TODO fixme unify creation of new datasets
	public void setDatasetFile(File file);

	// TODO remove TrackSaver
	public TrackSaver getTrackSaver();

	public CoordinateMap findCoordinateMap(String[] names);
	public List<CoordinateMapSelection> findCoordinateMaps(String[] names);
	public CoordinateMap loadCoordinateMap(String name);
	public void createCoordinateMapping(UUID datasetUuid, String name, Iterable<NamedFeature> mappings);

	public void deleteTrack(UUID uuid);
	public List<Segment> getTrackCoordinateRange(UUID trackUuid);

	public List<String> getBookmarkCollectionNames(UUID datasetUuid);
	public void writeBookmarks(BookmarkDataSource bookmarks, UUID datasetUuid);
	public void deleteBookmarks(String name, UUID datasetUuid);
	public int countBookmarks(String name, UUID datasetUuid);
	public BookmarkDataSource loadBookmarks(String name);
	public TrackBuilder getTrackBuilder(String type);
}
