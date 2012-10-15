package org.systemsbiology.genomebrowser.app;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.systemsbiology.genomebrowser.event.EventSupport;
import org.systemsbiology.genomebrowser.event.Event;

import org.apache.log4j.Logger;

public class ApplicationEventQueue implements Runnable {
	private static final Logger log = Logger.getLogger(ApplicationEventQueue.class);
	
	BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
	EventSupport eventSupport;
	volatile boolean done;
	Thread thread;
	ExecutorService executor = Executors.newCachedThreadPool();
	
	public ApplicationEventQueue(EventSupport eventSupport) {
		this.eventSupport = eventSupport;
	}

	public void enqueue(Event event) throws InterruptedException {
		queue.put(event);
	}

	public void enqueue(Runnable task) throws InterruptedException {
		queue.put(task);
	}

	public void start() {
		thread = new Thread(this);
		thread.start();
	}

	public void shutdown() {
		done=true;
		thread.interrupt();
	}

	public void run() {
		while(!done) {
			try {
				Object obj = queue.take();
				if (obj instanceof Runnable) {
					executor.execute((Runnable)obj);
				}
				else if (obj instanceof Event) {
					eventSupport.fireEvent((Event)obj);
				}
			}
			catch (InterruptedException e) {
				done = true;
				// TODO what about items left on the queue?
				log.info("shutting down queue.size()=" + queue.size());
			}
			catch (Exception e) {
				log.error(e);
				e.printStackTrace();
			}
			
			if (Thread.currentThread().isInterrupted()) done = true;
		}
	}
}
