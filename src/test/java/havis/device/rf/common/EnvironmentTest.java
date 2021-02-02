package havis.device.rf.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

public class EnvironmentTest {

	private static final Logger log = Logger.getLogger(Environment.class.getName());
	
	private Properties properties;
	
	private static final String FILE_NAME = "havis.device.rf.properties";
	

	@Before
	public void setup() throws Exception {
		log.setLevel(Level.ALL);
		
		try (InputStream propStream = this.getClass().getClassLoader()
				.getResourceAsStream(FILE_NAME)) {
			Properties properties = new Properties();
			properties.load(propStream);
			this.properties = properties;
		}
	}

	@Test 
	public void testUNSPECIFIED_REGION_ID() {
		assertEquals(
			properties.getProperty("havis.device.rf.common.unspecifiedRegionId"), 
			Environment.UNSPECIFIED_REGION_ID);
	}
	
	@Test 
	public void testDEFAULT_REGION_ID() {
		assertEquals(
			properties.getProperty("havis.device.rf.common.defaultRegionId"), 
			Environment.DEFAULT_REGION_ID);
	}
	
	@Test 
	public void testCUSTOM_CONFIG_FILE() {
		assertEquals(
			properties.getProperty("havis.device.rf.common.currentConfigFile"), 
			Environment.CUSTOM_CONFIG_FILE);
	}
	
	@Test 
	public void testRESULT_FILE() {
		assertEquals(
			properties.getProperty("havis.device.rf.common.resultFile"), 
			Environment.RESULT_FILE);
	}
	
	@Test 
	public void testHARDWARE_MANAGER_CLASS() {
		assertEquals(
			properties.getProperty("havis.device.rf.common.hardwareManager.class"), 
			Environment.HARDWARE_MANAGER_CLASS);
	}		
	
	@Test 
	public void testSERIAL_DEVICE_PATH() {
		assertEquals(
			properties.getProperty("havis.device.rf.common.serialDevicePath"), 
			Environment.SERIAL_DEVICE_PATH);
	}
	
	@Test 
	public void testBROKEN_RESULT_STORE_LOCATION() {
		assertEquals(
			properties.getProperty("havis.device.rf.common.brokenResultStoreLocation"), 
			Environment.BROKEN_RESULT_STORE_LOCATION);
	}
	
	@Test 
	public void testFIRMWARE_UPDATE_SCRIPT() {
		assertEquals(				
			properties.getProperty("havis.device.rf.common.firmware.update.script"), 
			Environment.FIRMWARE_UPDATE_SCRIPT);
	}
	
	@Test 
	public void testPERSIST_RESULTS() {
		assertEquals(				
			Boolean.parseBoolean(properties.getProperty("havis.device.rf.common.persistResults")), 
			Environment.PERSIST_RESULTS);
	}
	
	@Test 
	public void testSERIALIZER_PRETTY_PRINT() {
		assertEquals(				
			Boolean.parseBoolean(properties.getProperty("havis.device.rf.common.serializerPrettyPrint")), 
			Environment.SERIALIZER_PRETTY_PRINT);
	}
	
	@Test 
	public void testOPTIMIZED_TID_BANK_READING() {
		assertEquals(				
			Boolean.parseBoolean(properties.getProperty("havis.device.rf.common.optimizedTidBankReading")), 
			Environment.OPTIMIZED_TID_BANK_READING);
	}
	
	@Test 
	public void testHANDLE_TRANSPONDER_EPC_CHANGE() {
		assertEquals(				
			Boolean.parseBoolean(properties.getProperty("havis.device.rf.common.handleTransponderEpcChange")), 
			Environment.HANDLE_TRANSPONDER_EPC_CHANGE);
	}
	
	@Test 
	public void testCOMPLETE_USERBANK_WORD_COUNT() {
		assertEquals(				
			Short.parseShort(properties.getProperty("havis.device.rf.common.completeUserBankWordCount")), 
			(short)Environment.COMPLETE_USERBANK_WORD_COUNT);
		
	}
	
	private List<Short> parseAntennasProperty(String propId) {
		String antStr = properties.getProperty(propId);
		if (antStr != null) {
			try {
				String[] antStrs = antStr.split("\\s*\\,\\s*");
				List<Short> ret = new ArrayList<Short>();
				for (String s : antStrs)
					ret.add(Short.parseShort(s));
				return ret;
			} catch (Exception ex) { }
		}
		return null;
	}
	
	@Test 
	public void testHARDWARE_MANAGER_ANTENNAS() {
		Object[] exp = parseAntennasProperty("havis.device.rf.common.hardwareManager.antennas").toArray();
		Object[] act = Environment.HARDWARE_MANAGER_ANTENNAS.toArray();		
		assertArrayEquals(exp, act);
	}
	
	@Test 
	public void testSERIAL_DEVICE_BAUDRATE() {
		assertEquals(Baudrate.valueOf(properties.getProperty("havis.device.rf.common.serialDeviceBaudrate")), Environment.SERIAL_DEVICE_BAUDRATE);
	}	
}