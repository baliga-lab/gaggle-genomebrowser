package org.systemsbiology.genomebrowser.model;

import java.util.UUID;

import org.systemsbiology.util.Attributes;


/**
 * A chromosome, replicon, plasmid, or other sequence.
 * 
 * @author cbare
 */
public interface Sequence {
	public UUID getUuid();
	public String getSeqId(); // TODO change to getName
	public int getLength();
	public Topology getTopology();

	public Attributes getAttributes();


	Sequence NULL_SEQUENCE = new Sequence() {
		private Attributes attr = Attributes.EMPTY;

		public UUID getUuid() {
			return null;
		}

		public Attributes getAttributes() {
			return attr;
		}

		public int getLength() {
			return 0;
		}

		public String getSeqId() {
			return "";
		}

		public Topology getTopology() {
			return Topology.unknown;
		}
	};
}
