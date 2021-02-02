package havis.device.rf.common;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import havis.device.rf.RFConsumer;

public class KeepAliveThread implements Runnable {

	private final static Logger log = Logger.getLogger(KeepAliveThread.class.getName());

	private RFConsumer consumer;
	private int interval;
	private boolean running;
	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	protected KeepAliveThread(RFConsumer consumer, int interval) {
		this.consumer = consumer;
		this.interval = interval;
	}

	@Override
	public void run() {
		this.running = true;
		while (running) {
			long now = new Date().getTime();
			long sleepUntil = now + interval;
			try {
				if (consumer == null) {
					log.log(Level.WARNING, "Failed to send keep-alive to consumer. Consumer was null. Stopping keep-alive thread.");
					this.running = false;
				} else {
					log.log(Level.FINER, "Sending keep-alive to consumer: {0}", consumer);
					consumer.keepAlive();
				}
			} catch (Exception e) {
				LogRecord logRec = new LogRecord(Level.SEVERE, "Failed to send keep alive: {0}");
				logRec.setThrown(e);
				logRec.setParameters(new Object[] { e });		
				logRec.setLoggerName(log.getName());
				log.log(logRec);				
			}

			try {
				lock.lock();
				while (running & sleepUntil > now) {
					condition.await(sleepUntil - now, TimeUnit.MILLISECONDS);
					now = new Date().getTime();
				}
			} catch (InterruptedException e) {
				this.running = false;
			} finally {
				lock.unlock();
			}
		}
	}

	public void stop() {
		log.finer("Stopping keep-alive thread.");

		lock.lock();
		try {
			this.running = false;
			condition.signal();
		} finally {
			lock.unlock();
		}
	}
}
