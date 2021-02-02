package havis.device.rf.common;

import static mockit.Deencapsulation.getField;
import static mockit.Deencapsulation.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import havis.device.rf.capabilities.Capabilities;
import havis.device.rf.capabilities.CapabilityType;
import havis.device.rf.capabilities.DeviceCapabilities;
import havis.device.rf.capabilities.RegulatoryCapabilities;
import havis.device.rf.common.test.TestHardwareManager;
import havis.device.rf.common.util.FileUtils;
import havis.device.rf.common.util.RFUtils;
import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.AntennaConfigurationList;
import havis.device.rf.configuration.AntennaProperties;
import havis.device.rf.configuration.AntennaPropertyList;
import havis.device.rf.configuration.Configuration;
import havis.device.rf.configuration.ConfigurationType;
import havis.device.rf.configuration.ConnectType;
import havis.device.rf.configuration.InventorySettings;
import havis.device.rf.configuration.KeepAliveConfiguration;
import havis.device.rf.configuration.RFConfiguration;
import havis.device.rf.configuration.RFRegion;
import havis.device.rf.configuration.RssiFilter;
import havis.device.rf.configuration.SelectionMask;
import havis.device.rf.configuration.SingulationControl;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Before;
import org.junit.Test;

public class ConfigurationManagerTest {
	private static final Logger log = Logger.getLogger(ConfigurationManager.class.getName());

	private final String CUST_CFG_FILE = "classes/conf/havis/device/rf/config.json";
	private final String CUST_CFG_FILE_WITH_INV_SETTINGS = "classes/conf/havis/device/rf/config_with_inv_settings.json";

	@Before
	@SuppressWarnings("serial")
	public void setup() throws Exception {
		
		log.setLevel(Level.ALL);
		
		new NonStrictExpectations() { {						
			setField(Environment.class, "CUSTOM_CONFIG_FILE", CUST_CFG_FILE);
			setField(Environment.class, "DEFAULT_CONFIG", 
				new RFConfiguration() {{
					setRegion("Unspecified");
					setAntennaConfigurationList(new AntennaConfigurationList() {{
						setEntryList(new ArrayList<AntennaConfiguration>() {{
							add(new AntennaConfiguration() {{
								setChannelIndex((short)1);
								setTransmitPower((short)2);
								setHopTableID((short)3);
							}});
							
							add(new AntennaConfiguration() {{
								setChannelIndex((short)2);
								setTransmitPower((short)3);
								setHopTableID((short)4);
							}});
						}});
					}});				
				}}
			);
		}};
	}

	@Test
	public void testConfigurationManager(@Mocked final MainController mainController) throws ImplementationException, URISyntaxException {
		/*
		 * Test:
		 * 	- Constructor call
		 * 
		 * Expected:
		 * 	- all instance variables of config manager instance are set   
		 */
		ConfigurationManager cfgMgr = new ConfigurationManager(mainController);
		assertTrue(getField(cfgMgr, "supportedRegions") != null);
		assertEquals(CUST_CFG_FILE, getField(cfgMgr, "customConfigFile"));
		assertEquals(getField(cfgMgr, "mainController"), mainController);
	}

