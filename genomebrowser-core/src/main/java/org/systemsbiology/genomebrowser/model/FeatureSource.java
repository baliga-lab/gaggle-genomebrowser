package org.systemsbiology.genomebrowser.model;

import org.systemsbiology.util.ProgressListener;

/**
 * The purpose of this interface is to connect a source of features to
 * a "sink" for features -- the FeatureProcessor. It is used, for example,
 * to load features from various file formats and store them in a datastore,
 * whose implementation is supposed to be flexible. The source can stream
 * feature information to limit memory usage.
 * 
 * The pattern used here is a double-dispatch something like the Visitor
 * pattern in order to process features in a manner dependent on both source
 * and consumer.
 * 
 * I considered a producer-consumer implementation using a thread-safe queue,
 * but the concurrency complicates things without much benefit. The real issue
 * here is to allow variation of both the producer and consumer and not to
 * manage the workload in any special way.
 * 
 * A FeatureSource is like an iterator except the FeatureSource pushes features
 * to the FeatureProcessor rather than having the consumer pull features from
 * the iterator. This makes it easier for the FeatureSource to ensure that
 * proper cleanup happens, such as closing files.
 *
 * It's still possible that there's one too many levels of indirection going on
 * here or maybe an Iterator would have worked just as well. 
 */
public interface FeatureSource {
    void processFeatures(FeatureProcessor featureProcessor) throws Exception;

    // TODO remove progress methods from FeatureSource
    void addProgressListener(ProgressListener progressListener);
    void removeProgressListener(ProgressListener progressListener);
}
