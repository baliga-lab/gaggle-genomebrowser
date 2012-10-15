package org.systemsbiology.genomebrowser.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.event.EventListener;
import org.systemsbiology.genomebrowser.event.EventSupport;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.FeatureUtils;
import org.systemsbiology.util.Selectable;


// TODO thread safety of selections
// Selections will be accessed by the UI thread and the Gaggle RMI thread, at least.

// TODO update selections by events only
// Right now parts of the code directly manipulate what's
// selected in the UI. We should update the selections object
// and use events to redraw the UI as necessary, MVC style.


/**
 * Manage selections. There are two kinds of selections, a set of selected segments
 * on the genome and features that implement Selectable.
 * 
 * So far, selections are mutated by mouse events in GenomeViewPanel (by direct access),
 * UI methods gotoAndSelectFeature(s), and deselect, and by GenomeBrowserGoose through
 * the external API. 
 * 
 * @author cbare
 */
public class Selections implements EventListener {
	private static final Logger log = Logger.getLogger(Selections.class);
	private List<Segment> segments = new ArrayList<Segment>();

	// we sometimes need strand information (for example, bookmarks) but Segments
	// don't store what strand they belong to. Here's a hacky halfway point to
	// storing Locations instead.
	private Strand strandHint = Strand.none;

	// TODO generalize selection of features
	
	// HACK
	// This is something of a dirty hack. It introduces a hidden dependency on
	// the selectable features (which are only genes at this point) being kept
	// in memory, which is the case with the list based implementation of
	// GeneTrack. For other features to be selectable we may have to give each
	// feature an id field, which I'd like to avoid.
	// Maybe a better approach would be to determine selected features by name
	// or by their containment in selected segments.
	
	// Features need to know if they're selected to support speedy rendering.
	// We don't want to have to look up features in a list or a hashtable while
	// we're rendering.

	// Features don't have an ID field, so matching up a feature with its entry
	// in a selected list is going to be tricky at best. Label or position
	// would have to serve as a unique identifier.
	
	// Keeping a features list doesn't work for flyweight features at all.

	private Set<Feature> features = new HashSet<Feature>();
	private EventSupport eventSupport = new EventSupport();
	private boolean suppressEvents = false;
	
	/**
	 * Indicated whether the selections changed due to input by the user (say,
	 * mouse events) or whether it's being done programatically. In some cases,
	 * we need to differentiate between the two.
	 */
	private boolean primary = false;



	public Selections() {
	}


	
	public void selectFeatures(Iterable<Feature> features, boolean primary) {
		this.primary = primary;
		suppressEvents = true;
		int n = 0;
		for (Feature feature: features) {
			selectFeature(feature, primary);
			n++;
		}
		suppressEvents = false;
		fireSelectionsChangedEvent("select " + n + " features");
	}

	public void selectFeature(Feature feature, boolean primary) {
		this.primary = primary;
		if (feature==null) return;
		if (feature instanceof Selectable) {
			this.features.add(feature);
			((Selectable)feature).setSelected(true);
		}
		log.info("selecting = " + feature);
		addSegment(new Segment(feature.getSeqId(), feature.getStart(), feature.getEnd()), primary);
	}

	public void deselectFeature(Feature feature, boolean primary) {
		this.primary = primary;
		if (feature==null) return;
		if (feature instanceof Selectable) {
			this.features.remove(feature);
			((Selectable)feature).setSelected(false);
		}
		removeSegment(new Segment(feature.getSeqId(), feature.getStart(), feature.getEnd()), primary);
		log.info("deselected = " + feature + "-" + ((Selectable)feature).selected());
	}

	public void toggleSelection(Feature feature, boolean primary) {
		this.primary = primary;
		if (feature==null) return;
		if (feature instanceof Selectable) {
			Selectable selectable = (Selectable)feature;
			if (selectable.selected()) {
				deselectFeature(feature, primary);
			}
			else {
				selectFeature(feature, primary);
			}
		}
	}

