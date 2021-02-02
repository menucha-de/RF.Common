package havis.device.rf.common;

import havis.device.rf.capabilities.Capabilities;
import havis.device.rf.capabilities.CapabilityType;
import havis.device.rf.capabilities.DeviceCapabilities;
import havis.device.rf.common.util.FileUtils;
import havis.device.rf.common.util.PropertyException;
import havis.device.rf.common.util.PropertyUtil;
import havis.device.rf.common.util.RFUtils;
import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.AntennaProperties;
import havis.device.rf.configuration.AntennaPropertyList;
import havis.device.rf.configuration.Configuration;
import havis.device.rf.configuration.ConfigurationType;
import havis.device.rf.configuration.ConnectType;
import havis.device.rf.configuration.InventorySettings;
import havis.device.rf.configuration.KeepAliveConfiguration;
import havis.device.rf.configuration.RFConfiguration;
import havis.device.rf.configuration.RFRegion;
import havis.device.rf.configuration.SelectionMask;
import havis.device.rf.configuration.TagSmoothingSettings;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This class gives access to configuration and region settings. After building
 * an instance of the class the active region will be the 'unspecified' one if
 * not defined differently by the property
 * havis.device.rf.daemon.defaultRegionId.
 */

class ConfigurationManager {
	private static final String REGION_UUID = "2a4c7dea-0ef5-4813-aa35-c49a7a0b4ec1";

	private static final Logger log = Logger.getLogger(ConfigurationManager.class.getName());

	private Map<String, RFRegion> supportedRegions;
	private final String customConfigFile;
	private RFConfiguration config;
	private boolean unsavedChanges;
	private MainController mainController;

	/**
	 * Creates a new instance.
	 * 
	 * @param mainController
	 *            A reference to the MainController instance.
	 * @throws ImplementationException
	 */
	ConfigurationManager(MainController mainController) throws ImplementationException {
		super();

		log.finer("Initializing configuration manager.");

		this.customConfigFile = Environment.CUSTOM_CONFIG_FILE;
		this.mainController = mainController;
		this.supportedRegions = Environment.SUPPORTED_REGIONS;

		log.finer("Configuration manager initialized.");
	}
	
	private String getRegionFromStore() {
		try {
			String region = PropertyUtil.getProperty(REGION_UUID);
			if (region.isEmpty()) {
				return Environment.UNSPECIFIED_REGION_ID;
			}
			return region;
		} catch (PropertyException e) {
			return Environment.UNSPECIFIED_REGION_ID;
		}
	}

	/**
	 * Loads the custom configuration file if exists. If no custom configuration
	 * file exists, the default configuration is loaded.
	 * 
	 * @throws ImplementationException
	 * @throws IOException
	 * 
	 */
	void loadConfiguration() throws ImplementationException, ParameterException {
		log.entering(getClass().getName(), "loadConfiguration");
		File customConfigFile = new File(this.customConfigFile);
		RFConfiguration config = null;

		if (customConfigFile.exists()) {
			log.finer("Loading custom configuration.");

			try (InputStream cfgStream = new FileInputStream(customConfigFile)) {
				config = FileUtils.deserialize(cfgStream, RFConfiguration.class);
			} catch (IOException ex) {
				resetConfig();
				throw new ImplementationException("Failed to load custom configuration.", ex);
			}
		} else
			config = Environment.DEFAULT_CONFIG.clone();

		this.config = config;
		if (Environment.isNurHardware() && Environment.hasRpcTool())
			this.config.setRegion(getRegionFromStore());

		/*
		 * if no custom config file exists...
		 */
		if (!customConfigFile.exists()) {

			/* acquire the maximum number of antennas from the hardware manager. */
			int antennaCount = this.mainController.getHardwareManager().getMaxAntennas();

			/* the antenna configuration list */
			List<AntennaConfiguration> antCfgList = this.config.getAntennaConfigurationList().getEntryList();

			/* get the number of antenna entries from configuration */
			int antennasInConfig = antCfgList.size();

			/*
			 * if configuration contains more antenna entries as detected on
			 * hardware level, remove redundant entries
			 */
			if (antennasInConfig > antennaCount) {
				while (antCfgList.size() > antennaCount)
					antCfgList.remove(antCfgList.size() - 1);
				this.saveConfig();
			}

			// /* if configuration contains to few entries, add the missing
			// entries */
			// else if (antennasInConfig < antennaCount) {
			// while (antCfgList.size() < antennaCount) {
			// AntennaConfiguration newCfg = new AntennaConfiguration();
			// newCfg.setChannelIndex((short)0);
			// newCfg.setHopTableID((short)0);
			// newCfg.setTransmitPower((short)0);
			// newCfg.setReceiveSensitivity((short)0);
			// newCfg.setConnect(ConnectType.AUTO);
			// newCfg.setId((short)(antCfgList.size()+1));
			// antCfgList.add(newCfg);
			// }
			// this.saveConfig();
			// }

			/*
			 * Set the region to its initial value as specified in the default
			 * config (presumably 'unspecified') which resets settings such as
			 * transmit power to 0 and applies those changes to the hardware
			 * manager.
			 */
			try {
				setRegion(this.config.getRegion());
			} catch (ParameterException e) {
				throw new ImplementationException(e);
			}
		}
		/*
		 * apply the regional settings as specified in the custom config file to
		 * the hardware manager
		 */
		else
			this.mainController.getHardwareManager().setRegion(this.regionForId(this.config.getRegion()), this.config.getAntennaConfigurationList());

		if (config.getInventorySettings() == null)
			config.setInventorySettings(new InventorySettings());

		if (config.getInventorySettings().getRssiFilter() == null)
			config.getInventorySettings().setRssiFilter(this.mainController.getHardwareManager().getRssiFilter());
		else
			this.mainController.getHardwareManager().setRssiFilter(config.getInventorySettings().getRssiFilter());

		if (config.getInventorySettings().getSingulationControl() == null)
			config.getInventorySettings().setSingulationControl(this.mainController.getHardwareManager().getSingulationControl());
		else
			this.mainController.getHardwareManager().setSingulationControl(config.getInventorySettings().getSingulationControl());

		if (config.getInventorySettings().getTagSmoothing() == null)
			config.getInventorySettings().setTagSmoothing(new TagSmoothingSettings());

		log.exiting(getClass().getName(), "loadConfiguration");
	}

