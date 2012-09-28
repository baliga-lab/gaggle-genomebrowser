package org.systemsbiology.genomebrowser.gaggle;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JToolBar;

import org.apache.log4j.Logger;
import org.systemsbiology.gaggle.core.Boss;
import org.systemsbiology.gaggle.core.Goose;
import org.systemsbiology.gaggle.core.datatypes.*;
import org.systemsbiology.gaggle.geese.common.GaggleConnectionListener;
import org.systemsbiology.gaggle.geese.common.RmiGaggleConnector;
import org.systemsbiology.gaggle.geese.common.actions.ConnectToBossAction;
import org.systemsbiology.gaggle.geese.common.actions.DisconnectFromBossAction;
import org.systemsbiology.genomebrowser.app.Event;
import org.systemsbiology.genomebrowser.app.ExternalAPI;
import org.systemsbiology.genomebrowser.app.Plugin;
import org.systemsbiology.genomebrowser.gaggle.GaggleToolbar.GaggleBroadcastData;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.util.FeatureUtils;
import org.systemsbiology.util.Attributes;
import org.systemsbiology.util.NiceTuple;
import org.systemsbiology.util.StringUtils;


/**
 * A Genome Browser plugin that provides gaggle connectivity.
 * @author cbare
 */
public class GenomeBrowserGoose implements Goose, GaggleConnectionListener, Plugin {
	private static final Logger log = Logger.getLogger(GenomeBrowserGoose.class);
	private static final String NAME = "Genome Browser";
	private Boss boss;
	private ExternalAPI api;
	private RmiGaggleConnector connector;
	private String gooseName = NAME;
	private GaggleToolbar toolbar;

	// ---- Actions ----------------------------------------------------------
	private ConnectToBossAction connectAction;
	private DisconnectFromBossAction disconnectAction;
	private BroadcastNamelistAction broadcastNamelistAction;
	private ToggleToolbarVisibleAction visibleAction;


	public GenomeBrowserGoose() {
		connector = new RmiGaggleConnector(this);
		connector.setVerbose(false);

		toolbar = new GaggleToolbar();

		// create actions
		connectAction = new ConnectToBossAction(connector);
		disconnectAction = new DisconnectFromBossAction(connector);
		broadcastNamelistAction = new BroadcastNamelistAction(this);

		toolbar.addStatusButtonActionListener(new ToggleConnectionAction());
		toolbar.addBroadcastActionListener(broadcastNamelistAction);
		toolbar.addShowActionListener(new ShowAction());
		toolbar.addHideActionListener(new HideAction());

		connector.addListener(connectAction);
		connector.addListener(disconnectAction);
		connector.addListener(broadcastNamelistAction);
		connector.addListener(toolbar);
		
	}

	public void setExternalApi(ExternalAPI api) {
		this.api = api;
	}

	public JToolBar getToolbarPanel() {
		return toolbar;
	}

	public void init() {
		log.info("initializing GenomeBrowserGoose (autostart=" + api.getOptions().autostartBoss + ")");
 
		visibleAction = new ToggleToolbarVisibleAction(toolbar, api);

		api.addEventListener(this);
		api.addMenu("Gaggle",  new Action[] {
				connectAction,
				disconnectAction,
				null,
				broadcastNamelistAction,
				null,
				visibleAction});
		api.addToolbar(GaggleToolbar.TITLE, toolbar, visibleAction);

		// Sometimes if the port is in a weird state (wake-up after sleep), trying
		// to connect can take a long time and finally fail. We don't want to wait
		// while starting up, so do this in its own thread.
		// TODO make connect to Gaggle cancelable?
		new Thread(new Runnable() {
			public void run() {
				try {
					log.info("trying to connect to Gaggle");
					// TODO gaggle connect can block for quite a while before timing out
					connector.setAutoStartBoss(api.getOptions().autostartBoss);
					connector.connectToGaggle();
				}
				catch (Exception e) {
					log.warn("GenomeBrowserGoose tried and failed to connect to Gaggle Boss");
				}
				finally {
					connector.setAutoStartBoss(true);
					log.info("connect to Gaggle " + ((boss==null) ? "failed." : "succeeded.") );
				}
			}
		}).start();
	}

