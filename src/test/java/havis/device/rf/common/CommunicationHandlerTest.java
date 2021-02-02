package havis.device.rf.common;

import static mockit.Deencapsulation.getField;
import static mockit.Deencapsulation.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import havis.device.rf.RFConsumer;
import havis.device.rf.capabilities.CapabilityType;
import havis.device.rf.common.test.TestHardwareManager;
import havis.device.rf.configuration.Configuration;
import havis.device.rf.configuration.ConfigurationType;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.TagOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Before;
import org.junit.Test;

public class CommunicationHandlerTest {
	private static final Logger log = Logger.getLogger(CommunicationHandler.class.getName());
	
	@Mocked
	MainController mainController;

	@Mocked
	TestHardwareManager hwMgr;
	
	@Before
	public void setup() {
		log.setLevel(Level.ALL);
	}

	class TestConsumer implements RFConsumer {
		@Override
		public void connectionAttempted() {
		}

		@Override
		public List<TagOperation> getOperations(TagData arg0) {
			return null;
		}

		@Override
		public void keepAlive() {

		}
	}

	@Test
	public void testCommunicationHandler() {
		
		/* 
		 * Test: 
		 * - Constructor call
		 * 
		 * Expected: 
		 * - init method of MainController is called
		 * - mainController field of CommunicationHandler instance is null
		 */
		
		CommunicationHandler cmmHdl = new CommunicationHandler();
		
		new Verifications() {{
			MainController.init();
			times = 1;
		}};
		
		assertNull(getField(cmmHdl, "mainController"));
	}

