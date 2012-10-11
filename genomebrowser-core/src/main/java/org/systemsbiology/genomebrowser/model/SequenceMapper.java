package org.systemsbiology.genomebrowser.model;

public interface SequenceMapper<T> {
    T map(String name);
}
