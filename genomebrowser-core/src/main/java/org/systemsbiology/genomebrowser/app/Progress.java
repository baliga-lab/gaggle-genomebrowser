package org.systemsbiology.genomebrowser.app;

import org.systemsbiology.util.Pair;

public class Progress {
	private int progress;
	private int expected;

	public Progress() {}

	public Progress(int expected) {
		this.expected = expected;
	}

	public synchronized void init(int expected) {
		this.expected = expected;
		this.progress = 0;
	}

	public synchronized int getProgress() {
		return progress;
	}
	public synchronized void setProgress(int progress) {
		this.progress = progress;
	}
	public synchronized void increment() {
		this.progress++;
	}
	public synchronized void add(int amount) {
		this.progress += amount;
	}
	public synchronized int getExpected() {
		return expected;
	}
	public synchronized void setExpected(int expected) {
		this.expected = expected;
	}

	public synchronized Pair<Integer, Integer> getProgressAndExpected() {
		return new Pair<Integer, Integer>(progress, expected);
	}
}