	/**
	 * Deletes the custom configuration and restores the default configuration.
	 * 
	 * @throws ImplementationException
	 */
	void resetConfig() throws ImplementationException, ParameterException {
		log.entering(getClass().getName(), "resetConfig");
		new File(this.customConfigFile).delete();
		if (Environment.isNurHardware() && Environment.hasRpcTool()) {
			try {
				log.info("Resetting region in persistent key storage");
				PropertyUtil.setProperty(REGION_UUID, Environment.UNSPECIFIED_REGION_ID);
			} catch (PropertyException e) {
				throw new ImplementationException(e);
			}
		}
		log.exiting(getClass().getName(), "resetConfig");
	}

	/**
	 * Retrieves the requested region by ID and applies its regulatory
	 * capabilities to the current config.
	 * 
	 * @param id
	 *            a region ID
	 * @throws ParameterException
	 * @throws ImplementationException
	 */
	void setRegion(String id) throws ParameterException, ImplementationException {
		log.entering(getClass().getName(), "setRegion", id);

		String current = getRegion();
		if (Environment.isNurHardware() && Environment.hasRpcTool() && !current.equals(Environment.UNSPECIFIED_REGION_ID) && !current.equals(id)) {
			throw new ParameterException("Region already set to '" + current + "'. Changing is not permitted.");
		}
		
		RFRegion region = this.supportedRegions.get(id);

		if (region == null)
			throw new ParameterException("Unsupported region '" + id + "'");

		this.regionChanged(region);

		log.exiting(getClass().getName(), "setRegion");
	}

	/**
	 * Method that is called once the region has been successfully changed. It
	 * resets all indices in the antenna configuration to avoid them referencing
	 * non-existent entries in the region settings. Afterwards the changes are
	 * persisted in the current configuration file.
	 * 
	 * @throws ImplementationException
	 */
	private void regionChanged(RFRegion newRegion) throws ImplementationException, ParameterException {
		log.entering(getClass().getName(), "regionChanged", newRegion);

		// reset antenna configs ( transmit power, channel index, hopping table
		// )
		for (AntennaConfiguration aCfg : this.config.getAntennaConfigurationList().getEntryList()) {
			aCfg.setTransmitPower((short) 0);
			aCfg.setChannelIndex((short) 0);
			aCfg.setHopTableID((short) 0);

		}
		this.config.setRegulatoryCapabilities(newRegion.getRegulatoryCapabilities());
		this.config.setRegion(newRegion.getId());
		this.mainController.getHardwareManager().setRegion(newRegion, this.config.getAntennaConfigurationList());
		this.saveConfig();

		if (Environment.isNurHardware() && Environment.hasRpcTool()) {
			try {
				log.info("Saving region to persistent key storage");
				PropertyUtil.setProperty(REGION_UUID, newRegion.getId());
			} catch (PropertyException e) {
				throw new ImplementationException(e);
			}
		}
		log.exiting(getClass().getName(), "regionChanged");
	}

