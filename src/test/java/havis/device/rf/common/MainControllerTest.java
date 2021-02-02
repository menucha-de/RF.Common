package havis.device.rf.common;

import static mockit.Deencapsulation.getField;
import static mockit.Deencapsulation.setField;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import havis.device.rf.RFConsumer;
import havis.device.rf.capabilities.CapabilityType;
import havis.device.rf.common.test.TestHardwareManager;
import havis.device.rf.common.util.RFUtils;
import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.Configuration;
import havis.device.rf.configuration.ConfigurationType;
import havis.device.rf.configuration.GPIState;
import havis.device.rf.configuration.KeepAliveConfiguration;
import havis.device.rf.configuration.SelectionMask;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.LockOperation;
import havis.device.rf.tag.operation.ReadOperation;
import havis.device.rf.tag.operation.TagOperation;
import havis.device.rf.tag.operation.WriteOperation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

public class MainControllerTest {

	private static final Logger log = Logger.getLogger(ConfigurationManager.class.getName());
	
	@Mocked
	Environment environment;

	@Before
	public void setUp() throws Exception {
		log.setLevel(Level.ALL);					
	}

	@Test
	public void testGetInstance(@Mocked final ConfigurationManager configManager, @Mocked final TestHardwareManager hwManager) throws ImplementationException, ParameterException, ConnectionException {		
		
		/*
		 * Test: 
		 * 	- static getInstance method of MainController class returning a singleton
		 * 
		 * Expected: 
		 * 	- one and only one instance is returned
		 *  - class of hardware manager instance is the one of TestHardwareManager 
		 * 
		 */
		setField(MainController.class, "instance", null);
		final MainController mc1 = MainController.getInstance();
		assertTrue(getField(MainController.class, "instance") != null);
		
		new Verifications() {{
			new ConfigurationManager(withSameInstance(mc1));
			times = 1;
			
			configManager.loadConfiguration();
			times = 1;
			
			hwManager.openConnection();
			times = 1;
		}};
		
		final MainController mc2 = MainController.getInstance();
		assertTrue(mc1 == mc2);
		assertEquals(getField(mc1, "hwManager").getClass(), TestHardwareManager.class);
		
		/*
		 * Test:
		 * 	- hwManager.openConnection throws exception
		 * Expected:
		 * 	- ImplementationException is being thrown
		 */
		
		setField(MainController.class, "instance", null);
		new NonStrictExpectations() {			
			{			
				hwManager.openConnection();
				result = new Exception();
			}
		};
		
		try { 			
			MainController.getInstance();
			fail("Exception expected.");			
		} catch(ImplementationException e) { 
			
		} catch(Exception e) { 
			fail("Unexpected exception.");
		}
		
	}

	@Test
	public void testGetConfiguration(@Mocked final ConfigurationManager configManager) throws ImplementationException {
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();

		final ConfigurationType type = ConfigurationType.ANTENNA_CONFIGURATION;
		final short antennaID = 1;

		try {
			mc.getConfiguration(type, antennaID, (short) 0, (short) 0);
		} catch (ImplementationException ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				ConfigurationType captType = null;
				short captAntennaID = 0;
				configManager
						.getConfiguration(captType = withCapture(),
								captAntennaID = withCapture());
				times = 1;
				assertEquals(captType, type);
				assertEquals(captAntennaID, antennaID);
			}
		};

		new NonStrictExpectations() {
			{
				configManager.getConfiguration(
						withInstanceOf(ConfigurationType.class), anyShort);
				result = new ImplementationException();
			}
		};