	@Test
	public void testLoadConfiguration(@Mocked final MainController mainController, @Mocked final HardwareManager hwMgr) 
			throws ImplementationException, InterruptedException, ParameterException, IOException {
		final RssiFilter rssiFilter = new RssiFilter();
		final SingulationControl singCtrl = new SingulationControl();
		
		new NonStrictExpectations() {{
			mainController.getHardwareManager();
			result = hwMgr;		
			
			hwMgr.getRssiFilter();
			result = rssiFilter;
			
			hwMgr.getSingulationControl();
			result = singCtrl;						
		}};
		
		/*
		 * Test:
		 * 	- loadConfig with existing custom config file
		 * 
		 * Expected:
		 * 	- region "EU" is set on hardware manager
		 * 	- antennaConfigList has two entries 
		 * 	- transmitPower entry 0 is 10
		 * 	- connectType of entry 0 is true   
		 *  - rssi filter and singulation control is set from hwMgr
		 */
		final ConfigurationManager cfgMgr1 = new ConfigurationManager(mainController);
		cfgMgr1.loadConfiguration();
		
		new Verifications() {{
			RFRegion captReg;
			AntennaConfigurationList captAntCfgList;
			hwMgr.setRegion(captReg = withCapture(), captAntCfgList = withCapture());			
			assertEquals("EU", captReg.getId());
			assertEquals(2, captAntCfgList.getEntryList().size());
			AntennaConfiguration aCfg = captAntCfgList.getEntryList().get(0);
			assertEquals((short)10, (short)aCfg.getTransmitPower()); 
			assertEquals(ConnectType.TRUE, aCfg.getConnect());				
			assertEquals(rssiFilter, ((RFConfiguration)getField(cfgMgr1, "config")).getInventorySettings().getRssiFilter());
			assertEquals(singCtrl, ((RFConfiguration)getField(cfgMgr1, "config")).getInventorySettings().getSingulationControl());
		}};
		
		/*
		 * Test:
		 * 	- loadConfig with existing custom config file including inventory settings  
		 * 
		 * Expected:
		 * 	- rssi filter and singulation control is used from config file and applied to hwMgr
		 */
		
		new NonStrictExpectations() {{
			setField(Environment.class, "CUSTOM_CONFIG_FILE", CUST_CFG_FILE_WITH_INV_SETTINGS);
		}};
		
		final ConfigurationManager cfgMgr1_1 = new ConfigurationManager(mainController);		
		cfgMgr1_1.loadConfiguration();
		
		new Verifications() {{			
			RssiFilter rssiFilter;
			SingulationControl singulationControl;
			
			hwMgr.setRssiFilter(rssiFilter = withCapture());
			hwMgr.setSingulationControl(singulationControl = withCapture());
			
			assertEquals(((RFConfiguration)getField(cfgMgr1_1, "config")).getInventorySettings().getRssiFilter(), rssiFilter);
			assertEquals(((RFConfiguration)getField(cfgMgr1_1, "config")).getInventorySettings().getSingulationControl(), singulationControl);
		}};
		
		/*
		 * Test:
		 * 	- loadConfig with non-existing custom config file
		 * 
		 * Expected:
		 * 	- setRegion() is called with region "Unspecified"
		 */
		final File tmpFile = File.createTempFile("current_", ".json");
		tmpFile.delete();
		
		setField(Environment.class, "CUSTOM_CONFIG_FILE", tmpFile.getAbsolutePath());		
		final ConfigurationManager cfgMgr2 = new ConfigurationManager(mainController);
		
		new NonStrictExpectations(cfgMgr2) {{
			cfgMgr2.setRegion(anyString);
			result = null;
			
			hwMgr.getMaxAntennas();
			result = 1;
		}};
		
		cfgMgr2.loadConfiguration();
		new Verifications() {{	
			cfgMgr2.setRegion("Unspecified");
			
			RFConfiguration cfg = getField(cfgMgr2, "config");
			assertEquals(1, cfg.getAntennaConfigurationList().getEntryList().size());
			
			hwMgr.getMaxAntennas();
		}};
		
		/*
		 * Test:
		 * 	- loadConfig with non-existing custom config file and setRegion throwing ParameterException
		 * 
		 * Expected:
		 * 	- ImplementationException thrown  
		 */
		
		final File tmpFile2 = File.createTempFile("current_", ".json");
		tmpFile2.delete();
		setField(Environment.class, "CUSTOM_CONFIG_FILE", tmpFile2.getAbsolutePath());
		final ConfigurationManager cfgMgr3 = new ConfigurationManager(mainController);
		new NonStrictExpectations() {{
			cfgMgr3.setRegion(anyString);
			result = new ParameterException();
		}};
		
		try {
			cfgMgr3.loadConfiguration();
			fail("Exception expected.");
		} catch (ImplementationException e) { }
		
		
		/*
		 * Test:
		 * 	- loadConfig with existing custom config file and IOException during load
		 * 
		 * Expected:
		 * - resetConfig is called
		 * - ImplementationException is thrown
		 * 	  
		 */
		setField(Environment.class, "CUSTOM_CONFIG_FILE", "/dev/null");
		final ConfigurationManager cfgMgr4 = new ConfigurationManager(mainController);
		
		new NonStrictExpectations() {{
			cfgMgr4.resetConfig();
			result = null;
		}};
		
		try {
			cfgMgr4.loadConfiguration();
			fail("Exception expected.");
		} catch (ImplementationException e) {
			
		} catch (Exception e) {
			fail("Unexpected exception.");
		}

		new Verifications() {{
			cfgMgr4.resetConfig();
		}};
	}

