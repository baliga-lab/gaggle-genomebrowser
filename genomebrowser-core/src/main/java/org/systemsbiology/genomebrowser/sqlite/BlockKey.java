package org.systemsbiology.genomebrowser.sqlite;

import java.util.UUID;

import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;


/**
 * Describes a block sufficiently to load the block's data from the DB.
 */
public class BlockKey {
	private final UUID trackUuid;
	private final int sequencesId;
	private final String seqId;
	private final Strand strand;
	private final int start;
	private final int end;
	private final int length;
	private final String table;
	private final int firstRowId; // TODO change row IDs to long
	private final int lastRowId;

	/*
	Note that, for the human genome the largest chromosomes are ~250 million bps, which
	fits in a java int, which ranges from -2,147,483,648 to 2,147,483,647.
	
	BUT the rowID's could easily exceed the size of an integer, for example single base
	resolution data on the human genome would need 3 billion bps, or twice that if we
	have strand specific data.
	*/

	public BlockKey(UUID trackUuid, int sequencesId, String seqId, Strand strand, int start, int end, int length, String table, int firstRowId, int lastRowId) {
		this.trackUuid = trackUuid;
		this.sequencesId = sequencesId;
		this.seqId = seqId;
		this.strand = strand;
		this.start = start;
		this.end = end;
		this.length = length;
		this.table = table;
		this.firstRowId = firstRowId;
		this.lastRowId = lastRowId;
	}

	
	public UUID getTrackUuid() {
		return trackUuid;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public int getLength() {
		return length;
	}

	public int getSequencesId() {
		return sequencesId;
	}

	public String getSeqId() {
		return seqId;
	}

	public Strand getStrand() {
		return strand;
	}

	public String getTable() {
		return table;
	}

	public int getFirstRowId() {
		return firstRowId;
	}

	public int getLastRowId() {
		return lastRowId;
	}

	// careful, we're assuming contiguous row ids
	public int getFeatureCount() {
		return lastRowId - firstRowId + 1;
	}

	@Override
	public int hashCode() {
		return trackUuid.hashCode() * 151 + firstRowId * 31 + lastRowId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlockKey other = (BlockKey) obj;
		
		// a block is uniquely defined by it's track uuid and first
		// and last rowId in the track's features table.

		if (trackUuid != other.trackUuid)
			return false;
		if (firstRowId != other.firstRowId)
			return false;
		if (lastRowId != other.lastRowId)
			return false;
		return true;
	}
	
	public boolean overlaps(Sequence sequence, Strand strand, int coord) {
		if (!this.seqId.equals(sequence.getSeqId())) return false;
		if (this.strand != Strand.any && strand != Strand.any && this.strand != strand) return false;
		return (this.start <= coord && this.end >= coord);
	}

	@Override
	public String toString() {
		return String.format("(BlockKey uuid=%s, seq=(%d)%s, strand=%s, start=%d, end=%d, len=%d, table=%s, rows=%d:%d)",
				trackUuid.toString(), sequencesId, seqId, strand.toAbbreviatedString(), start, end, length, table, firstRowId, lastRowId);
	}
}


