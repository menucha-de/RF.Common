package havis.device.rf.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import havis.device.rf.RFConsumer;
import havis.device.rf.configuration.GPIState;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.TagOperation;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mockit.Deencapsulation;

import org.junit.Before;
import org.junit.Test;

public class KeepAliveThreadTest {

	@Before
	public void setup() throws Exception {
		
	}

	class KeepAliveReceiver implements RFConsumer {

		private int keepAlivesReceived = 0;

		@Override
		public void connectionAttempted() {
		}

		@Override
		public List<TagOperation> getOperations(TagData arg0) {
			return null;
		}

		public void gpiChanged(GPIState arg0) {

		}

		@Override
		public void keepAlive() {
			keepAlivesReceived++;
		}

		public int getKeepAlivesReceived() {
			return keepAlivesReceived;

		}
	}

	@Test
	public void testKeepAliveThread() throws InterruptedException {
		KeepAliveReceiver consumer = new KeepAliveReceiver();
		int interval = 200;

		KeepAliveThread kat = new KeepAliveThread(consumer, interval);
		assertEquals(Deencapsulation.getField(kat, "consumer"), consumer);
		assertEquals(Deencapsulation.<Integer>getField(kat, "interval").intValue(), interval);
		TimeUnit.MILLISECONDS.sleep(100);
		assertTrue(consumer.getKeepAlivesReceived() == 0);
	}

	@Test
	public void testRun() throws InterruptedException {
		/* Test 1 (positive): non-null consumer and valid interval */
		KeepAliveReceiver consumer = new KeepAliveReceiver();
		KeepAliveThread kat = new KeepAliveThread(consumer, 10);
		ExecutorService xs = Executors.newCachedThreadPool();
		xs.submit(kat);
		TimeUnit.MILLISECONDS.sleep(100);
		assert ((boolean) Deencapsulation.getField(kat, "running"));
		Deencapsulation.setField(kat, "running", false);
		xs.awaitTermination(100, TimeUnit.MILLISECONDS);
		assertTrue(consumer.getKeepAlivesReceived() > 0);

		/* Test 2 (negative): remove consumer during wait */
		kat = new KeepAliveThread(consumer, 100);
		xs = Executors.newCachedThreadPool();
		xs.submit(kat);
		TimeUnit.MILLISECONDS.sleep(10);
		assertTrue((boolean) Deencapsulation.getField(kat, "running"));
		Deencapsulation.setField(kat, "consumer", null);
		TimeUnit.MILLISECONDS.sleep(120);
		assertTrue(!(boolean) Deencapsulation.getField(kat, "running"));
		xs.shutdown();
		xs.awaitTermination(100, TimeUnit.MILLISECONDS);

		/* Test 3 (negative): force interruption during wait */
		kat = new KeepAliveThread(consumer, 10000);
		xs = Executors.newCachedThreadPool();
		xs.submit(kat);
		TimeUnit.MILLISECONDS.sleep(100);
		assertTrue((boolean) Deencapsulation.getField(kat, "running"));
		xs.shutdownNow();
		xs.awaitTermination(100, TimeUnit.MILLISECONDS);
		assertTrue(!(boolean) Deencapsulation.getField(kat, "running"));
	}

	@Test
	public void testStop() throws InterruptedException {

		KeepAliveReceiver consumer = new KeepAliveReceiver();
		KeepAliveThread kat = new KeepAliveThread(consumer, 10000);
		ExecutorService xs = Executors.newCachedThreadPool();
		xs.submit(kat);
		TimeUnit.MILLISECONDS.sleep(100);
		assertTrue((boolean) Deencapsulation.getField(kat, "running"));
		kat.stop();
		TimeUnit.MILLISECONDS.sleep(100);
		assertTrue(!(boolean) Deencapsulation.getField(kat, "running"));
		xs.shutdown();
		xs.awaitTermination(100, TimeUnit.MILLISECONDS);

	}

}