	/**
	 * Persists the current configuration object. If no current configuration
	 * file exists, a new one will be created.
	 * 
	 * @throws ImplementationException
	 */
	void saveConfig() throws ImplementationException {
		log.entering(getClass().getName(), "saveConfig");
		try {
			File file = new File(this.customConfigFile);
			if (file.exists()) {
				log.finer("Saving custom configuration.");
			} else {
				log.finer("Creating custom configuration file.");
				Files.createDirectories(file.toPath().getParent(), new FileAttribute<?>[] {});
			}

			File tmpFile = File.createTempFile(this.customConfigFile, ".tmp", file.getParentFile());
			FileUtils.serialize(tmpFile, this.config);
			Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);

			this.unsavedChanges = false;
		} catch (IOException e) {
			throw new ImplementationException(e.toString());
		}
		log.exiting(getClass().getName(), "saveConfig");
	}

	/**
	 * Returns the supported region IDs.
	 * 
	 * @return a set of strings
	 */
	Set<String> getSupportedRegions() {
		return this.supportedRegions.keySet();
	}

	/**
	 * Sets an object of a sub class of Configuation. The method detects the
	 * type of the object in question and adds it appropriately to the config
	 * structure. If the given object is of type AntennaConfiguration it will
	 * replace the AntennaConfigutation object in the list having the same ID or
	 * will be appended to the list if no such item exists. If the object is of
	 * type KeepAliveConfiguration the current KeepAliveConfiguration object
	 * will be replaced.
	 * 
	 * @param config
	 *            an object of class or sub class of Configuration
	 */
	void setConfiguration(Configuration config) throws ImplementationException, ParameterException {
		log.entering(getClass().getName(), "setConfigObject", config);
		if (config instanceof AntennaConfiguration) {
			AntennaConfiguration newCnf = (AntennaConfiguration) config;
			List<AntennaConfiguration> aCnfList = this.config.getAntennaConfigurationList().getEntryList();

			boolean antennaFound = false;
			for (int i = 0; i < aCnfList.size(); i++) {
				if (aCnfList.get(i).getId() == newCnf.getId() || newCnf.getId() == 0) {

					/* create a copy of the antenna configuration */
					AntennaConfiguration aCnf = aCnfList.get(i).clone();

					/*
					 * change the properties of the copy depending on which
					 * properties are set in the new config
					 */
					if (newCnf.getChannelIndex() != null)
						aCnf.setChannelIndex(newCnf.getChannelIndex());
					if (newCnf.getConnect() != null)
						aCnf.setConnect(newCnf.getConnect());
					if (newCnf.getHopTableID() != null)
						aCnf.setHopTableID(newCnf.getHopTableID());
					if (newCnf.getReceiveSensitivity() != null)
						aCnf.setReceiveSensitivity(newCnf.getReceiveSensitivity());
					if (newCnf.getTransmitPower() != null)
						aCnf.setTransmitPower(newCnf.getTransmitPower());

					/* set the antenna found flag */
					antennaFound = true;

					/*
					 * try to apply the config change to the hardware and
					 * replace the original antenna config object with the
					 * modified copy on success or throw an exception on failure
					 * leaving the original config object unchanged
					 */
					try {
						this.mainController.getHardwareManager().setAntennaConfiguration(aCnf, this.config.getRegulatoryCapabilities(), false);
						aCnfList.set(i, aCnf);
						if (log.isLoggable(Level.FINER))
							log.log(Level.FINER, "Antenna configuration applied: {0}", RFUtils.serialize(aCnf));

					} catch (Exception ex) {
						if (log.isLoggable(Level.WARNING)) {
							LogRecord logRec = new LogRecord(Level.WARNING, "Failed to apply antenna configuration: {0}");
							logRec.setThrown(ex);
							logRec.setParameters(new Object[] { RFUtils.serialize(aCnf) });
							logRec.setLoggerName(log.getName());
							log.log(logRec);
						}
						throw new ParameterException("Failed to apply configuration for antenna: " + aCnf.getId());
					}
				}
			}

			if (!antennaFound)
				throw new ParameterException("No antenna with ID " + newCnf.getId() + " exists.");

			unsavedChanges = true;
		}

		else if (config instanceof AntennaProperties) {
			AntennaProperties aProps = (AntennaProperties) config;

			List<AntennaConfiguration> aCnfList = this.config.getAntennaConfigurationList().getEntryList();
			boolean antennaFound = false;
			for (int i = 0; i < aCnfList.size(); i++) {
				if (aCnfList.get(i).getId() == aProps.getId() || aProps.getId() == 0) {
					if (aProps.isConnected())
						aCnfList.get(i).setConnect(ConnectType.TRUE);
					else
						aCnfList.get(i).setConnect(ConnectType.FALSE);
					antennaFound = true;
					this.mainController.getHardwareManager().setAntennaConfiguration(aCnfList.get(i), this.config.getRegulatoryCapabilities(), false);
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "Applied connect type {0} of antenna {1}", new Object[] { aCnfList.get(i).getConnect(), aCnfList.get(i).getId() });
				}
			}

			if (!antennaFound)
				throw new ParameterException("No antenna with ID " + aProps.getId() + " exists.");

			unsavedChanges = true;

		}

		else if (config instanceof InventorySettings) {
			InventorySettings newConf = ((InventorySettings) config).clone();

			if (newConf.getRssiFilter() != null) {
				try {
					this.mainController.getHardwareManager().setRssiFilter(newConf.getRssiFilter());
					this.config.getInventorySettings().setRssiFilter(newConf.getRssiFilter());
					unsavedChanges = true;
				} catch (Exception ex) {
					if (log.isLoggable(Level.WARNING)) {
						LogRecord logRec = new LogRecord(Level.WARNING, "Failed to apply RSSI filter settings: {0}");
						logRec.setThrown(ex);
						logRec.setParameters(new Object[] { RFUtils.serialize(newConf.getRssiFilter()) });
						logRec.setLoggerName(log.getName());
						log.log(logRec);
					}
					throw new ParameterException("Failed to apply RSSI filter settings.");
				}

				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "Applied RSSI filter settings: {0}",
							new Object[] { RFUtils.serialize(this.config.getInventorySettings().getRssiFilter()) });
			}

			if (newConf.getSingulationControl() != null) {
				try {
					this.mainController.getHardwareManager().setSingulationControl(newConf.getSingulationControl());
					this.config.getInventorySettings().setSingulationControl(newConf.getSingulationControl());
					unsavedChanges = true;
				} catch (Exception ex) {
					if (log.isLoggable(Level.WARNING)) {
						LogRecord logRec = new LogRecord(Level.WARNING, "Failed to apply singulation control settings: {0}");
						logRec.setThrown(ex);
						logRec.setParameters(new Object[] { RFUtils.serialize(newConf.getSingulationControl()) });
						logRec.setLoggerName(log.getName());
						log.log(logRec);
					}
					throw new ParameterException("Failed to apply singulation control settings.");
				}

				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "Applied singulation control settings: {0}",
							new Object[] { RFUtils.serialize(this.config.getInventorySettings().getSingulationControl()) });
			}

			if (newConf.getSelectionMasks() != null) {
				this.config.getInventorySettings().getSelectionMasks().clear();
				this.config.getInventorySettings().getSelectionMasks().addAll(newConf.getSelectionMasks());
				unsavedChanges = true;
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "Applied selection mask list: {0}",
							new Object[] { RFUtils.serializeList(this.config.getInventorySettings().getSelectionMasks(), SelectionMask.class) });
			}

			if (newConf.getTagSmoothing() != null) {
				try {
					this.mainController.updateTagSmoothingHandler(newConf.getTagSmoothing());
					this.config.getInventorySettings().setTagSmoothing(newConf.getTagSmoothing());
					unsavedChanges = true;
				} catch (Exception ex) {
					if (log.isLoggable(Level.WARNING)) {
						LogRecord logRec = new LogRecord(Level.WARNING, "Failed to apply tag smoothing settings: {0}");
						logRec.setThrown(ex);
						logRec.setParameters(new Object[] { RFUtils.serialize(newConf.getTagSmoothing()) });
						logRec.setLoggerName(log.getName());
						log.log(logRec);
					}

					if (ex instanceof ParameterException)
						throw new ParameterException(ex.getMessage());
					throw new ImplementationException("Failed to apply tag smoothing settings due to an unknown error");
				}

				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "Applied tag smoothing settings: {0}",
							new Object[] { RFUtils.serialize(this.config.getInventorySettings().getTagSmoothing()) });

			}
		}

		else if (config instanceof KeepAliveConfiguration) {
			KeepAliveConfiguration kaConfig = (KeepAliveConfiguration) config;
			if (kaConfig.getInterval() <= 0)
				throw new ParameterException("Keep-alive interval must be greater than zero.");

			this.config.setKeepAliveConfiguration(kaConfig.clone());
			unsavedChanges = true;

			this.mainController.restartKeepAliveThread((KeepAliveConfiguration) config);

			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "Keep-alive configuration applied: {0}", RFUtils.serialize(this.config.getKeepAliveConfiguration()));
		}

		log.exiting(getClass().getName(), "setConfigObject");
	}

	/**
	 * Indicates whether the configuration structure has been saved or has
	 * pending unsaved changes.
	 * 
	 * @return true if configuration structure unsaved, false otherwise
	 */
	boolean hasUnsavedChanges() {
		return this.unsavedChanges;
	}

	/**
	 * Gets a list of configurations from the structure based on a given
	 * configuration type. If this type is ANTENNA_CONFIGURATION or
	 * ANTENNA_PROPERTIES only the antenna data is returned for the antenna with
	 * the given antennaID. If antennaID is 0, then the data for all antennas is
	 * returned.
	 * 
	 * @param type
	 *            a value of the ConfigurationType enumeration
	 * @param antennaId
	 *            an antenna ID or 0 for getting all antennas
	 * @return a list of Configuration objects
	 * 
	 * @throws ImplementationException
	 */
	List<Configuration> getConfiguration(ConfigurationType type, short antennaId) throws ImplementationException {
		log.entering(getClass().getName(), "getConfigrationsByTypeAndId", new Object[] { type, antennaId });
		List<Configuration> result = new ArrayList<>();

		switch (type) {

		case ALL:
		case ANTENNA_CONFIGURATION:
			for (AntennaConfiguration aCfg : this.config.getAntennaConfigurationList().getEntryList())
				if (antennaId == 0 || antennaId == aCfg.getId())
					result.add(aCfg.clone());

			if (type != ConfigurationType.ALL)
				break;

		case ANTENNA_PROPERTIES:
			Map<Short, ConnectType> connectTypeMap = new HashMap<>();
			for (AntennaConfiguration aCfg : this.config.getAntennaConfigurationList().getEntryList())
				connectTypeMap.put(aCfg.getId(), aCfg.getConnect());

			AntennaPropertyList apl = this.mainController.getHardwareManager().getAntennaProperties(connectTypeMap);
			apl.setEntryList(apl.getEntryList());

			for (AntennaProperties aProps : apl.getEntryList())
				if (antennaId == 0 || antennaId == aProps.getId())
					result.add(aProps.clone());

			if (type != ConfigurationType.ALL)
				break;

		case KEEP_ALIVE_CONFIGURATION:
			result.add(this.config.getKeepAliveConfiguration().clone());
			break;

		case INVENTORY_SETTINGS:
			if (config.getInventorySettings() != null)
				result.add(this.config.getInventorySettings().clone());
		}

		if (log.isLoggable(Level.FINER))
			log.exiting(getClass().getName(), "getConfigrationsByTypeAndId", RFUtils.serializeList(result, Configuration.class));

		return result;
	}

	/**
	 * Gets a list of capabilities from the structure based on the given
	 * capability type.
	 * 
	 * @param type
	 *            a value of the CapabilityType enumeration
	 * @return a list a Capability objects
	 * @throws ConnectionException
	 */
	List<Capabilities> getCapabilitiesByType(CapabilityType type) throws ConnectionException, ImplementationException {

		log.entering(getClass().getName(), "getCapabilitiesByType", type);

		List<Capabilities> result = new ArrayList<>();

		switch (type) {
		case ALL:
		case DEVICE_CAPABILITIES:
			DeviceCapabilities devCaps = this.config.getDeviceCapabilities();
			devCaps.setFirmware(this.mainController.getHardwareManager().getFirmwareVersion());
			result.add(devCaps.clone());
			if (type != CapabilityType.ALL)
				break;

		case REGULATORY_CAPABILITIES:
			result.add(this.config.getRegulatoryCapabilities().clone());
		}

		if (log.isLoggable(Level.FINER))
			log.exiting(getClass().getName(), "getCapabilitiesByType", RFUtils.serializeList(result, Capabilities.class));

		return result;
	}

	RFRegion regionForId(String regionId) {
		return this.supportedRegions.get(regionId);
	}

	String getRegion() {
		return this.config.getRegion();
	}

	KeepAliveConfiguration getKeepAliveConfiguration() {
		return this.config.getKeepAliveConfiguration();
	}

	List<SelectionMask> getDefaultFilters() {
		return this.config.getInventorySettings().getSelectionMasks();
	}
}