	@Test
	public void testGetCapabilities() throws ConnectionException, ImplementationException {
		
		/* 
		 * Test: 
		 * - getCapabilities call with mainController field being null
		 * 
		 * Expected: 
		 * - Connection exception being thrown 
		 */
		
		CommunicationHandler cmmHdl = new CommunicationHandler();
		final CapabilityType capType = CapabilityType.DEVICE_CAPABILITIES;

		try {
			cmmHdl.getCapabilities(capType);
			fail("Exception expected but none thrown.");
		} catch (ConnectionException ex) { 			
		} catch (ImplementationException ex) {
			fail("Exception not expected.");
		}
		
		/* 
		 * Test: 
		 * - getCapabilities call with mainController field being not null
		 * 
		 * Expected: 
		 * - no exception being throw
		 * - getCapabilities of mainController instance called once with given capability type 
		 */
		
		setField(cmmHdl, "mainController", mainController);				
		try {
			cmmHdl.getCapabilities(capType);
		} catch (ConnectionException | ImplementationException ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				CapabilityType captCapType = null;
				mainController.getCapabilities(captCapType = withCapture());
				times = 1;
				assertEquals(capType, captCapType);
			}
		};
	}

	@Test
	public void testGetConfiguration() throws ImplementationException {
		CommunicationHandler cmmHdl = new CommunicationHandler();
		final ConfigurationType type = ConfigurationType.ANTENNA_CONFIGURATION;
		final short antennaID = 1;
		
		/* 
		 * Test: 
		 * - getConfiguration call with mainController field being null
		 * 
		 * Expected: 
		 * - Connection exception being thrown 
		 */		

		try {
			cmmHdl.getConfiguration(type, antennaID, (short) 0, (short) 0);
			fail("Exception expected but none thrown.");
		} catch (ConnectionException cex) {
		} catch (ImplementationException iex) {
			fail("Exception not expected.");
		}

		
		setField(cmmHdl, "mainController", mainController);
		
		/* 
		 * Test: 
		 * - getConfiguration call with mainController field being not null
		 * 
		 * Expected: 
		 * - getConfiguration of mainController called with given configuration type and antenna id
		 */		
		try {
			cmmHdl.getConfiguration(type, antennaID, (short) 0, (short) 0);
		} catch (ConnectionException | ImplementationException ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				ConfigurationType captType = null;
				short captAntennaID = 0;
				mainController.getConfiguration(captType = withCapture(), captAntennaID = withCapture(), (short) 0, (short) 0);
				times = 1;
				assertEquals(captType, type);
				assertEquals(captAntennaID, antennaID);
			}
		};		
	}

	@Test
	public void testSetConfiguration() throws ParameterException, ImplementationException {
		CommunicationHandler cmmHdl = new CommunicationHandler();
		final List<Configuration> configs = new ArrayList<>();

		/* 
		 * Test: 
		 * - setConfiguration call with mainController field being null
		 * 
		 * Expected: 
		 * - Connection exception being thrown 
		 */		
		
		try {
			cmmHdl.setConfiguration(configs);
			fail("Exception expected but none thrown.");
		} catch (ConnectionException cex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		/* 
		 * Test: 
		 * - getConfiguration call with mainController field being not null
		 * 
		 * Expected: 
		 * - setConfiguration of mainController called with given configuration list 
		 */				
		setField(cmmHdl, "mainController", mainController);
		try {
			cmmHdl.setConfiguration(configs);
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				List<Configuration> captConfigs = null;
				mainController.setConfiguration(captConfigs = withCapture());
				times = 1;
				assertEquals(captConfigs, configs);
			}
		};
		
	}

	@Test
	public void testResetConfiguration() throws ImplementationException, ParameterException {
		CommunicationHandler cmmHdl = new CommunicationHandler();
		
		/* 
		 * Test: 
		 * - resetConfiguration call with mainController field being null
		 * 
		 * Expected: 
		 * - Connection exception being thrown 
		 */				
		try {
			cmmHdl.resetConfiguration();
			fail("Exception expected but none thrown.");
		} catch (ConnectionException cex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		/* 
		 * Test: 
		 * - resetConfiguration call with mainController field being not null
		 * 
		 * Expected: 
		 * - resetConfiguration of mainController called 
		 */			
		setField(cmmHdl, "mainController", mainController);
		try {
			cmmHdl.resetConfiguration();
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				mainController.resetConfiguration();
				times = 1;
			}
		};
	}

	@Test
	public void testOpenConnection() throws ImplementationException, ConnectionException {
		/* 
		 * Test: 
		 * - successful openConnection call 
		 * 
		 * Expected: 
		 * - MainController.getInstance called
		 * - mainController field being null before and not null afterwards
		 * - openConnection on mainController instance called with given consumer and timeout
		 */		
		new NonStrictExpectations() {
			{
				MainController.getInstance();
				result = mainController;
			}
		};

		CommunicationHandler cmmHdl = new CommunicationHandler();
		final RFConsumer consumer = new TestConsumer();
		final int timeout = 500;

		assertNull(getField(cmmHdl, "mainController"));
		try {
			cmmHdl.openConnection(consumer, timeout);
		} catch (ConnectionException e) {
			fail("Exception not expected");
		}

		new Verifications() {
			{
				MainController.getInstance();
				times = 1;				
				RFConsumer captConsumer;
				int captTimeout;
				mainController.openConnection(captConsumer = withCapture(), captTimeout = withCapture());
				times = 1;
				assertEquals(consumer, captConsumer);
				assertEquals(timeout, captTimeout);
			}
		};
		assertTrue(getField(cmmHdl, "mainController") != null);

		
		/* 
		 * Test: 
		 * - openConnection call with ConnectionException 
		 * 
		 * Expected:
		 * - MainController.getInstance called
		 * - mainController field being null afterwards 
		 * - Connection exception being thrown
		 */			
		setField(cmmHdl, "mainController", null);
		new NonStrictExpectations() {
			{
				MainController.getInstance();
				result = mainController;
				mainController.openConnection(withInstanceOf(RFConsumer.class), anyInt);
				result = new ConnectionException();
			}
		};
		cmmHdl = new CommunicationHandler();
		try {
			cmmHdl.openConnection(consumer, timeout);
			fail("Exception expected but none thrown.");
		} catch (ConnectionException ex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}
		
		new Verifications() {
			{
				MainController.getInstance();
				times = 1;								
			}
		};
		assertNull(getField(cmmHdl, "mainController"));
	}

	@Test
	public void testCloseConnection() {
		/* 
		 * Test: 
		 * - closeConnection call with mainController field being null 
		 * 
		 * Expected:
		 * - Connection exception being thrown
		 */			
		
		CommunicationHandler cmmHdl = new CommunicationHandler();

		try {
			cmmHdl.closeConnection();
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		/* 
		 * Test: 
		 * - closeConnection call with mainController field being not null 
		 * 
		 * Expected:
		 * - mainController field being null afterwards
		 */	
		setField(cmmHdl, "mainController", mainController);
		try {
			cmmHdl.closeConnection();
		} catch (Exception ex) {
			fail("Exception not expected.");
		}
		assertNull(getField(cmmHdl, "mainController"));
		
	}

	@Test
	public void testGetSupportedRegions() {
		/* 
		 * Test: 
		 * - getSupportedRegions call with mainController field being null 
		 * 
		 * Expected:
		 * - Connection exception being thrown
		 */			
		CommunicationHandler cmmHdl = new CommunicationHandler();

		try {
			cmmHdl.getSupportedRegions();
			fail("Exception expected but none thrown.");
		} catch (ConnectionException cex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		/* 
		 * Test: 
		 * - getSupportedRegions call with mainController field being not null 
		 * 
		 * Expected:
		 * - getSupportedRegions called on mainController instance
		 */		
		setField(cmmHdl, "mainController", mainController);
		try {
			cmmHdl.getSupportedRegions();
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				mainController.getSupportedRegions();
				times = 1;
			}
		};			
	}

	@Test
	public void testGetRegion() {
		/* 
		 * Test: 
		 * - getRegion call with mainController field being null 
		 * 
		 * Expected:
		 * - Connection exception being thrown
		 */			
		CommunicationHandler cmmHdl = new CommunicationHandler();

		try {
			cmmHdl.getRegion();
			fail("Exception expected but none thrown.");
		} catch (ConnectionException cex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		/* 
		 * Test: 
		 * - getRegion call with mainController field being not null
		 * 
		 * Expected:
		 * - getRegion called on mainController instance
		 */	
		setField(cmmHdl, "mainController", mainController);
		try {
			cmmHdl.getRegion();
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				mainController.getRegion();
				times = 1;
			}
		};
	}

	@Test
	public void testSetRegion() throws ParameterException, ImplementationException {	
		/* 
		 * Test: 
		 * - setRegion call with mainController field being null
		 * 
		 * Expected: 
		 * - Connection exception being thrown 
		 */		
		CommunicationHandler cmmHdl = new CommunicationHandler();
		final String region = "eu";

		try {
			cmmHdl.setRegion(region);
			fail("Exception expected but none thrown.");
		} catch (ConnectionException cex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}
		
		/* 
		 * Test: 
		 * - setRegion call with mainController field being not null
		 * 
		 * Expected: 
		 * - setRegion of mainController called with given region 
		 */
		setField(cmmHdl, "mainController", mainController);
		try {
			cmmHdl.setRegion(region);
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				String captRegion = null;
				mainController.setRegion(captRegion = withCapture());
				times = 1;
				assertEquals(captRegion, region);
			}
		};
	}

	@Test
	public void testExecute() throws ImplementationException, ParameterException {
		/* 
		 * Test: 
		 * - execute call with mainController field being null
		 * 
		 * Expected: 
		 * - Connection exception being thrown 
		 */				
		CommunicationHandler cmmHdl = new CommunicationHandler();
		final List<Short> antennas = Arrays.asList((short) 1, (short) 2, (short) 4);
		final List<Filter> filters = new ArrayList<Filter>();
		final List<TagOperation> operations = new ArrayList<TagOperation>();
		
		try {
			cmmHdl.execute(antennas, filters, operations);
			fail("Exception expected but none thrown.");
		} catch (ConnectionException cex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		/* 
		 * Test: 
		 * - execute call with mainController field being not null
		 * 
		 * Expected: 
		 * - execute called on mainController instance   
		 */
		setField(cmmHdl, "mainController", mainController);
		try {
			cmmHdl.execute(antennas, filters, operations);
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				List<Short> captAntennas = null;
				List<Filter> captFilters = null;
				List<TagOperation> captOperations = null;
				mainController.execute(captAntennas = withCapture(), captFilters = withCapture(), captOperations = withCapture());
				times = 1;
				assertEquals(captAntennas, antennas);
				assertEquals(captFilters, filters);
				assertEquals(captOperations, operations);
			}
		};

		new NonStrictExpectations() {
			{
				mainController.execute(withInstanceLike(antennas), withInstanceLike(filters), withInstanceLike(operations));
				result = new RuntimeException();
			}
		};

		try {
			cmmHdl.execute(antennas, filters, operations);
			fail("Exception expected but none thrown.");
		} catch (RuntimeException ex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}
	}

	@Test
	public void installFirmware() throws ImplementationException, ParameterException {
		/* 
		 * Test: 
		 * - installFirmware call with mainController field being null 
		 * 
		 * Expected:
		 * - Connection exception being thrown
		 */			
		CommunicationHandler cmmHdl = new CommunicationHandler();

		try {
			cmmHdl.installFirmware();
			fail("Exception expected but none thrown.");
		} catch (ConnectionException cex) {
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		/* 
		 * Test: 
		 * - installFirmware call with mainController field being not null
		 * 
		 * Expected:
		 * - installFirmware called on mainController instance
		 */	
		setField(cmmHdl, "mainController", mainController);
		try {
			cmmHdl.installFirmware();
		} catch (Exception ex) {
			fail("Exception not expected.");
		}

		new Verifications() {
			{
				mainController.installFirmware();
				times = 1;
			}
		};
	}
}
