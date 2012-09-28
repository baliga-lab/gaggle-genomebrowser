package org.systemsbiology.genomebrowser.sqlite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.Iteratable;
import org.systemsbiology.util.MultiHashMap;
import org.systemsbiology.util.Pair;


public class BlockIndex {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(BlockIndex.class);

	// map (SeqId, Strand) -> BlockKey 
	private MultiHashMap<Pair<String, Strand>, BlockKey> keyMap = new MultiHashMap<Pair<String, Strand>, BlockKey>(); 


	public BlockIndex() {}

	public void add(BlockKey key) {
		//log.info(key);
		keyMap.add(new Pair<String, Strand>(key.getSeqId(), key.getStrand()), key);
	}

	public void addAll(List<BlockKey> keys) {
		for (BlockKey key: keys) {
			add(key);
		}
	}

	public int size() {
		int size = 0;
		for (List<BlockKey> list: keyMap.values()) {
			size += list.size();
		}
		return size;
	}

	public Iteratable<BlockKey> keys() {
		return new Iteratable.Wrapper<BlockKey>(keyMap.getAllValues().iterator());
	}

	public Iteratable<BlockKey> keys(String seqId, Strand strand) {
		List<BlockKey> keys = new ArrayList<BlockKey>();
		if (strand == Strand.any) {
			keys.addAll(keyMap.getList(new Pair<String, Strand>(seqId, Strand.forward)));
			keys.addAll(keyMap.getList(new Pair<String, Strand>(seqId, Strand.reverse)));
			keys.addAll(keyMap.getList(new Pair<String, Strand>(seqId, Strand.none)));
		}
		else {
			keys.addAll(keyMap.getList(new Pair<String, Strand>(seqId, strand)));
		}
		return new Iteratable.Wrapper<BlockKey>(keys.iterator());
	}

	public Iteratable<BlockKey> keys(String seqId, Strand strand, int start, int end) {
		List<BlockKey> keys = new ArrayList<BlockKey>();
		if (strand == Strand.any) {
			keys.addAll(keyMap.getList(new Pair<String, Strand>(seqId, Strand.forward)));
			keys.addAll(keyMap.getList(new Pair<String, Strand>(seqId, Strand.reverse)));
			keys.addAll(keyMap.getList(new Pair<String, Strand>(seqId, Strand.none)));
		}
		else {
			keys.addAll(keyMap.getList(new Pair<String, Strand>(seqId, strand)));
		}
		return new BlockKeyIteratable(keys, start, end);
	}

	private class BlockKeyIteratable implements Iteratable<BlockKey> {
		int start;
		int end;
		int i;
		BlockKey next;
		List<BlockKey> keys;

		BlockKeyIteratable(List<BlockKey> keys, int start, int end) {
			this.keys = keys;
			this.start = start;
			this.end = end;
			i = 0;
			if (keys == null || keys.size() < 1)
				next = null;
			else {
				next = keys.get(i);
				while (next.getEnd()<start || next.getStart()>end) {
					i++;
					if (i>=keys.size()) {
						next = null;
						break;
					}
					next = keys.get(i);
				}
			}
		}

		public Iterator<BlockKey> iterator() {
			return this;
		}

		public boolean hasNext() {
			return next != null;
		}

		public BlockKey next() {
			BlockKey current = next;
			i++;
			if (i>=keys.size())
				next = null;
			else {
				next = keys.get(i);
				while (next.getEnd()<start || next.getStart()>end) {
					i++;
					if (i>=keys.size()) {
						next = null;
						break;
					}
					next = keys.get(i);
				}
			}
			return current;
		}

		public void remove() {
			throw new UnsupportedOperationException("BlockKeyIteratable doesn't support remove().");
		}
	}
}