		try {
			mc.getConfiguration(type, antennaID, (short) 0, (short) 0);
			fail("Exception expected but none thrown.");
		} catch (ImplementationException ex) {
		} catch (Exception ex) {
			fail("Exception no expected.");
		}
	}

	@Test
	public void testSetConfiguration(@Mocked final ConfigurationManager configManager) throws ImplementationException,
			ParameterException {
		
		/*
		 * Test: 
		 * 	- setConfiguration called with two config objects and no unsaved changes
		 * Expected:
		 * - setConfigObject on config manager instance called per config object
		 * - saveConfig is not called 
		 */
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();

		final List<Configuration> configs = new ArrayList<>();
		configs.add(new AntennaConfiguration());
		configs.add(new KeepAliveConfiguration());

		try {
			mc.setConfiguration(configs);
		} catch (ImplementationException ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				configManager.setConfiguration(withInstanceOf(Configuration.class));
				times = configs.size();

				configManager.saveConfig();
				times = 0;
			}
		};

		/*
		 * Test: 
		 * 	- setConfiguration called with two config objects and unsaved changes are true
		 * Expected:
		 * - setConfigObject on config manager instance called per config object
		 * - saveConfig is called 
		 */
		new NonStrictExpectations() {
			{
				configManager.hasUnsavedChanges();
				result = true;
			}
		};

		try {
			mc.setConfiguration(configs);
		} catch (ImplementationException ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				configManager
						.setConfiguration(withInstanceOf(Configuration.class));
				times = configs.size();

				configManager.saveConfig();
				times = 1;
			}
		};
		/*
		 * Test: 
		 * 	- setConfigObject of config manager throws parameter exception
		 * Expected:
		 * - parameter exception is thrown 
		 */
		new NonStrictExpectations() {
			{
				configManager.setConfiguration(withInstanceOf(Configuration.class));
				result = new ParameterException();
			}
		};

		try {
			mc.setConfiguration(configs);
			fail("Exception expected but none thrown.");
		} catch (ParameterException ex) {
		} catch (ImplementationException ex) {
			fail("Exception not expected.");
		}

	}

	@Test
	public void testResetConfiguration(@Mocked final ConfigurationManager configManager) throws ImplementationException, ParameterException {	
		/*
		 * Test: 
		 * 	- resetConfiguration without exception
		 * Expected:
		 * - resetConfig called on configManager once
		 * - loadConfig called on configManager twice (once by MainController.getInstance and once by resetConfiguration)
		 */
		
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();

		try {
			mc.resetConfiguration();
		} catch (ImplementationException ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{				
				configManager.resetConfig();
				times = 1;
				
				configManager.loadConfiguration();
				times = 2;
			}
		};

		/*
		 * Test: 
		 * 	- Parameter exception during loadConfiguration
		 * Expected:
		 * 	- exception thrown
		 */
		new NonStrictExpectations() {
			{
				configManager.loadConfiguration();
				result = new ParameterException();
			}
		};

		try {
			mc.resetConfiguration();
			fail("Exception expected but none thrown.");
		} catch (ParameterException ex) {
		} catch (ImplementationException ex) {
			fail("Exception not expected.");
		}
		
		/*
		 * Test: 
		 * 	- Parameter exception during resetConfig
		 * Expected:
		 * 	- exception thrown
		 */
		new NonStrictExpectations() {
			{
				configManager.resetConfig();
				result = new ParameterException();
			}
		};

		try {
			mc.resetConfiguration();
			fail("Exception expected but none thrown.");
		} catch (ParameterException ex) {
		} catch (ImplementationException ex) {
			fail("Exception not expected.");
		}
	}

	@Test
	public void testGetCapabilities(@Mocked final ConfigurationManager configManager) throws ImplementationException, ConnectionException {		
		/*
		 * Test: 
		 * 	- getCapabilities call
		 * Expected:
		 * 	- getCapabilitiesByType called on configManager once
		 */
		
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();

		final CapabilityType capType = CapabilityType.DEVICE_CAPABILITIES;
		mc.getCapabilities(capType);

		new Verifications() {
			{
				configManager.getCapabilitiesByType(withSameInstance(capType));
				times = 1;				
			}
		};
				
	}

	@Test
	public void testGetSupportedRegions(@Mocked final ConfigurationManager configManager) throws ImplementationException {
		/*
		 * Test: 
		 * 	- getSupportedRegions call
		 * Expected:
		 * 	- getSupportedRegions called on configManager once
		 */
		
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();
		mc.getSupportedRegions();

		new Verifications() {
			{
				configManager.getSupportedRegions();
				times = 1;
			}
		};
	}

	@Test
	public void testSetRegion(@Mocked final ConfigurationManager configManager) throws ImplementationException, ParameterException {		
		/*
		 * Test:
		 * 	- setRegion call with regionId "eu"
		 * 
		 * Expected:
		 * 	- setRegion on main controller is called once with given region ID 
		 */
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();
		final String regionId = "eu";

		try {
			mc.setRegion(regionId);
		} catch (ParameterException e) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				configManager.setRegion(withSameInstance(regionId));
				times = 1;				
			}
		};

		/*
		 * Test:
		 * 	- setRegion call with ParameterException
		 * 
		 * Expected:
		 * 	- exception is thrown 
		 */
		new NonStrictExpectations() {
			{
				configManager.setRegion(anyString);
				result = new ParameterException();
			}
		};

		try {
			mc.setRegion(regionId);
			fail("Exception expected but none thrown.");
		} catch (ParameterException e) {
		}
	}

	@Test
	public void testGetRegion(@Mocked final ConfigurationManager configManager) throws ImplementationException {		
		/*
		 * Test:
		 * 	- getRegion call
		 * 
		 * Expected:
		 * 	- getRegion on configManager is called once
		 */		
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();
		
		mc.getRegion();

		new Verifications() {
			{
				configManager.getRegion();
				times = 1;
			}
		};
	}

	class RFCTestConsumer implements RFConsumer, Callable<Object> {

		private String name;
		private MainController mainCtl;
		private int timeoutBeforeStart;
		private int connectionTimeout;
		private int timeoutAfterRound;

		private int initialRounds;
		private int roundsAfterDisconnectRequest;

		private boolean disconnectRequested;
		private boolean finalRoundsRunning;

		private boolean connectionSuccessful;

		public RFCTestConsumer(String name, MainController mainCtl,
				int timeoutBeforeStart, int connectionTimeout,
				int roundTimeout, int initialRounds,
				int roundsAfterDisconnectRequest) {
			super();
			this.name = name;
			this.mainCtl = mainCtl;

			this.timeoutBeforeStart = timeoutBeforeStart;
			this.timeoutAfterRound = roundTimeout;
			this.connectionTimeout = connectionTimeout;

			this.initialRounds = initialRounds;
			this.roundsAfterDisconnectRequest = roundsAfterDisconnectRequest;
		}

		public void connectionAttempted() {
			this.disconnectRequested = true;
		}

		private void println(String message, Object... args) {
			System.out.printf(
					String.format("RFCTestThread %s (%s): ", this.name, Thread
							.currentThread().getName())
							+ message + "\n", args);
		}

		public Object call() throws ConnectionException,
				ImplementationException, InterruptedException {
			TimeUnit.MILLISECONDS.sleep(timeoutBeforeStart);

			try {
				mainCtl.openConnection(this, connectionTimeout);
				connectionSuccessful = true;
			} catch (ConnectionException e) {
				e.printStackTrace();
				connectionSuccessful = false;
				throw e;
			}

			int i = 0;
			int n = initialRounds;

			for (; (n == -1 || i < n); i++) {
				println("working for round %d...", i);
				TimeUnit.MILLISECONDS.sleep(timeoutAfterRound);

				if (disconnectRequested && !finalRoundsRunning) {
					n = i + roundsAfterDisconnectRequest;
					finalRoundsRunning = true;
					println("working for last %d rounds...", i);
				}
			}

			this.mainCtl.closeConnection();
			println("Disconnected after %d rounds.", i);

			return null;
		}

		public List<TagOperation> getOperations(TagData arg0) {
			return null;
		}

		public void gpiChanged(GPIState arg0) {
		}

		public void keepAlive() {
		}

		public boolean isConnectionSuccessful() {
			return connectionSuccessful;
		}
	}

	@Test
	public void testOpenCloseConnection(@Mocked final TestHardwareManager hwMgr, @Mocked final ConfigurationManager configManager)
			throws ImplementationException, ConnectionException,
			InterruptedException {

		new NonStrictExpectations() {
			
			{
				configManager.getKeepAliveConfiguration();
				result = null;
			}
		};
		
		setField(MainController.class, "instance", null);
		final MainController mc1 = MainController.getInstance();
		ExecutorService xs = Executors.newCachedThreadPool();

		/*
		 * Test: 
		 * 	- Connection timeout of RFConsumer "worker2" too small
		 * Expected: 
		 * - ConnectionException thrown upon connection attempt of worker2
		 */
		final RFCTestConsumer worker1 = new RFCTestConsumer("Consumer 1", mc1, 0, 100, 10, 20, 10);
		final RFCTestConsumer worker2 = new RFCTestConsumer("Consumer 2", mc1, 50, 20, 10, 10, 10);

		Future<Object> res1 = xs.submit(worker1);
		Future<Object> res2 = xs.submit(worker2);

		xs.shutdown();
		xs.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

		try {
			res1.get();
		} catch (Exception e) {
			fail("Exception not expected.");
		}

		try {
			res2.get();
			fail("Exception expected but none thrown.");
		} catch (ExecutionException e) {
			if (!(e.getCause() instanceof ConnectionException))
				fail("Exception not expected.");
		}

		new Verifications() {
			{
				hwMgr.openConnection();
				times = 1;
				assertNull(getField(mc1, "consumer"));
				assertTrue(worker1.isConnectionSuccessful());
				assertTrue(!worker2.isConnectionSuccessful());
			}
		};

		/*
		 * Test: 
		 * 	- Connection timeout of RFConsumer "worker4" long enough
		 * Expected:
		 * 	- connection will be transferred to worker4
		 */
		setField(MainController.class, "instance", null);
		final MainController mc2 = MainController.getInstance();
		final RFCTestConsumer worker3 = new RFCTestConsumer("Consumer 3", mc2, 0, 100, 10, 20, 10);
		final RFCTestConsumer worker4 = new RFCTestConsumer("Consumer 4", mc2, 50, 200, 10, 10, 10);

		xs = Executors.newCachedThreadPool();
		res1 = xs.submit(worker3);
		res2 = xs.submit(worker4);

		xs.shutdown();
		xs.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

		try {
			res1.get();
		} catch (Exception e) {
			fail("Exception not expected.");
		}

		try {
			res2.get();
		} catch (Exception e) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				hwMgr.openConnection();
				times = 2;
				assertNull(getField(mc2, "consumer"));
				assertTrue(worker3.isConnectionSuccessful());
				assertTrue(worker4.isConnectionSuccessful());
			}
		};
	}

	@Test
	public void testExecute(@Mocked final TestHardwareManager hwMgr, @Mocked final ConfigurationManager configManager) throws ImplementationException,
			ParameterException {
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();		
		
		final RFConsumer consumer = new RFConsumer() {
			public void keepAlive() { }
			public List<TagOperation> getOperations(TagData arg0) { return null; }
			public void connectionAttempted() { }
		}; 
		
		setField(mc, "consumer", consumer);
		
		final List<SelectionMask> defaultFilters = Arrays.asList( 
			new SelectionMask() {{ 
				setBank(RFUtils.BANK_EPC); 
				setBitLength((short)64);
				setBitOffset((short)32);
				setMask(RFUtils.hexToBytes("8899aabbccddeeff"));
			}},
			new SelectionMask() {{
				setBank(RFUtils.BANK_TID);
				setBitLength((short)40);
				setBitOffset((short)0);
				setMask(RFUtils.hexToBytes("e2802000aabbccddeeff"));
			}}
		);
		
		final List<Short> antennas = Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4);
		final List<Filter> filters = Arrays.asList(new Filter(), new Filter());
		final List<TagOperation> operations = Arrays.asList(
				new ReadOperation() {
					{
						setOperationId("rdOp");
					}
				}, new WriteOperation() {
					{
						setOperationId("wrOp");
					}
				}, new LockOperation() {
					{
						setOperationId("lkOp");
					}
				});

		try {
			mc.execute(antennas, filters, operations);
		} catch (ParameterException e) {
			fail("Exception not expected");
		}

		new Verifications() {
			{
				hwMgr.execute(
					withSameInstance(antennas), 
					withSameInstance(filters), 
					withSameInstance(operations), 
					withSameInstance(consumer));
				times = 1;
			}
		};
		
		new NonStrictExpectations() {{
			configManager.getDefaultFilters();
			result = defaultFilters;
		}};
		
		try {
			mc.execute(antennas, new ArrayList<Filter>(), operations);
		} catch (ParameterException e) {
			fail("Exception not expected");
		}
		
		new Verifications() {{
			List<Filter> cptDefaultFilters;
			hwMgr.execute(withSameInstance(antennas), cptDefaultFilters = withCapture(), withSameInstance(operations), withSameInstance(consumer));
			times = 1;
			
			assertEquals(defaultFilters.size(), cptDefaultFilters.size());
			
			assertEquals(defaultFilters.get(0).getBank(), cptDefaultFilters.get(0).getBank());
			assertEquals(defaultFilters.get(0).getBitOffset(), cptDefaultFilters.get(0).getBitOffset());
			assertEquals(defaultFilters.get(0).getBitLength(), cptDefaultFilters.get(0).getBitLength());
			assertArrayEquals(defaultFilters.get(0).getMask(), cptDefaultFilters.get(0).getData());
			
			assertEquals(defaultFilters.get(1).getBank(), cptDefaultFilters.get(1).getBank());
			assertEquals(defaultFilters.get(1).getBitOffset(), cptDefaultFilters.get(1).getBitOffset());
			assertEquals(defaultFilters.get(1).getBitLength(), cptDefaultFilters.get(1).getBitLength());
			assertArrayEquals(defaultFilters.get(1).getMask(), cptDefaultFilters.get(1).getData());
			
		}};
	}

	@Test
	public void testGetHardwareManager(@Mocked final TestHardwareManager hwMgr, @Mocked final ConfigurationManager configManager) throws ImplementationException {
		/*
		 * Test:
		 * 	- getHardwareManager call
		 * Expected:
		 * 	- injected instance of TestHardwareManager is returned
		 */
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();
		setField(mc, "hwManager", hwMgr);
		assertEquals(hwMgr, mc.getHardwareManager());
	}
	
	@Test
	public void testInstallFirmware(@Mocked final TestHardwareManager hwMgr, @Mocked final ConfigurationManager configManager) throws ImplementationException {
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();
		mc.installFirmware();
		new Verifications() {{
			hwMgr.installFirmware();
			times = 1;
		}};
	}
	
	@Test
	public void testRestartKeepAliveThread(
		@Mocked final KeepAliveThread keepAliveThread,
		@Mocked final RFConsumer consumer,
		@Mocked final HardwareManager hwMgr, 
		@Mocked final KeepAliveConfiguration config,
		@Mocked final ConfigurationManager cfgManager) throws ImplementationException {
		
		/*
		 * Test:
		 * 	- stopKeepAliveThread
		 * Expected:
		 * 	- stop method of keepAliveThread is called
		 */
		setField(MainController.class, "instance", null);
		MainController mc = MainController.getInstance();
		setField(mc, "keepAliveThread", keepAliveThread);
		setField(mc, "consumer", consumer);
		mc.restartKeepAliveThread(null);		
		new Verifications() {
			{
				keepAliveThread.stop();
				times = 1;				
			}
		};
		
		/*
		 * Test:
		 * 	- startKeepAliveThread
		 * Expected:
		 * 	- run method of keepAliveThread is called
		 */
		new NonStrictExpectations() {{
			config.getInterval();
			result = 500;
			
			config.isEnable();
			result = true;
		
		}};
		setField(mc, "keepAliveThread", null);
		mc.restartKeepAliveThread(config);		
		new Verifications() {
			{
				keepAliveThread.run();
				times = 1;
			}
		};
	}
}