	public void shuttingDown() {
		connector.disconnectFromGaggle(false);
		boss = null;
	}


	public void broadcast() {
		GaggleBroadcastData data = toolbar.getSelectedBroadcastData();
		if (data==GaggleToolbar.SELECTED_GENES)
			broadcastNamelist();
		else if (data.type=="dataset") {
			Dataset dataset = api.getDataset();
			GaggleTuple tuple = makeDatasetDescriptionTuple(data.name, dataset, api.getOptions().datasetUrl);
			try {
				boss.broadcastTuple(gooseName, toolbar.getTarget(), tuple);
			}
			catch (RemoteException e) {
				api.showErrorMessage("Error trying to broadcast dataset description.", e);
			}
		}
		else if (data.type=="coordinates") {
			Segment coordinates = api.getSelectedSegment();
			if (coordinates==null)
				coordinates = api.getVisibleSegment();
			Tuple tuple = new Tuple("coordinates");
			String species = api.getDataset().getAttributes().getString("species");
			if (species != null)
				tuple.addSingle(new Single("species", species));
			tuple.addSingle(new Single("sequence", coordinates.seqId));
			tuple.addSingle(new Single("start", coordinates.start));
			tuple.addSingle(new Single("end", coordinates.end));
			GaggleTuple gt = new GaggleTuple();
			gt.setName("coordinates");
			gt.setSpecies(species);
			gt.setData(tuple);
			try {
				boss.broadcastTuple(gooseName, toolbar.getTarget(), gt);
			}
			catch (RemoteException e) {
				api.showErrorMessage("Error trying to broadcast coordinates.", e);
			}
		}
		else {
			Dataset dataset = api.getDataset();
			Track<? extends Feature> track = dataset.getTrack(data.name);
			List<Segment> segments = api.getSelectedSegments();

			if (segments.size()==0) {
				api.showMessage("Make a selection before broadcasting.");
				return;
			}

			// make a Gaggle dataMatrix type out of the track data

			if (track instanceof Track.Quantitative<?>) {
				Track<Feature.Quantitative> quant = extractQuantitativeTrack(track);
				List<String> names = new ArrayList<String>();

				boolean isMatrixTrack = FeatureUtils.getFeatureClass(quant)==Feature.Matrix.class;
				int columns = 1;

				// first, construct a list of row names of the form chr+:1001-1200
				for (Segment segment : segments) {
					Iterable<Feature.Quantitative> features = quant.features(new FeatureFilter(dataset.getSequence(segment.seqId), segment.start, segment.end));
					for (Feature.Quantitative feature : features) {
						names.add(String.format("%s%s:%d-%d", feature.getSeqId(), feature.getStrand().toAbbreviatedString(), feature.getStart(), feature.getEnd()));
						if (isMatrixTrack) {
							// all features should have the same number of columns, but we check anyway just 'cause we're already in a loop.
							columns = Math.max(columns, ((Feature.Matrix)feature).getValues().length);
						}
					}
				}

				// construct a table of doubles (with one column) parallel to names
				double[][] values = new double[names.size()][columns];
				int i = 0;
				for (Segment segment : segments) {
					Iterable<Feature.Quantitative> features = quant.features(new FeatureFilter(dataset.getSequence(segment.seqId), segment.start, segment.end));
					for (Feature.Quantitative feature : features) {
						if (isMatrixTrack) {
							values[i] = ((Feature.Matrix)feature).getValues();
						}
						else {
							values[i][0] = feature.getValue();
						}
						i++;
					}
				}

				// construct column titles
				String[] columnTitles = new String[columns];
				if (isMatrixTrack) {
					for (int j=0; j<columns; j++) {
						columnTitles[j] = data.name + "_" + j;
					}
				}
				else {
					columnTitles[0] = data.name;
				}

				DataMatrix matrix = new DataMatrix(data.name);
				matrix.setSpecies(dataset.getAttributes().getString("species"));
				matrix.setRowTitlesTitle("Location");
				matrix.setColumnTitles(columnTitles);
				matrix.setRowTitles(names.toArray(new String[names.size()]));
				matrix.set(values);

				try {
					boss.broadcastMatrix(gooseName, null, matrix);
				}
				catch (RemoteException e) {
					api.showErrorMessage("Error trying to broadcast namelist.", e);
				}
			}
		}
	}

