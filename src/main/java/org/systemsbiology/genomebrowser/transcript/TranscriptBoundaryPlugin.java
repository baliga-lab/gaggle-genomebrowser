package org.systemsbiology.genomebrowser.transcript;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.app.ExternalAPI;
import org.systemsbiology.genomebrowser.app.Plugin;
import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.model.BasicPositionalFeature;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.genomebrowser.model.Track.Gene;
import org.systemsbiology.genomebrowser.model.Track.Quantitative;
import org.systemsbiology.genomebrowser.util.FeatureUtils;
import org.systemsbiology.genomebrowser.util.TrackUtils;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.StringUtils;


// TODO synchronization leaks due to bookmarkDataSource being shared, if not other reasons.
// TODO maintain sorted bookmarkDataSource


/**
 * A plugin that supports annotating transcript boundaries - transcript start and termination sites,
 * used for Sung-Ho's paper on the transcriptome of MMP. Helps the user call start and termination
 * sites by eye.
 */
@SuppressWarnings("serial")
public class TranscriptBoundaryPlugin implements Plugin {
	private static final Logger log = Logger.getLogger(TranscriptBoundaryPlugin.class);
	public static final String TRANSCRIPT_BOOKMARK_LIST = "transcripts";
	public static final String TRANSCRIPT_TYPE = "transcripts";
	private ExternalAPI api;

	private BookmarkDataSource bookmarkDataSource;
	private Bookmark oldBookmark;
	private Bookmark bookmark;

	private Track.Gene<GeneFeatureImpl> genes;
	private Track.Quantitative<Feature.Quantitative> breaksForward;
	private Track.Quantitative<Feature.Quantitative> breaksReverse;

	private Set<TranscriptBoundaryListener> listeners = new CopyOnWriteArraySet<TranscriptBoundaryListener>();
	private TranscriptBoundaryDialog dialog;

	/**
	 * We use a skip-list here, which I'm not 100% sure is the right choice, particularly 'cause
	 * the concurrency aspects aren't really used.
	 * The key is the coordinate on the sequence. Given a coordinate, you can easily find the
	 * entries with the next lower or high coordinate. Maybe a sorted list plus a binary search would
	 * do just as well?
	 * Note: ConcurrentSkipListMap requires Java 6
	 */
	private ConcurrentSkipListMap<Integer, Feature> snaps = new ConcurrentSkipListMap<Integer, Feature>();
	
	private int utrThreshold = 100;
	private UUID seqUuid;


	public synchronized void setExternalApi(ExternalAPI api) {
		this.api = api;
	}
	
	public synchronized void setBookmarkDataSource(BookmarkDataSource bookmarkDataSource) {
		this.bookmarkDataSource = bookmarkDataSource;
		this.bookmarkDataSource.getAttributes().put("type", TRANSCRIPT_TYPE);
	}

	public synchronized void setBookmark(Bookmark bookmark) {
		this.oldBookmark = bookmark;
		this.bookmark = bookmark;
	}

