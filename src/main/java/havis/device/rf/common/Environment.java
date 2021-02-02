package havis.device.rf.common;

import havis.device.rf.common.util.FileUtils;
import havis.device.rf.configuration.RFConfiguration;
import havis.device.rf.configuration.RFRegion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Environment {
	private static final Logger log = Logger.getLogger(Environment.class.getName());
	private static final Properties properties = new Properties();	
	private static final String FILE_NAME = "havis.device.rf.properties";
	
	public static RFConfiguration DEFAULT_CONFIG;
	public final static Map<String, RFRegion> SUPPORTED_REGIONS = new LinkedHashMap<>();

	static {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		log.log(Level.FINER, "Loading properties file {0}", FILE_NAME);
		try (InputStream propStream = classLoader.getResourceAsStream(FILE_NAME)) {
			properties.load(propStream);
		} catch (IOException e) {
			LogRecord logRec = new LogRecord(Level.SEVERE, "Initialization of environment failed: {0}");
			logRec.setThrown(e);
			logRec.setParameters(new Object[] { e });
			logRec.setLoggerName(log.getName());
			log.log(logRec);
		}
		
		log.finer("Loading default configuration file.");
		try {
			DEFAULT_CONFIG = 
				FileUtils.deserialize(classLoader.getResourceAsStream(
					properties.getProperty("havis.device.rf.common.defaultConfigFile", "havis/device/rf/config/default.json")), 
					RFConfiguration.class);
		} catch (IOException e) {
			LogRecord logRec = new LogRecord(Level.SEVERE, "Failed to load default configuration file: {0}");
			logRec.setThrown(e);
			logRec.setParameters(new Object[] { e });
			logRec.setLoggerName(log.getName());
			log.log(logRec);
		}
		
		log.finer("Loading supported regions.");
		String regionIds = properties.getProperty("havis.device.rf.common.supportedRegions", "Unspecified,EU,FCC");
		for (String regionId : regionIds.split("\\s*\\,\\s*")) {
			try {
				SUPPORTED_REGIONS.put(regionId,
						FileUtils.deserialize(classLoader.getResourceAsStream(
							properties.getProperty("havis.device.rf.common.regionPath", "havis/device/rf/region/") + regionId + ".json"), 
							RFRegion.class));
				log.log(Level.FINER, "Region {0} loaded", regionId);
			} catch (IOException e) {
				LogRecord logRec = new LogRecord(Level.SEVERE, "Failed to read region ''{0}'': {1}");
				logRec.setThrown(e);
				logRec.setParameters(new Object[] { regionId, e });
				logRec.setLoggerName(log.getName());
				log.log(logRec);
			}
		}
	}
	
	private static List<Short> parseAntennasProperty(String propId) {
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
	
	public static final String UNSPECIFIED_REGION_ID = properties.getProperty("havis.device.rf.common.unspecifiedRegionId", "Unspecified");
	public static final String DEFAULT_REGION_ID = properties.getProperty("havis.device.rf.common.defaultRegionId", "EU"); //TODO: not unspecified???
	public static final String CUSTOM_CONFIG_FILE = properties.getProperty("havis.device.rf.common.currentConfigFile", "conf/havis/device/rf/config.json");
	public static final String RESULT_FILE = properties.getProperty("havis.device.rf.common.resultFile", "conf/havis/device/rf/results.json");
	public static final String NUR_HARDWARE_MANAGER_CLASS = "havis.device.rf.nur.NurHardwareManager";
	public static final String HARDWARE_MANAGER_CLASS = properties.getProperty("havis.device.rf.common.hardwareManager.class", NUR_HARDWARE_MANAGER_CLASS);		
	public static final String SERIAL_DEVICE_PATH = properties.getProperty("havis.device.rf.common.serialDevicePath", "/dev/ttyACM0");
	public static final String BROKEN_RESULT_STORE_LOCATION = properties.getProperty("havis.device.rf.common.brokenResultStoreLocation");
	public static final String FIRMWARE_UPDATE_SCRIPT = properties.getProperty("havis.device.rf.common.firmware.update.script", "install-firmware.sh");
	public static final boolean PERSIST_RESULTS = Boolean.parseBoolean(properties.getProperty("havis.device.rf.common.persistResults", "false"));
	public static final boolean SERIALIZER_PRETTY_PRINT = Boolean.parseBoolean(properties.getProperty("havis.device.rf.common.serializerPrettyPrint", "true"));
	public static final boolean OPTIMIZED_TID_BANK_READING = Boolean.parseBoolean(properties.getProperty("havis.device.rf.common.optimizedTidBankReading", "true"));
	public static final boolean HANDLE_TRANSPONDER_EPC_CHANGE = Boolean.parseBoolean(properties.getProperty("havis.device.rf.common.handleTransponderEpcChange", "true"));
	public static final Short COMPLETE_USERBANK_WORD_COUNT = Short.valueOf(properties.getProperty("havis.device.rf.common.completeUserBankWordCount", "32"));
	public static final List<Short> HARDWARE_MANAGER_ANTENNAS = parseAntennasProperty("havis.device.rf.common.hardwareManager.antennas");
	public static final Baudrate SERIAL_DEVICE_BAUDRATE = Baudrate.valueOf(properties.getProperty("havis.device.rf.common.serialDeviceBaudrate", "BAUDRATE_1000000"));
	public final static String WS_URI = properties.getProperty("havis.device.rf.common.wsUri", "https://mica/ws/");
	public final static String RPC_TOOL = properties.getProperty("havis.device.rf.common.rpcTool", "/usr/bin/mica-rpc");

	public static boolean isNurHardware() {
		return Environment.NUR_HARDWARE_MANAGER_CLASS.equals(Environment.HARDWARE_MANAGER_CLASS);
	}

	public static boolean hasRpcTool() {
		return new File(Environment.RPC_TOOL).exists();
	}
}