	/**
	 * We can broacast a tuple that reflects the structure of a dataset, which
	 * has sequences and tracks, all of which have key/value attributes.
	 */
	private GaggleTuple makeDatasetDescriptionTuple(String name, Dataset dataset, String filename) {
		GaggleTuple tuple = new GaggleTuple();
		tuple.setName(name);
		tuple.setSpecies(dataset.getAttributes().getString("species"));
		tuple.getData().addSingle(new Single("name", dataset.getName()));
		tuple.getMetadata().addSingle(new Single("from", "genome.browser"));
		tuple.getData().addSingle(new Single("uuid", dataset.getUuid().toString()));
		tuple.getData().addSingle(new Single("filename", filename));
		for (String key: dataset.getAttributes().keySet()) {
			Object value = dataset.getAttributes().get(key);
	        if (value instanceof String ||
	                value instanceof Integer ||
	                value instanceof Long ||
	                value instanceof Float ||
	                value instanceof Double ||
	                value instanceof Boolean ||
	                value instanceof GaggleData ||
	                value instanceof Tuple) {
				tuple.getData().addSingle(new Single(key, (Serializable)value));
			}
	        else {
				tuple.getData().addSingle(new Single(key, String.valueOf(value)));
	        }
		}
		Tuple sequences = new Tuple("sequences");
		for (Sequence seq: dataset.getSequences()) {
			Tuple seqDescription = new Tuple(seq.getSeqId());
			seqDescription.addSingle(new Single("length",seq.getLength()));
			seqDescription.addSingle(new Single("topology",seq.getTopology().toString()));
			seqDescription.addSingle(new Single("uuid",seq.getUuid().toString()));
			for (String key: seq.getAttributes().keySet()) {
				Object value = seq.getAttributes().get(key);
				if (value instanceof Serializable) {
					seqDescription.addSingle(new Single(key, (Serializable)value));
				}
			}
			sequences.addSingle(new Single(seq.getSeqId(), seqDescription));
		}
		tuple.getData().addSingle(new Single("sequences", sequences));

		Tuple tracks = new Tuple("tracks");
		for (Track<Feature> track : dataset.getTracks()) {
			Tuple trackDesc = new Tuple(track.getName());
			trackDesc.addSingle(new Single("uuid", track.getUuid().toString()));
			for (String key: track.getAttributes().keySet()) {
				Object value = track.getAttributes().get(key);
				if (value instanceof Serializable) {
					trackDesc.addSingle(new Single(key, (Serializable)value));
				}
			}
			tracks.addSingle(new Single(track.getName(), trackDesc));
		}
		tuple.getData().addSingle(new Single("tracks", tracks));
		return tuple;
	}

	/**
	 * exists just to isolate the unchecked warning
	 */
	@SuppressWarnings("unchecked")
	private Track.Quantitative<Feature.Quantitative> extractQuantitativeTrack(
			Track<? extends Feature> track) {
		return (Track.Quantitative<Feature.Quantitative>)track;
	}