	@SuppressWarnings("unchecked")
	public synchronized void setGeneTrack(Track.Gene<? extends GeneFeatureImpl> genes) {
		this.genes = (Gene<GeneFeatureImpl>) genes;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void setBreaksForwardTrack(Track.Quantitative<? extends Feature.Quantitative> track) {
		breaksForward = (Quantitative<Feature.Quantitative>) track;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void setBreaksReverseTrack(Track.Quantitative<? extends Feature.Quantitative> track) {
		breaksReverse = (Quantitative<Feature.Quantitative>) track;
	}

	public int getUtrThreshold() {
		return utrThreshold;
	}

	public void setUtrThreshold(int utrThreshold) {
		this.utrThreshold = utrThreshold;
	}

	private void computeSnaps(Sequence sequence) {
		log.info(String.format("Computing snaps from tracks %s, %s, and %s.", nameOrNull(genes), nameOrNull(breaksForward), nameOrNull(breaksReverse)));
		snaps.clear();
		FeatureFilter filter = new FeatureFilter(sequence);
		if (genes != null) {
			for (GeneFeatureImpl gene : genes.features(filter)) {
				snaps.put(gene.getStart(), gene);
				snaps.put(gene.getEnd(), gene);
			}
		}
		else
			log.warn("no genes track found");

		if (breaksForward != null) {
			for (Feature.Quantitative b: breaksForward.features(filter)) {
				// don't forget that these features are returned as flyweights
				snaps.put(b.getCentralPosition(), new BasicPositionalFeature(b));
			}
		}
		else
			log.warn("no breaks forward track found");

		if (breaksReverse != null) {
			for (Feature.Quantitative b: breaksReverse.features(filter)) {
				// don't forget that these features are returned as flyweights
				snaps.put(b.getCentralPosition(), new BasicPositionalFeature(b));
			}
		}
		else
			log.warn("no breaks reverse track found");
	}

	private String nameOrNull(Track<? extends Feature> track) {
		if (track == null)
			return "null";
		return track.getName();
	}

	@SuppressWarnings("unchecked")
	public synchronized void computeSnaps(Dataset dataset, Sequence sequence) {
		setGeneTrack((Track.Gene<? extends GeneFeatureImpl>) TrackUtils.findTrack(dataset, "Genes", "genes", "Genome"));
		setBreaksForwardTrack((Track.Quantitative<? extends Feature.Quantitative>) TrackUtils.findTrack(dataset, "breaks.forward"));
		setBreaksReverseTrack((Track.Quantitative<? extends Feature.Quantitative>) TrackUtils.findTrack(dataset, "breaks.reverse"));
		computeSnaps(sequence);
		seqUuid = sequence.getUuid();
	}

	public synchronized void init() {
		log.info("Initializing Transcript boundary plugin");
		BookmarkTranscriptBoundaryAction bookmarkAction = new BookmarkTranscriptBoundaryAction();
		SettingsAction settingsAction = new SettingsAction();
		ViewBookmarks viewBookmarksAction = new ViewBookmarks();
		api.addEventListener(this);
		api.addMenu("Tools|Transcript Annotation",  new Action[] {
				settingsAction,
				bookmarkAction,
				viewBookmarksAction});
	}

	public synchronized void receiveEvent(Event event) {
		// need to receive events when selection changes due to user mouse action
		// and differentiate those from selection changes caused from here.
		// TranscriptBoundaryPlugin: (Event src=org.systemsbiology.genomebrowser.app.Selections@4a009ab0, action=selections.changed, object=add segment ["chr", 15293, 16459])
		if ("selections.changed.primary".equals(event.getAction())) {
			log.info(event);
			if (bookmark != null) {
				Segment segment = api.getSelectedSegment();
				if (segment==null)
					return;
				if (segment.seqId.equals(bookmark.getSeqId()) && segment.start == bookmark.getStart() && segment.end == bookmark.getEnd()) {
					log.info("dropping selections event");
					return;
				}
				bookmark.setSeqId(segment.seqId);
				bookmark.setStart(segment.start);
				bookmark.setEnd(segment.end);
				bookmark.setStrand(api.getSelectionStrandHint());
				bookmark.setLabel(constructBookmarkName(api.getSelectedFeatures(), bookmark.getStrand(), segment.start, segment.end));
				fireBookmarkUpdateEvent();
			}
		}
	}

	private String constructBookmarkName(Collection<Feature> features, Strand strand, int start, int end) {
		Set<Feature> found = new HashSet<Feature>();
		StringBuilder sb = new StringBuilder();
		if (features.size() == 0) {
			features = findFeatures(strand, start, end);
		}
		if (features.size() > 0) {
			boolean rest = false;
			for (Feature feature : features) {
				if (found.add(feature)) {
					if (rest) sb.append("-");
					if (feature instanceof NamedFeature) {
						sb.append(((NamedFeature)feature).getName());
					}
					else {
						sb.append(feature.getLabel());
					}
					rest = true;
				}
			}
		}
		else {
			sb.append(start).append("-").append(end);
		}
		return sb.toString();
	}

	private Collection<Feature> findFeatures(Strand strand, int start, int end) {
		List<Feature> features = new ArrayList<Feature>();
		Entry<Integer, Feature> entry = snaps.ceilingEntry(start);
		while (entry != null && entry.getKey()<end) {
			entry = snaps.higherEntry(entry.getKey());
			if (entry.getValue() instanceof GeneFeatureImpl) {
				GeneFeatureImpl feature = (GeneFeatureImpl) entry.getValue();
				if (feature.getStrand()==strand && feature.getStart() <= end && feature.getEnd() >= end) {
					features.add(feature);
				}
			}
		}
		return features;
	}

	/**
	 * Entry point if used to delimit a single transcript
	 */
	public synchronized void bookmarkTranscriptBoundary() {
		if (api.getDataset()==null || api.getDataset()==Dataset.EMPTY_DATASET)
			return;
		Sequence sequence = api.getDataset().getSequence(api.getVisibleSegment().seqId);
		computeSnaps(api.getDataset(), sequence);
		// get current selection and create a bookmark
		Segment segment = api.getSelectedSegment();
		if (segment==null)
			segment = api.getVisibleSegment();
		if (segment==null) {
			log.warn("No visible segment??");
			return;
		}
		oldBookmark = null;
		bookmark = new Bookmark(
					segment.seqId,
					api.getSelectionStrandHint(),
					segment.start,
					segment.end);
		String[] names = FeatureUtils.extractNames(api.getSelectedFeatures());
		bookmark.setAssociatedFeatureNames(names);
		bookmark.setLabel(namesToLabel(names));

		if (bookmarkDataSource==null) {
			// if currently selected bookmark set (in BookmarksPanel) is of TRANSCRIPT_TYPE use it,
			// otherwise create a new one.
			bookmarkDataSource = api.getSelectedBookmarkDataSource();
			if (bookmarkDataSource == null || !TRANSCRIPT_TYPE.equals(bookmarkDataSource.getAttributes().getString("type"))) {
				bookmarkDataSource = api.getOrCreateBookmarkDataSource(TRANSCRIPT_BOOKMARK_LIST);
				bookmarkDataSource.getAttributes().put("type", TRANSCRIPT_TYPE);
			}
		}

		showTranscriptBoundaryDialog();
	}

	public synchronized void edit(BookmarkDataSource bookmarkDataSource, Bookmark bookmark) {
		if (bookmarkDataSource==null) {
			if (this.bookmarkDataSource == null) {
				this.bookmarkDataSource = api.getSelectedBookmarkDataSource();
				if (this.bookmarkDataSource == null || !TRANSCRIPT_TYPE.equals(this.bookmarkDataSource.getAttributes().getString("type"))) {
					this.bookmarkDataSource = api.getOrCreateBookmarkDataSource(TRANSCRIPT_BOOKMARK_LIST);
					this.bookmarkDataSource.getAttributes().put("type", TRANSCRIPT_TYPE);
				}
			}
		}
		else {
			this.bookmarkDataSource = bookmarkDataSource;
		}

		// check if we need to recompute snaps
		Sequence sequence = api.getDataset().getSequence(api.getVisibleSegment().seqId);
		if (sequence.getUuid() != seqUuid) {
			computeSnaps(api.getDataset(), sequence);
		}

		oldBookmark = bookmark;
		this.bookmark = new Bookmark(bookmark);

		showTranscriptBoundaryDialog();
	}

	/**
	 * Make a bookmark name from a list of feature names
	 * @param names
	 * @return null if names is null or empty
	 */
	public static String namesToLabel(String[] names) {
		if (names==null || names.length==0)
			return null;
		return StringUtils.join("-", names);
	}

	/**
	 * given a bookmark, which we assume is a gene (protein coding region)
	 * expand in the 5' direction to the next break-point if it's within
	 * a reasonable distance (utrThreshold) from the coding sequence start.
	 */
	private synchronized void guessTranscriptBoundaries() {
		log.info("+ guessing transcript boundries");
		// find the transcription break point before the start of the gene
		if (bookmark.getStrand()==Strand.forward) {
			Entry<Integer, Feature> entry = snaps.lowerEntry(bookmark.getStart());
			while (entry != null && !bookmark.getStrand().encompasses(entry.getValue().getStrand())) {
				entry = snaps.lowerEntry(entry.getKey());
			}
			if (entry != null && !(entry.getValue() instanceof GeneFeatureImpl)  && (bookmark.getStart() - entry.getKey() < utrThreshold)) {
				bookmark.setStart(entry.getKey());
			}
		}
		else if (bookmark.getStrand()==Strand.reverse) {
			Entry<Integer, Feature> entry = snaps.higherEntry(bookmark.getEnd());
			while (entry != null && !bookmark.getStrand().encompasses(entry.getValue().getStrand())) {
				entry = snaps.higherEntry(entry.getKey());
			}
			if (entry != null && !(entry.getValue() instanceof GeneFeatureImpl) && (entry.getKey() - bookmark.getEnd() < utrThreshold)) {
				bookmark.setEnd(entry.getKey());
			}
		}
		log.info("- guessing transcript boundries!");
	}

	/**
	 * Attempt to guess where the next transcript is from the current transcript
	 */
	private void guessNextTranscript() {
		log.info("+ guess next transcript");
		int key;
		if (bookmark==null)
			key = 0;
		else {
			key = bookmark.getEnd();
		}
		Entry<Integer, Feature> entry = snaps.higherEntry(key);
		while (entry != null && (!(entry.getValue() instanceof GeneFeatureImpl) || entry.getValue().getStart() < key))  {
			entry = snaps.higherEntry(entry.getKey());
		}
		if (entry==null) return;
		Feature feature = entry.getValue();
		bookmark = new Bookmark(feature.getSeqId(), feature.getStrand(), feature.getStart(), feature.getEnd());
		if (feature instanceof NamedFeature) {
			bookmark.setAssociatedFeatureNames(((NamedFeature)feature).getName());
			bookmark.setLabel(((NamedFeature)feature).getName());
		}
		guessTranscriptBoundaries();
		log.info("- guess next transcript");
	}

	private Bookmark findNextExistingBookmark() {
		for (Bookmark b : bookmarkDataSource) {
			if (b.getStart() < bookmark.getStart())
				continue;
			if (b.getStart()==bookmark.getStart()) {
				if (b.getEnd() > bookmark.getEnd())
					return b;
				continue;
			}
			// b.getStart() > bookmark.getStart()
			return b;
		}
		return null;
	}

	public synchronized void gotoNextTranscript() {
		log.info("next transcript");
		Bookmark bk = findNextExistingBookmark();
		if (bk==null) {
			guessNextTranscript();
			oldBookmark = null;
		}
		else {
			bookmark = bk;
			oldBookmark = bookmark;
		}
		fireBookmarkUpdateEvent();
		fireApplicationEvent();
		fireGotoApplicationEvent();
	}


	public synchronized void showTranscriptBoundaryDialog() {
		if (bookmark==null) {
			bookmark = new Bookmark();
			oldBookmark = null;
		}
		if (dialog==null) {
			dialog = new TranscriptBoundaryDialog(api.getMainWindow(), this);
			addTranscriptBoundaryListener(dialog);
			dialog.setVisible(true);
		}
		else {
			fireBookmarkUpdateEvent();
		}
	}

	public synchronized void snapStartLeft() {
		log.info("snap start left: from " + bookmark.getStart());
		Entry<Integer, Feature> entry = snaps.lowerEntry(bookmark.getStart());
		while (entry != null) {
			log.info("entry = " + entry);
			if (bookmark.getStrand().encompasses(entry.getValue().getStrand()))
				break;
			entry = snaps.lowerEntry(entry.getKey());
		}
		if (entry != null) {
			bookmark.setStart(entry.getKey());
			fireBookmarkUpdateEvent();
			fireApplicationEvent();
		}
	}

	public synchronized void snapStartRight() {
		log.info("snap start right: from " + bookmark.getStart());
		Entry<Integer, Feature> entry = snaps.higherEntry(bookmark.getStart());
		while (entry != null) {
			if (bookmark.getStrand().encompasses(entry.getValue().getStrand()))
				break;
			entry = snaps.higherEntry(entry.getKey());
		}
		if (entry != null) {
			bookmark.setStart(entry.getKey());
			fireBookmarkUpdateEvent();
			fireApplicationEvent();
		}
	}

	public synchronized void snapEndLeft() {
		log.info("snap end left: from " + bookmark.getEnd());
		Entry<Integer, Feature> entry = snaps.lowerEntry(bookmark.getEnd());
		while (entry != null) {
			if (bookmark.getStrand().encompasses(entry.getValue().getStrand()))
				break;
			entry = snaps.lowerEntry(entry.getKey());
		}
		if (entry != null) {
			bookmark.setEnd(entry.getKey());
			fireBookmarkUpdateEvent();
			fireApplicationEvent();
		}
	}

	public synchronized void snapEndRight() {
		log.info("snap end right: from " + bookmark.getEnd());
		Entry<Integer, Feature> entry = snaps.higherEntry(bookmark.getEnd());
		while (entry != null) {
			if (bookmark.getStrand().encompasses(entry.getValue().getStrand()))
				break;
			entry = snaps.higherEntry(entry.getKey());
		}
		if (entry != null) {
			bookmark.setEnd(entry.getKey());
			fireBookmarkUpdateEvent();
			fireApplicationEvent();
		}
	}

	public synchronized void addTranscriptBoundaryListener(TranscriptBoundaryListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeTranscriptBoundaryListener(TranscriptBoundaryListener listener) {
		listeners.remove(listener);
	}

	private void fireBookmarkUpdateEvent() {
		for (TranscriptBoundaryListener listener : listeners) {
			listener.bookmarkUpdateEvent(bookmark);
		}
	}

	private void fireApplicationEvent() {
		api.publishEvent(new Event(this, "bookmark.edit", getBookmark(), true));
	}

	private void fireGotoApplicationEvent() {
		api.publishEvent(new Event(this, "center on segment", Segment.fromFeature(bookmark), true));
	}

	private synchronized void _update(String seq, Strand strand, int start, int end, String name, String annotation, String attributes) {
		bookmark.setSeqId(seq);
		bookmark.setStrand(strand);
		bookmark.setStart(start);
		bookmark.setEnd(end);
		bookmark.setLabel(name);
		bookmark.setAnnotation(annotation);
		bookmark.setAttributes(attributes);
	}
	
	synchronized void save() {
		if (oldBookmark==null) {
			bookmarkDataSource.add(bookmark);
		}
		else {
			bookmarkDataSource.update(oldBookmark, bookmark);
		}
		// after saving, we want to still be editing the same bookmark.
		oldBookmark = bookmark;
	}

	public synchronized void update(String seq, Strand strand, int start, int end, String name, String annotation, String attributes) {
		_update(seq, strand, start, end, name, annotation, attributes);
		// when the bookmark is updated, we want that to make the selections change in the viewing area
		fireApplicationEvent();
	}

	public synchronized void saveAndGotoNextTranscript(String seq, Strand strand, int start, int end, String name, String annotation, String attributes) {
		_update(seq, strand, start, end, name, annotation, attributes);
		save();
		gotoNextTranscript();
	}

	public synchronized Bookmark getBookmark() {
		return new Bookmark(bookmark);
	}

	public synchronized void done(TranscriptBoundaryDialog dialog) {
		this.removeTranscriptBoundaryListener(dialog);
		this.dialog = null;
		this.bookmark = null;
		this.oldBookmark = null;
	}


	class BookmarkTranscriptBoundaryAction extends AbstractAction {
		public BookmarkTranscriptBoundaryAction() {
			super("Transcript Boundary");
			putValue(Action.SHORT_DESCRIPTION, "Create a bookmark denoting transcript boundaries.");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.ALT_MASK));
			putValue(Action.SMALL_ICON, FileUtils.getIconOrBlank("calipers_small.png"));
		}

		public void actionPerformed(ActionEvent e) {
			bookmarkTranscriptBoundary();
		}
	}
	
	class SettingsAction extends AbstractAction {
		public SettingsAction() {
			super("Settings");
			putValue(Action.SHORT_DESCRIPTION, "Set parameters for creating transcript boundary annotations.");
			putValue(Action.SMALL_ICON, FileUtils.getIconOrBlank("system.png"));
		}
		public void actionPerformed(ActionEvent e) {
			log.info("settings...");
		}
	}

	class ViewBookmarks extends AbstractAction {
		public ViewBookmarks() {
			super("View Transcript Bookmarks");
			putValue(Action.SHORT_DESCRIPTION, "View transcript boundary annotations in the bookmarks panel.");
			putValue(Action.SMALL_ICON, FileUtils.getIconOrBlank("bookmark.gif"));
		}
		public void actionPerformed(ActionEvent e) {
			api.publishEvent(new Event(TranscriptBoundaryPlugin.this, "open.bookmarks", TRANSCRIPT_BOOKMARK_LIST));
		}
	}

	interface TranscriptBoundaryListener {
		public void bookmarkUpdateEvent(Bookmark bookmark);
		public void nextTranscriptEvent(Bookmark bookmark);
	}
}