	@Test
	public void testResetConfig(@Mocked final MainController mainController) throws IOException, ImplementationException, ParameterException {
		/*
		 * Test:
		 * 	- resetConfig call 
		 * Expected:
		 * 	- custom config file is being deleted
		 */
		final File tmpFile = File.createTempFile("current_", ".json");
		final ConfigurationManager cfgMgr = new ConfigurationManager(mainController);
		setField(cfgMgr, "customConfigFile", tmpFile.getAbsolutePath());
		assertTrue(tmpFile.exists());
		cfgMgr.resetConfig();
		assertTrue(!tmpFile.exists());
	}
	
	@Test
	public void testSetRegion(@Mocked final RFConfiguration config, @Mocked final MainController mainController, @Mocked final TestHardwareManager hwMgr) throws ImplementationException, ParameterException {		

		final ConfigurationManager cfgMgr = new ConfigurationManager(mainController);
		
		final AntennaConfigurationList acl = new AntennaConfigurationList();
		acl.getEntryList().add(new AntennaConfiguration() {
			{
				setHopTableID((short) 1);
				setChannelIndex((short) 2);
				setTransmitPower((short) 3);
			}
		});
		
		new NonStrictExpectations(cfgMgr) {{
			cfgMgr.saveConfig();
			result = null;
			
			config.getAntennaConfigurationList();
			result = acl;
		}};
		
		setField(cfgMgr, "config", config);

		/* 
		 * Test:
		 * 	- setRegion called with null
		 * Expected:
		 * 	- ParameterException being thrown 
		 */		
		try {
			cfgMgr.setRegion(null);
			fail("Exception expected but none thrown.");
		} catch (ParameterException e) {
		}

		/*
		 * Test:
		 * 	- setRegion being called with non-existing region ID
		 * Expected:
		 * 	- ParameterException being thrown
		 */
		try {
			cfgMgr.setRegion("unsupported_region_id");
			fail("Exception expected but none thrown.");
		} catch (ParameterException e) {
		}

		/*
		 * Test:
		 * 	- setRegion called with region EU
		 * Expected:
		 *  - setRegion called on hardware manager instance
		 * 	- saveConfig called once
		 *  - channelIndex, hopTableId and transmitPower being reset to 0
		 */
		try {
			cfgMgr.setRegion("EU");
		} catch (ParameterException e) {
			fail("Exception not expected.");
		}

		new Verifications() {{
			cfgMgr.saveConfig();			
			config.setRegion("EU");			
			RFRegion captReg;
			AntennaConfigurationList captAcl;
			hwMgr.setRegion(captReg = withCapture(), captAcl = withCapture());
			assertEquals("EU", captReg.getId());
			assertEquals(1, captAcl.getEntryList().size());
			assertEquals((short) captAcl.getEntryList().get(0).getChannelIndex(), (short) 0);
			assertEquals((short) captAcl.getEntryList().get(0).getHopTableID(), (short) 0);
			assertEquals((short) captAcl.getEntryList().get(0).getTransmitPower(), (short) 0);
		}};
	}

