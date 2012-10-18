package org.systemsbiology.genomebrowser.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.genomebrowser.util.Attributes$;

/**
 * A dataset is a set of sequences and a set of tracks holding features
 * on those sequences.
 */
public interface Dataset {
    UUID getUuid();
    String getName();
    List<Sequence> getSequences();
    Sequence getSequence(String seqId);
    List<Track<Feature>> getTracks();
    Track<Feature> getTrack(String name);
    void addTrack(Track<? extends Feature> track);
    Attributes getAttributes();
    /**
     * @return an implementation of SequenceFetcher or null if there is none
     */
    SequenceFetcher getSequenceFetcher();

    Dataset EMPTY_DATASET = new Dataset() {
            private Attributes attr = Attributes$.MODULE$.EMPTY();

            public UUID getUuid() { return null; }
            public Attributes getAttributes() { return attr; }
            public String getName() { return ""; }
            public List<Sequence> getSequences() { return Collections.emptyList(); }
            public Sequence getSequence(String seqId) {
                throw new RuntimeException("Dataset does not contain a sequence named \"" + seqId + "\".");
            }
            public List<Track<Feature>> getTracks() { return Collections.emptyList(); }
            public Track<Feature> getTrack(String name) { return null; }
            public void addTrack(Track<? extends Feature> track) {
                throw new RuntimeException("Can't add a track to the empty dataset");
            }
            public SequenceFetcher getSequenceFetcher() { return null; }
        };
}
