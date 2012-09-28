package org.systemsbiology.genomebrowser.visualization.tracks;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.visualization.tracks.renderers.GeneTrackRenderer;
import org.systemsbiology.genomebrowser.visualization.tracks.renderers.TypedGeneTrackRenderer;


/**
 * The most important task of the TrackManager is to hold a set of renderers
 * which will be called upon to draw the tracks. It depends on a dataset (from
 * which it draws the track data) and a TrackRenderer<?>Registry which maps
 * particular renderers to each type of data.
 *
 * TrackManager is supposed to play the role of the Visualization class
 * described in Heer 2006 (Software Design Patterns for Information Visualization).
 * This might need some more thought and work.
 * 
 * There are likely threading issues here. My original intent was to confine the
 * track manager to the swing thread, but I'm not sure that always happens.
 *
 * @author cbare
 */
public class TrackManager implements Iterable<TrackRenderer> {
	private static final Logger log = Logger.getLogger(TrackManager.class);
	private static final ZOrderComparator zorder = new ZOrderComparator();
	private Dataset dataset;
	private TrackRendererRegistry registry;
	// used CopyOnWriteArrayList here for thread-safe iteration
	private List<TrackRenderer> renderers = new CopyOnWriteArrayList<TrackRenderer>();
	private TrackRenderer genomeTrackRenderer;
	private List<Track<? extends Feature>> tracks = new LinkedList<Track<? extends Feature>>();
	private Track<GeneFeature> genomeTrack;


	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
		log.info("set dataset: " + dataset.getName());
		refresh();
	}

	public void setTrackRendererRegistry(TrackRendererRegistry registry) {
		this.registry = registry;
	}

	public TrackRendererRegistry getTrackRendererRegistry() {
		return registry;
	}

	public List<Track<? extends Feature>> getTracks() {
		return new ArrayList<Track<? extends Feature>>(tracks);
	}

	public Track<? extends Feature> getTrack(String name) {
		if (name==null) return null;
		for (Track<? extends Feature> track: tracks) {
			if (name.equals(track.getName()))
				return track;
		}
		return null;
	}

	public Track<? extends Feature> getTrack(UUID uuid) {
		if (uuid==null) return null;
		for (Track<? extends Feature> track: tracks) {
			if (uuid.equals(track.getUuid()))
				return track;
		}
		return null;
	}

	/**
	 * Reconstruct track renderers from tracks.
	 */
	@SuppressWarnings("unchecked")
	public void refresh() {
		log.info("Track manager refreshing....");

		// added for importing tracks?
		tracks.clear();
		tracks.addAll(dataset.getTracks());
		Collections.sort(tracks, zorder);

		renderers.clear();
		genomeTrackRenderer = null;
		boolean foundGenome = false;

		// check for empty dataset to avoid "Can't find genome track" warning
		if (tracks.size()==0) {
			log.info("No tracks");
			return;
		}

		for (Track<? extends Feature> track: tracks) {
			if (track.getAttributes().getBoolean("visible", true)) {
				TrackRenderer renderer = registry.createTrackRenderer(track);
				renderers.add(renderer);
				log.info("created renderer for track: " + track.getName() + ", " + renderer.getClass().getName());

				if (!foundGenome && ("Genome".equals(track.getName()) || "Genes".equals(track.getName()))) {
					genomeTrackRenderer = renderer;
					genomeTrack = (Track<GeneFeature>)track;
					foundGenome = true;
				}
			}
		}

		// if we haven't found a genome track, pick the first track whose renderer is a type of genome renderer
		if (!foundGenome) {
			for (TrackRenderer renderer : renderers) {
				if (renderer instanceof GeneTrackRenderer || renderer instanceof TypedGeneTrackRenderer) {
					genomeTrackRenderer = renderer;
					genomeTrack = (Track<GeneFeature>)renderer.getTrack();
					foundGenome = true;
				}
			}
		}
		if (foundGenome)
			log.info("Genome Track found: " + genomeTrack.getName());
		else
			log.warn("Couldn't find genome track.");
	}

	public Iterator<TrackRenderer> iterator() {
		return renderers.iterator();
	}

	public Track<GeneFeature> getGenomeTrack() {
		return genomeTrack;
	}

	/**
	 * The genome track (a track named "Genome") is drawn by GenomeViewPanel
	 * immediately, whereas redraws for other data tracks are scheduled and
	 * rendered off-screen. This method exists so that GenomeViewPanel can
	 * quickly get the renderer for the genome track.
	 * @return the TrackRenderer for the genome track or null of none exists.
	 */
	public TrackRenderer getGenomeTrackRenderer() {
		return genomeTrackRenderer;
	}

	public List<Track<? extends Feature>> getTracksAt(Point p) {
		List<Track<? extends Feature>> tracks = new ArrayList<Track<? extends Feature>>();
		for (TrackRenderer renderer : renderers) {
			if (renderer.containsPoint(p)) {
				tracks.add(renderer.getTrack());
			}
		}
		return tracks;
	}

	public TrackRenderer getRendererFor(Track<? extends Feature> track) {
		if (track==null) return null;
		for (TrackRenderer renderer: renderers) {
			if (track.equals(renderer.getTrack()))
				return renderer;
		}
		return null;
	}

	public List<String> getOverlayGroups() {
	    Set<String> overlayGroups = new HashSet<String>();
	    for (Track<? extends Feature> track: tracks) {
	        if (track.getAttributes().containsKey("overlay"))
	            overlayGroups.add(track.getAttributes().getString("overlay"));
	    }
	    List<String> results = new ArrayList<String>(overlayGroups);
	    Collections.sort(results);
	    return results;
	}


	/**
	 * assign z-order attribute based on position in tracks list
	 */
	private void reassignZOrders() {
		for (int i=0; i<tracks.size(); i++) {
			tracks.get(i).getAttributes().put("z-order", i);
		}
	}

	/**
	 * adjust z-order of track
	 * @param track null OK.
	 */
	public void forward(Track<? extends Feature> track) {
		int index = tracks.indexOf(track);
		if (index < tracks.size()-1) {
			tracks.remove(track);
			tracks.add(index+1, track);
		}
		reassignZOrders();
	}

	/**
	 * adjust z-order of track
	 * @param track null OK.
	 */
	public void back(Track<? extends Feature> track) {
		int index = tracks.indexOf(track);
		if (index > 0) {
			tracks.remove(track);
			tracks.add(index-1, track);
		}
		reassignZOrders();
	}

	/**
	 * adjust z-order of track
	 * @param track null OK.
	 */
	public void sendToFront(Track<? extends Feature> track) {
		int index = tracks.indexOf(track);
		if (index < tracks.size()-1) {
			tracks.remove(track);
			tracks.add(track);
		}
		reassignZOrders();
	}

	/**
	 * adjust z-order of track
	 * @param track null OK.
	 */
	public void sendToBack(Track<? extends Feature> track) {
		int index = tracks.indexOf(track);
		if (index > 0) {
			tracks.remove(track);
			tracks.add(0, track);
		}
		reassignZOrders();
	}
	
	public void setVisibility(String group, boolean visible) {
		for (Track<Feature> track : dataset.getTracks()) {
			String g = track.getAttributes().getString("groups");
			if (g!=null && g.equals(group)) {
				track.getAttributes().put("visible", visible);
			}
		}
	}

	public void setVisibility(Track<Feature> track, boolean visible) {
		track.getAttributes().put("visible", visible);
	}


	// ---- events ------------------------------------------------------------

//	public void receiveEvent(Event event) {
//		System.out.println("+++++ TrackManger got " + event.getAction());
//		if (event.getAction().equals("set dataset")) {
//			Dataset dataset = (Dataset)event.getData();
//				this.setDataset(dataset);
//		}
//	}
}

class ZOrderComparator implements Comparator<Track<? extends Feature>> {
	public int compare(Track<? extends Feature> t1, Track<? extends Feature> t2) {
		if (t1==null && t2==null) return 0;
		if (t1==null) return -1;
		if (t2==null) return 1;
		int z1 = t1.getAttributes().getInt("z-order", -1);
		int z2 = t2.getAttributes().getInt("z-order", -1);
		return z1-z2;
	}
}