	public void broadcastNamelist() {
		if (boss != null) {
			Collection<Feature> features = api.getSelectedFeatures();

			if (features.size()==0) {
				api.showMessage("Select some genes before broadcasting.");
				return;
			}

			String[] names = new String[features.size()];
			int i = 0;
			for (Feature feature : features) {
				// To make sure we use the canonical name of a gene when
				// broadcasting, we need to do this little check.
				// TODO consider ways to avoid instanceof to get canonical feature identifier
				if (feature instanceof GeneFeature)
					names[i++] = ((GeneFeature)feature).getName();
				else
					names[i++] = feature.getLabel();
			}
			String species = api.getSpecies();
			try {
				boss.broadcastNamelist(gooseName, toolbar.getTarget(), new Namelist("Namelist", species, names));
			}
			catch (RemoteException e) {
				api.showErrorMessage("Error trying to broadcast namelist.", e);
			}
		}
	}

	// broadcast other data types? sequence?

	// ----  GaggleConnectionListener methods --------------------------------

	public void setConnected(boolean connected, Boss boss) {
		broadcastNamelistAction.setEnabled(connected);
		
		if (connected) {
			this.boss = boss;
		}
		else {
			this.boss = null;
			this.gooseName = NAME;
		}
	}


	// ----  goose methods ---------------------------------------------------



	public String getName() {
		return gooseName;
	}

	public void setName(String name) {
		this.gooseName = name;
	}

	public void doBroadcastList() {
		log.warn("deprecated method doBroadcastList called and ignored.");
	}

	public void doExit() {
		log.info("GenomeBrowserGoose.doExit() called.");
		api.requestShutdown();
	}

	public void doHide() {
		api.minimize();
	}

	public void doShow() {
		api.bringToFront();
	}

	public void update(String[] gooseNames) {
		log.info("connected geese= " + Arrays.toString(gooseNames));
		toolbar.update(gooseName, gooseNames);
	}

	public void handleCluster(String source, Cluster cluster) {
		log.info("received cluster... doing nothing");
	}

	public void handleMatrix(String source, DataMatrix matrix) {
		if (matrix.getName()==null) matrix.setName("matrix");
		log.info("Gaggle broadcast received: DataMatrix \"" + matrix.getName() + "\" from " + source);

		try {
			DataMatrixTrackBuilder trackBuilder = new DataMatrixTrackBuilder();
			trackBuilder.setApi(api);
			trackBuilder.handleMatrix(source, matrix);
		}
		catch (Exception e) {
			log.error("Error receiving matrix broadcast", e);
		}
	}

	public void handleNameList(String source, Namelist nameList) {
		log.info("Gaggle broadcast received: Namelist from " + source);
		api.selectFeaturesByName(Arrays.asList(nameList.getNames()));
	}

	public void handleNetwork(String source, Network network) {
		// anything to be done here?
		// display a transcription factor and the genes it regulates?
		log.info("received network... doing nothing");
	}