	public void setStrandHint(Strand strand) {
		this.strandHint = strand; 
	}

	public void replaceSelection(Segment segment, boolean primary) {
		this.primary = primary;
		_clear();
		segments.add(segment);
		fireSelectionsChangedEvent("replace segment " + segment.toString());
	}
	
	public void replaceSelection(Feature feature, boolean primary) {
		this.primary = primary;
		_clear();
		selectFeature(feature, primary);
	}

	public void addSegment(Segment segment, boolean primary) {
		this.primary = primary;
		// if segment overlaps any other segment, combine them
		Segment existing = null;
		Iterator<Segment> iterator = segments.iterator();
		while (iterator.hasNext()) {
			existing = iterator.next();
			if (existing.overlaps(segment)) {
				segment = existing.expandToInclude(segment);
				iterator.remove();
				break;
			}
		}
		segments.add(segment);
		fireSelectionsChangedEvent("add segment " + segment.toString());
	}

	/**
	 * Remove the given segment from the selection unless it would
	 * create a hole ('cause we're lazy).
	 */
	public void removeSegment(Segment segment, boolean primary) {
		this.primary = primary;
		Segment existing = null;
		Iterator<Segment> iterator = segments.iterator();
		while (iterator.hasNext()) {
			existing = iterator.next();
			if (existing.overlaps(segment)) {
				segment = existing.trimOverlap(segment);
				iterator.remove();
				if (segment!=null) {
					segments.add(segment);
					fireSelectionsChangedEvent("remove segment " + segment.toString());
				}
				break;
			}
		}
	}

	public void clear(boolean primary) {
		this.primary = primary;
		_clear();
		fireSelectionsChangedEvent("clear");
	}

	private void _clear() {
		segments.clear();
		for (Feature s: features) {
			((Selectable)s).setSelected(false);
		}
		features.clear();
	}


	public Strand getStrandHint() {
		return strandHint;
	}

	public List<Segment> getSegments() {
		return segments;
	}

	/**
	 * return a segment enclosing all selections on the given sequence.
	 */
	public Segment getEnclosingSegment(String sequence) {
		if (segments.size() == 0)
			return null;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (Segment segment : segments) {
			if (segment.seqId.equals(sequence)) {
				min = Math.min(min, segment.start);
				max = Math.max(max, segment.end);
			}
		}
		if (max < min)
			return null;
		return new Segment(sequence, min, max);
	}

	/**
	 * @return current single selection, most recent selection if there are multiple selections, or null if there are none. 
	 */
	public Segment getSingleSelection() {
		if (segments.size() > 0)
			return segments.get(segments.size()-1);
		else
			return null;
	}

	public Collection<Feature> getSelectedFeatures() {
		return features;
	}

	/**
	 * Receive application events that might cause selections to update
	 */
	public void receiveEvent(Event event) {
		if ("bookmark.edit".equals(event.getAction())) {
			log.info("received event: " + event + ", " + FeatureUtils.toString((Feature)event.getData()));
			replaceSelection((Feature)event.getData(), false);
		}
	}

	// ---- event support -----------------------------------------------------

	public void addEventListener(EventListener listener) {
		eventSupport.addEventListener(listener);
	}

	public void removeEventListener(EventListener listener) {
		eventSupport.removeEventListener(listener);
	}

	/**
	 * Signal that selections have changed. Some listeners may care whether event were generated by
	 * direct user input or programmatically. The action "selections.changed.primary" indicates
	 * that a selections changed event was caused by direct user input (@see GenomeViewPanel). 
	 */
	private void fireSelectionsChangedEvent(String msg) {
		if (!suppressEvents) {
			if (primary)
				eventSupport.fireEvent(this, "selections.changed.primary", msg, true);
			else
				eventSupport.fireEvent(this, "selections.changed", msg, true);
		}
	}
}
