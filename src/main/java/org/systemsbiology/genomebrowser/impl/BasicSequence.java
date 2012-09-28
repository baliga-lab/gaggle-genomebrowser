package org.systemsbiology.genomebrowser.impl;

import java.util.UUID;

import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.util.Attributes;


public class BasicSequence implements Sequence {
	private UUID uuid;
	private Attributes attr = new Attributes();
	private String seqId;
	private int length;
	private Topology topology;


	public BasicSequence(UUID uuid, String seqId, int length, Topology topology) {
		this.uuid = uuid;
		this.seqId = seqId;
		this.length = length;
		this.topology = topology;
	}

	public UUID getUuid() {
		return uuid;
	}

	public Attributes getAttributes() {
		return attr;
	}

	public int getLength() {
		return length;
	}

	public String getSeqId() {
		return seqId;
	}

	public Topology getTopology() {
		return topology;
	}

	@Override
	public int hashCode() {
		return 31 + ((uuid == null) ? 0 : uuid.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasicSequence other = (BasicSequence) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		}
		else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("(%s, %,d, %s)", seqId, length, topology.toString());
	}
}
