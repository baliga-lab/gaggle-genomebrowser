package org.systemsbiology.genomebrowser.gaggle;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ExternalAPI;
import org.systemsbiology.genomebrowser.io.track.TrackBuilder;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.genomebrowser.model.HeuristicSequenceMapper;
import org.systemsbiology.genomebrowser.model.Sequence;

// TODO reorganize import track code
// TODO import track from a SQL table
// Still trying to work out a convenient and super-flexible framework for
// importing data. There are several ways to import tracks into the GB
// and I'd like to have them all share the same underlying code.
// The situation now is something of a mess.

public class AnotherAttemptAtImportTrack {
	private static final Logger log = Logger.getLogger(AnotherAttemptAtImportTrack.class);
	private String name;
	private String type;
	private String tableName;
	private ExternalAPI api;
	private Attributes attributes;

	public AnotherAttemptAtImportTrack(ExternalAPI api) {
		this.api = api;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public void setTrackName(String name) {
		this.name = name;
	}

	public void setTrackType(String type) {
		this.type = type;
	}

	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}
	
	
	public void doImport() {
		
		if (attributes==null) {
			attributes = new Attributes();
		}
		if (!attributes.containsKey("created-on"))
			attributes.put("created-on", String.format("%TF %<TT", new Date()));
		if (!attributes.containsKey("top"))
			attributes.put("top", 0.10);
		if (!attributes.containsKey("height"))
			attributes.put("height", 0.15);
		if (!attributes.containsKey("created-by"))
			attributes.put("created-by", System.getProperty("user.name"));

		if (!attributes.containsKey("viewer")) {
			if ("quantitative.segment".equals(type)) {
				attributes.put("viewer", "Scaling");
				attributes.put("color", "0x80336699");
			}
			else if ("quantitative.positional".equals(type)) {
				attributes.put("viewer", "Scaling");
				attributes.put("color", "0x80006600");
			}
			else if ("quantitative.positional.p.value".equals(type)) {
				attributes.put("viewer", "Scaling");
				attributes.put("color", "0x80006600");
			}
			else if ("quantitative.segment.matrix".equals(type)) {
				attributes.put("viewer", "MatrixHeatmap");
				attributes.put("split.strands", true);
				attributes.put("overlap", true);
			}
		}
		log.info("viewer = " + attributes.getString("viewer"));

		List<String> sequenceNames = new ArrayList<String>();
		for (Sequence seq : api.getDataset().getSequences()) {
			sequenceNames.add(seq.getSeqId());
		}

		HeuristicSequenceMapper seqMap = new HeuristicSequenceMapper();
		seqMap.setStandardSequenceNames(sequenceNames);

		try {
			TrackBuilder trackBuilder = api.getTrackBuilder(type);
			trackBuilder.setSource(tableName);
			trackBuilder.startNewTrack(name, type);
			trackBuilder.applySequenceMapper(seqMap);
			trackBuilder.setAttributes(attributes);
			trackBuilder.processFeatures();
			api.addTrack(trackBuilder.getFinishedTrack());
		}
		catch (Exception e) {
			api.showErrorMessage("Error receiving track " + name, e);
		}

	}


	public void openImportDialog() {
		// open dialog on swing event thread
		Runnable r = new Runnable() {
			public void run() {
				final ReceiveTrackDialog dialog = new ReceiveTrackDialog(api);
				dialog.setTrackName(name);
				dialog.setTrackType(type);
				dialog.addDialogListener(new DialogListener() {
					public void cancel() {
						log.info("receive track canceled");
						dialog.close();
					}
					public void error(String message, Exception e) {
						api.showErrorMessage(message, e);
					}
					public void ok(String action, Object resultObject) {
						ReceiveTrackDialog.Result result = (ReceiveTrackDialog.Result)resultObject;
						type = result.type;
						name = result.name;
						doImport();
						dialog.close();
					}
				});
				dialog.setVisible(true);
			}
		};
		SwingUtilities.invokeLater(r);
	}

}