	public void handleTuple(String source, GaggleTuple gaggleTuple) {
		log.info("Gaggle broadcast received Tuple from " + source + ": \"" + gaggleTuple.getName() + "\"");
		log.info(gaggleTuple.getData());

		// TODO handle tuple selections, a track, a mapping from id to coordinates

		NiceTuple tuple = new NiceTuple(gaggleTuple.getData());

		if ("import.track".equals(tuple.getString("command"))) {
			log.info("Got import.track command from goose " + source);
			String type = tuple.getString("track.type");
			String name = tuple.getString("track.name", "new track");
			String tableName = tuple.getString("table.name", "temp");
			boolean auto = "true".equals( tuple.getString("auto.confirm", "false") );

			log.info("track name: " + name);
			log.info("track type: " + type);

			AnotherAttemptAtImportTrack importer = new AnotherAttemptAtImportTrack(api);
			importer.setTrackName(name);
			importer.setTrackType(type);
			importer.setTableName(tableName);

			// copy attributes if any
			Object attrs = tuple.get("attributes");
			log.info("attributes = " + attrs);
			if (attrs != null) log.info("attributes = " + attrs.getClass().getSimpleName());
			if (attrs != null && attrs instanceof Tuple) {
				importer.setAttributes(tupleToAttributes((Tuple)attrs));
			}

			if (auto) {
				importer.doImport();
			}
			else {
				importer.openImportDialog();
			}
		}

		else if ("coordinates".equals(tuple.getName()) || "coordinates".equals(tuple.getString("command"))) {
			log.info("Got coordinates command from goose " + source);
			Segment segment = new Segment(tuple.getString("sequence"), tuple.getInt("start"), tuple.getInt("end"));
			api.publishEvent(new Event(this, "goto", segment));
		}
		
		// TODO change to set.attributes
		
		else if ("set.track.attributes".equals(tuple.getName()) || "set.track.attributes".equals(tuple.getString("command"))) {
			log.info("Got set.track.attributes command from goose " + source);
			// TODO get UUID for either track or sequence
			// TODO modify attributes by UUID
			String trackName = tuple.getString("track.name");
			if (StringUtils.isNullOrEmpty(trackName)) {
				log.warn("No track.name specified in set.track.attributes command.");
				return;
			}
			Dataset dataset = api.getDataset();
			Track<Feature> track = dataset.getTrack(trackName);
			if (track==null) {
				log.warn("No track by the name: " + trackName);
				return;
			}

			Object attrs = tuple.get("attributes");
			log.info("attributes = " + attrs);
			if (attrs != null && attrs instanceof Tuple) {
				track.getAttributes().putAll(tupleToAttributes((Tuple)attrs));
				// save track info
				api.updateTrack(track);
				// signal a repaint
				api.publishEvent(new Event(this, "set.track.attributes", true));
			}
		}
	}

	// ------------------------------------------------------------------------

	public void receiveEvent(Event event) {
		log.info(event);
		if ("shutdown".equals(event.getAction())) {
			shuttingDown();
		}
		else if ("set dataset".equals(event.getAction())) {
			resetGaggleBroadcastData();
		}
	}

	private void resetGaggleBroadcastData() {
		toolbar.clearBroadcastMenu();
		Dataset dataset = api.getDataset();
		if (dataset != Dataset.EMPTY_DATASET) {
			toolbar.addGaggleData(GaggleToolbar.SELECTED_GENES);
			toolbar.addGaggleData(new GaggleBroadcastData("Coordinates", "coordinates"));
			toolbar.addGaggleData(new GaggleBroadcastData("Description of dataset: " + dataset.getName(), "dataset"));

			// add an entry for each track
			for (Track<Feature> track : dataset.getTracks()) {
				// only broadcast quantitative tracks, for now.
				if (track instanceof Track.Quantitative)
					toolbar.addGaggleData(track.getName(), track.getUuid());
			}
		}
	}

	private Attributes tupleToAttributes(Tuple tuple) {
		Attributes attr = new Attributes();
		if (tuple != null) {
			for (Single single : tuple.getSingleList()) {
				if (single.getValue() instanceof String ||
						single.getValue() instanceof Number ||
						single.getValue() instanceof Boolean ||
						single.getValue() instanceof Character)
				{
					attr.put(single.getName(), single.getValue());
				}
				else {
					log.warn("skipping attribute value: " + String.valueOf(single.getValue()));
				}
			}
		}
		return attr;
	}

	class ShowAction extends AbstractAction {
		public void actionPerformed(ActionEvent event) {
			try {
				if (boss!=null) {
					String target = toolbar.getTarget();
					boss.show(target);
				}
			}
			catch (Exception e) {
				log.warn("Exception in gaggle show operation:", e);
			}
		}
	}

	class HideAction extends AbstractAction {
		public void actionPerformed(ActionEvent event) {
			try {
				if (boss!=null) {
					String target = toolbar.getTarget();
					boss.hide(target);
				}
			}
			catch (Exception e) {
				log.warn("Exception in gaggle hide operation:", e);
			}
		}
	}

	class ToggleConnectionAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			if (boss==null) {
				connectAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "connect"));
			}
			else {
				disconnectAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "disconnect"));
			}
		}
	}
}