	@Test
	public void testSaveConfig(@Mocked final RFConfiguration rfcConfig, @Mocked final MainController mainController) throws ImplementationException, IOException {
		/*
		 * Test:
		 *  - saveConfig with non-existing custom config file
		 * Expected:
		 * 	- file is created
		 */		
		final File tmpFile = File.createTempFile("current_", ".json");
		tmpFile.delete();
		
		setField(Environment.class, "CUSTOM_CONFIG_FILE", tmpFile.getAbsolutePath());
		ConfigurationManager cfgMgr = new ConfigurationManager(mainController);
		setField(cfgMgr, "config", rfcConfig);
		
		try {
			setField(cfgMgr, "unsavedChanges", true);
			cfgMgr.saveConfig();
			assertTrue(!(boolean) getField(cfgMgr, "unsavedChanges"));
			assertTrue(tmpFile.exists());
		}

		finally {
			tmpFile.delete();
		}
		
		/*
		 * Test:
		 *  - saveConfig with existing custom config file
		 * Expected:
		 * 	- file has a length of 0 before and a length > 0 afterwards
		 */		
		final File tmpFile2 = File.createTempFile("current_", ".json");
		setField(Environment.class, "CUSTOM_CONFIG_FILE", tmpFile2.getAbsolutePath());
		cfgMgr = new ConfigurationManager(mainController);
		setField(cfgMgr, "config", rfcConfig);
		
		assertEquals(0, tmpFile2.length());
		try {
			setField(cfgMgr, "unsavedChanges", true);
			cfgMgr.saveConfig();
			assertTrue(tmpFile2.length() > 0);			
		} finally {
			tmpFile2.delete();
		}
		
		/*
		 * Test:
		 *  - saveConfig FileUtils.serialize throwing an IOException
		 * Expected:
		 * 	- ImplementationException being thrown
		 */		
		new NonStrictExpectations(FileUtils.class) {{
			FileUtils.serialize((File) any, any);
			result = new IOException();
		}};
		cfgMgr = new ConfigurationManager(mainController);
		setField(cfgMgr, "config", rfcConfig);
		try {
			cfgMgr.saveConfig();
			fail("Exception expected.");
		} catch (ImplementationException e) {
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetSupportedRegions(@Mocked final MainController mainController) throws ImplementationException {
		/*
		 * Test:
		 * 	- getSupportedRegions call
		 * Expected:
		 * 	- result returned by this method equals the value of the instance variable supportedRegions
		 */
		final ConfigurationManager cfgMgr = new ConfigurationManager(mainController);
		assertEquals(cfgMgr.getSupportedRegions(), ((Map<String, String>) getField(cfgMgr, "supportedRegions")).keySet());
	}

	@Test
	public void testSetConfiguration(@Mocked final MainController mainController, @Mocked final TestHardwareManager hwMgr) throws ImplementationException, ParameterException {
		
		new NonStrictExpectations() {{
			mainController.getHardwareManager();
			result = hwMgr;
		}};
		
		final ConfigurationManager cfgManager = new ConfigurationManager(mainController);		
		final RFConfiguration conf = new RFConfiguration();
		conf.setAntennaConfigurationList(new AntennaConfigurationList());
		final RegulatoryCapabilities regCaps = new RegulatoryCapabilities();
		conf.setRegulatoryCapabilities(regCaps);
		setField(cfgManager, "config", conf);

		/*
		 * Test:
		 * 	- set keep-alive-configuration having an interval > 0
		 * Expected:
		 * 	- no exception thrown
		 *  - mainController.restartKeepAliveThread is called with keep-alive-config
		 */		
		final KeepAliveConfiguration kac = new KeepAliveConfiguration();
		kac.setEnable(true);
		kac.setInterval(50);

		try {
			cfgManager.setConfiguration(kac);
		} catch (ParameterException e) {
			fail("Exception not expected.");
		}

		new Verifications() {{			
			mainController.restartKeepAliveThread(withSameInstance(kac));
		}};

		/*
		 * Test:
		 * 	- set keep-alive-configuration having an interval = 0
		 * Expected:
		 * 	- parameter exception thrown
		 */		
		kac.setEnable(true);
		kac.setInterval(0);

		try {
			cfgManager.setConfiguration(kac);
			fail("Exception expected.");
		} catch (ParameterException e) {

		}
		
		/*
		 * Test:
		 * 	- call of setConfiguration method twice with different antenna config for non-existent antenna
		 * Expected
		 * 	- parameter exception thrown
		 */
		
		final AntennaConfiguration antConf1 = new AntennaConfiguration() {{ setId((short) 1); }};				
		
		try {
			cfgManager.setConfiguration(antConf1);
			fail("Exception expected");
		} catch (ParameterException e) {
			
		}
		
		/* add two antenna config objects for further tests */
		RFConfiguration rfConfig = (RFConfiguration) getField(cfgManager, "config");
		rfConfig.getAntennaConfigurationList().getEntryList().add(
			new AntennaConfiguration() {{ 
				setId((short)1); 
				setConnect(ConnectType.AUTO);
				setTransmitPower((short)5);
			}});
		
		rfConfig.getAntennaConfigurationList().getEntryList().add(
			new AntennaConfiguration() {{ 
				setId((short)2); 
				setConnect(ConnectType.AUTO);
				setTransmitPower((short)10);
			}});
		
		/* 
		 * Test:
		 * 	- set connect type and transmit power of antenna with ID 0
		 * Expected:
		 * 	- transmit power of both antennas is changed
		 *  - setAntennaConfiguration method of hardware manager is called twice 
		 **/
		
		AntennaConfiguration newCfg = new AntennaConfiguration() {{ setId((short) 0); setTransmitPower((short) 15); setConnect(ConnectType.TRUE); }};
		cfgManager.setConfiguration(newCfg);
		
		rfConfig = (RFConfiguration) getField(cfgManager, "config");
		assertEquals(ConnectType.TRUE, rfConfig.getAntennaConfigurationList().getEntryList().get(0).getConnect());
		assertEquals(new Short((short) 15), rfConfig.getAntennaConfigurationList().getEntryList().get(0).getTransmitPower());
		assertEquals(ConnectType.TRUE, rfConfig.getAntennaConfigurationList().getEntryList().get(1).getConnect());
		assertEquals(new Short((short) 15), rfConfig.getAntennaConfigurationList().getEntryList().get(1).getTransmitPower());		
		
		new Verifications() {{
			hwMgr.setAntennaConfiguration(withInstanceOf(AntennaConfiguration.class), withSameInstance(regCaps), false);
			times = 2;
		}};
		
		/*
		 * Test:
		 * 	- call of setConfiguration method with AntennaProperties object
		 * Expected
		 * 	- connectType of antenna config is set according to isConnected property of antenna properties object
		 *  - setAntennaConfiguration method of hardware manager is called
		 */
		AntennaProperties antProps = new AntennaProperties() {{ setId((short) 0); setConnected(false); }};
		cfgManager.setConfiguration(antProps);		
		rfConfig = (RFConfiguration) getField(cfgManager, "config");
		assertEquals(ConnectType.FALSE, rfConfig.getAntennaConfigurationList().getEntryList().get(0).getConnect());
		assertEquals(ConnectType.FALSE, rfConfig.getAntennaConfigurationList().getEntryList().get(1).getConnect());
		
		new Verifications() {{
			hwMgr.setAntennaConfiguration(withInstanceOf(AntennaConfiguration.class), withSameInstance(regCaps), false);
			times = 4;			
		}};
		
		antProps = new AntennaProperties() {{ setId((short) 0); setConnected(true); }};
		cfgManager.setConfiguration(antProps);		
		rfConfig = (RFConfiguration) getField(cfgManager, "config");
		assertEquals(ConnectType.TRUE, rfConfig.getAntennaConfigurationList().getEntryList().get(0).getConnect());
		assertEquals(ConnectType.TRUE, rfConfig.getAntennaConfigurationList().getEntryList().get(1).getConnect());
		
		new Verifications() {{
			hwMgr.setAntennaConfiguration(withInstanceOf(AntennaConfiguration.class), withSameInstance(regCaps), false);
			times = 6;			
		}};
				
		/*
		 * Test:
		 * 	- call of setConfiguration with InventorySettings (RssiFilter)
		 *  - RSSI filter is successfully applied to hwMgr
		 * Expected:
		 *  - config is applied and unsavedChanges if therefore true
		 */
		
		conf.setInventorySettings(new InventorySettings());
		final InventorySettings invSettings = new InventorySettings();
		RssiFilter rssiFilter = new RssiFilter() {{ setMinRssi((short) -64); setMaxRssi((short) 64); }};
		invSettings.setRssiFilter(rssiFilter);
		setField(cfgManager, "unsavedChanges", false);
		
		new NonStrictExpectations() {{
			hwMgr.setRssiFilter(withInstanceOf(RssiFilter.class));
			result = null;
		}};
		
		try {
			cfgManager.setConfiguration(invSettings);
		} catch (Exception e1) {
			fail("Exception unexpected.");
		}
		new Verifications() {{
			assertEquals(true, getField(cfgManager, "unsavedChanges"));
		}};
		
		/*
		 * Test:
		 * 	- call of setConfiguration with InventorySettings (RssiFilter)
		 *  - hwMgr throws exception during application of RSSI filter
		 * Expected:
		 *  - config is rolled back and unsavedChanges if therefore false
		 */
		
		new NonStrictExpectations() {{
			hwMgr.setRssiFilter(withInstanceOf(RssiFilter.class));
			result = new ImplementationException();
		}};
		setField(cfgManager, "unsavedChanges", false);		
		try {
			cfgManager.setConfiguration(invSettings);
			fail("Exception expected.");
		} catch (ParameterException e) { }		
		
		new Verifications() {{
			assertEquals(false, getField(cfgManager, "unsavedChanges"));
		}};
		
		invSettings.setRssiFilter(null);
		
		/*
		 * Test:
		 * 	- call of setConfiguration with InventorySettings (SingulationControl)
		 *  - singulation control is successfully applied to hwMgr
		 * Expected:
		 *  - config is applied and unsavedChanges if therefore true
		 */
		
		SingulationControl singCtl = new SingulationControl() {{ 
			setQValue((short) 1); 
			setRounds((short) 2); 
			setSession((short) 3); 
			setTransitTime((short) 200); 
		}};
		invSettings.setSingulationControl(singCtl);
		
		setField(cfgManager, "unsavedChanges", false);
		
		new NonStrictExpectations() {{
			hwMgr.setSingulationControl(withInstanceOf(SingulationControl.class));
			result = null;
		}};
		
		try {
			cfgManager.setConfiguration(invSettings);
		} catch (Exception e1) {
			fail("Exception unexpected.");
		}
		new Verifications() {{
			assertEquals(true, getField(cfgManager, "unsavedChanges"));
		}};
		
		/*
		 * Test:
		 * 	- call of setConfiguration with InventorySettings (SingulationControl)
		 *  - hwMgr throws exception during application of singulation control
		 * Expected:
		 *  - config is rolled back and unsavedChanges if therefore false
		 */
		
		new NonStrictExpectations() {{
			hwMgr.setSingulationControl(withInstanceOf(SingulationControl.class));
			result = new ImplementationException();
		}};
		setField(cfgManager, "unsavedChanges", false);		
		try {
			cfgManager.setConfiguration(invSettings);
			fail("Exception expected.");
		} catch (ParameterException e) { }		
		
		new Verifications() {{
			assertEquals(false, getField(cfgManager, "unsavedChanges"));			
		}};
		
		invSettings.setSingulationControl(null);
		
		/*
		 * Test:
		 *  - call of setConfiguration with InventorySettings (SelectionMask)
		 * Expected:
		 * 	- selection mask list of config contains all selection masks specified
		 *  - unsaved changes is true  
		 */
		
		setField(cfgManager, "unsavedChanges", false);
		
		invSettings.getSelectionMasks().add( 
			new SelectionMask() {{ 
				setBank(RFUtils.BANK_EPC); 
				setBitLength((short) 32); 
				setBitOffset((short) 8); 
				setMask(new byte[] { (byte) 0xaa, (byte) 0xbb }); }});
		
		invSettings.getSelectionMasks().add( 	
			new SelectionMask() {{ 
				setBank(RFUtils.BANK_TID); 
				setBitLength((short) 64); 
				setBitOffset((short) 16); 
				setMask(new byte[] { (byte) 0xcc, (byte) 0xdd }); }});
		
		cfgManager.setConfiguration(invSettings);		
		
		new Verifications() {{
			assertEquals(true, getField(cfgManager, "unsavedChanges"));			
			assertEquals(((RFConfiguration) getField(cfgManager, "config")).getInventorySettings().getSelectionMasks().size(), invSettings.getSelectionMasks().size()); 
		}};
	}

	@Test
	public void testHasUnsavedChanges(@Mocked final MainController mainController) throws ImplementationException {
		/*
		 * Test:
		 * 	- hasUnsavedChanges call
		 * Expected:
		 * 	- result returned by this method equals the value of the instance variable unsavedChanges
		 */
		final ConfigurationManager cfgMgr = new ConfigurationManager(mainController);
		setField(cfgMgr, "unsavedChanges", true);
		assertEquals(cfgMgr.hasUnsavedChanges(), (boolean) getField(cfgMgr, "unsavedChanges"));
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testGetConfiguration(@Mocked final MainController mainController, @Mocked final HardwareManager hwMgr) throws ImplementationException {
		final ConfigurationManager cfgManager = new ConfigurationManager(mainController);
		final RFConfiguration conf = new RFConfiguration();

		AntennaConfigurationList antennaConfigurationList = new AntennaConfigurationList();
		AntennaConfiguration antCfg1 = new AntennaConfiguration(); 
		antCfg1.setId((short) 1); 		
		antCfg1.setConnect(ConnectType.TRUE);
		
		AntennaConfiguration antCfg2 = new AntennaConfiguration(); 
		antCfg2.setId((short) 2); 
		antCfg2.setConnect(ConnectType.FALSE);
		
		antennaConfigurationList.getEntryList().add(antCfg1);
		antennaConfigurationList.getEntryList().add(antCfg2);
		conf.setAntennaConfigurationList(antennaConfigurationList);

		KeepAliveConfiguration keepAliveConfiguration = new KeepAliveConfiguration();
		conf.setKeepAliveConfiguration(keepAliveConfiguration);

		conf.setDeviceCapabilities(new DeviceCapabilities());
		conf.getDeviceCapabilities().setNumberOfAntennas((short) 2);
		setField(cfgManager, "config", conf);

		final AntennaPropertyList apl = new AntennaPropertyList();
		
		AntennaProperties antProps1 = new AntennaProperties();
		antProps1.setId((short)1); 
		antProps1.setConnected(true);
		
		AntennaProperties antProps2 = new AntennaProperties();
		antProps2.setId((short)2); 
		antProps2.setConnected(false);
		
		apl.getEntryList().add(antProps1);
		apl.getEntryList().add(antProps2);
		
		new NonStrictExpectations() {{
			mainController.getHardwareManager();
			result = hwMgr;
			
			hwMgr.getAntennaProperties((Map<Short, ConnectType>) any);
			result = apl;
		}};
		
		/*
		 * Test:
		 * 	- getConfiguration with config type KEEP_ALIVE_CONFIGURATION
		 * Expected:
		 * 	- result contains exactly one element which is a keep-alive configuration
		 * 
		 */
		List<Configuration> configs = cfgManager.getConfiguration(ConfigurationType.KEEP_ALIVE_CONFIGURATION, (short) 0);
		assertEquals(1, configs.size());
		assertEquals(keepAliveConfiguration.getClass(), configs.get(0).getClass());

		/*
		 * Test:
		 * 	- getConfiguration with config type ANTENNA_CONFIGURATION for antenna 1
		 * Expected:
		 * 	- result contains exactly one element which is the antenna config for antenna 1 
		 */
		configs = cfgManager.getConfiguration(ConfigurationType.ANTENNA_CONFIGURATION, (short) 1);
		assertEquals(1, configs.size());
		assertEquals(AntennaConfiguration.class, configs.get(0).getClass());
		assertEquals(antCfg1.getId(), ((AntennaConfiguration) configs.get(0)).getId());

		/*
		 * Test:
		 * 	- getConfiguration with config type ANTENNA_CONFIGURATION for antenna 2
		 * Expected:
		 * 	- result contains exactly one element which is the antenna config for antenna 2		 
		 */
		configs = cfgManager.getConfiguration(ConfigurationType.ANTENNA_CONFIGURATION, (short) 2);
		assertEquals(1, configs.size());
		assertEquals(AntennaConfiguration.class, configs.get(0).getClass());
		assertEquals(antCfg2.getId(), ((AntennaConfiguration) configs.get(0)).getId());

		/*
		 * Test:
		 * 	- getConfiguration with config type ANTENNA_CONFIGURATION for antenna 0, i.e. all available antennas
		 * Expected:
		 * 	- result contains exactly two elements which are the antenna configs for antenna 1 and 2 
		 */
		configs = cfgManager.getConfiguration(ConfigurationType.ANTENNA_CONFIGURATION, (short) 0);
		assertEquals(2, configs.size());		
		assertEquals(AntennaConfiguration.class, configs.get(0).getClass());
		assertEquals(antCfg1.getId(), ((AntennaConfiguration) configs.get(0)).getId());
		assertEquals(AntennaConfiguration.class, configs.get(1).getClass());
		assertEquals(antCfg2.getId(), ((AntennaConfiguration) configs.get(1)).getId());
		
		/*
		 * Test:
		 * 	- getConfiguration with config type ANTENNA_PROPERTIES for antenna 0, i.e. all available antennas
		 * Expected:
		 * 	- result contains exactly two elements which are the antenna properties for antenna 1 and 2 
		 */
		configs = cfgManager.getConfiguration(ConfigurationType.ANTENNA_PROPERTIES, (short) 0);
		assertEquals(2, configs.size());
		assertEquals(AntennaProperties.class, configs.get(0).getClass());
		assertEquals(AntennaProperties.class, configs.get(1).getClass());
		assertEquals(apl.getEntryList().get(0).getId(), ((AntennaProperties)configs.get(0)).getId());
		assertEquals(apl.getEntryList().get(1).getId(), ((AntennaProperties)configs.get(1)).getId());

		/*
		 * Test:
		 * 	- getConfiguration with config type ANTENNA_PROPERTIES for antenna 1
		 * Expected:
		 * 	- result contains exactly one element which are the antenna properties for antenna 1 
		 */
		configs = cfgManager.getConfiguration(ConfigurationType.ANTENNA_PROPERTIES, (short) 1);
		assertEquals(1, configs.size());
		assertEquals(AntennaProperties.class, configs.get(0).getClass());
		assertEquals(apl.getEntryList().get(0).getId(), ((AntennaProperties)configs.get(0)).getId());		
		
		/*
		 * Test:
		 * 	- getConfiguration with config type ALL for all antennas
		 * Expected:
		 * 	- result contains all configurations for all antennas 
		 */
		configs = cfgManager.getConfiguration(ConfigurationType.ALL, (short) 0);
		assertEquals(5, configs.size());
		assertEquals(AntennaConfiguration.class, configs.get(0).getClass());
		assertEquals(AntennaConfiguration.class, configs.get(1).getClass());
		assertEquals(AntennaProperties.class, configs.get(2).getClass());
		assertEquals(AntennaProperties.class, configs.get(3).getClass());
		assertEquals(keepAliveConfiguration.getClass(), configs.get(4).getClass());		
				
	}

	@Test
	public void testGetCapabilitiesByType(@Mocked final MainController mainController) throws ImplementationException, ConnectionException {
		final ConfigurationManager cfgManager = new ConfigurationManager(mainController);

		final RFConfiguration conf = new RFConfiguration();
		DeviceCapabilities devCaps = new DeviceCapabilities();
		
		RegulatoryCapabilities regCaps = new RegulatoryCapabilities();
		conf.setDeviceCapabilities(devCaps);
		conf.setRegulatoryCapabilities(regCaps);
		setField(cfgManager, "config", conf);

		List<Capabilities> caps = cfgManager.getCapabilitiesByType(CapabilityType.DEVICE_CAPABILITIES);
		assertEquals(caps.size(), 1);
		assertEquals(caps.get(0).getClass(), devCaps.getClass());

		caps = cfgManager.getCapabilitiesByType(CapabilityType.REGULATORY_CAPABILITIES);
		assertEquals(caps.size(), 1);
		assertEquals(caps.get(0).getClass(), regCaps.getClass());

		caps = cfgManager.getCapabilitiesByType(CapabilityType.ALL);
		assertEquals(caps.size(), 2);
		assertEquals(caps.get(0).getClass(), devCaps.getClass());
		assertEquals(caps.get(1).getClass(), regCaps.getClass());
	}
	
	@Test
	public void testRegionForId(@Mocked final MainController mainController) throws ImplementationException {
		/*
		 * Test
		 * 	- testRegionForId call
		 * Expected
		 * 	- returns RFRegion instance for the key "eu"
		 *  - returns null for non-existing keys 
		 */
		final ConfigurationManager cfgManager = new ConfigurationManager(mainController);
		HashMap<String,RFRegion> supportedRegions = new HashMap<>();
		supportedRegions.put("eu", new RFRegion());		
		setField(cfgManager, "supportedRegions", supportedRegions);
		
		assertEquals(supportedRegions.get("eu"), cfgManager.regionForId("eu"));
		assertTrue(cfgManager.regionForId("none") == null);
	}
	
	@Test
	public void testGetRegion(@Mocked final MainController mainController) throws ImplementationException {
		/*
		 * Test
		 * 	- getRegion call
		 * Expected
		 * 	- result returned by this method equals the return value of config.getRegion() 
		 */
		final ConfigurationManager cfgManager = new ConfigurationManager(mainController);
		RFConfiguration config = new RFConfiguration();
		config.setRegion("eu");
		setField(cfgManager, "config", config);
		
		assertEquals("eu", cfgManager.getRegion());
	}
	
	@Test
	public void testGetKeepAliveConfiguration(@Mocked final MainController mainController) throws ImplementationException {
		/*
		 * Test
		 * 	- getKeepAliveConfiguration call
		 * Expected
		 * 	- result returned by this method equals the return value of config.getKeepAliveConfiguration() 
		 */
		final ConfigurationManager cfgManager = new ConfigurationManager(mainController);
		RFConfiguration config = new RFConfiguration();
		config.setKeepAliveConfiguration(new KeepAliveConfiguration());
		setField(cfgManager, "config", config);
		
		assertEquals(config.getKeepAliveConfiguration(), cfgManager.getKeepAliveConfiguration());
	}
}
