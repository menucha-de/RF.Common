package havis.device.rf.common;

import havis.device.rf.RFConsumer;
import havis.device.rf.capabilities.Capabilities;
import havis.device.rf.capabilities.CapabilityType;
import havis.device.rf.common.tagsmooth.TagSmoothingHandler;
import havis.device.rf.common.util.RFUtils;
import havis.device.rf.configuration.Configuration;
import havis.device.rf.configuration.ConfigurationType;
import havis.device.rf.configuration.KeepAliveConfiguration;
import havis.device.rf.configuration.SelectionMask;
import havis.device.rf.configuration.TagSmoothingSettings;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.TagDataList;
import havis.device.rf.tag.operation.TagOperation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

class MainController {

	private static final Logger log = Logger.getLogger(MainController.class.getName());

	private static Class<?> clazz;

	private static MainController instance;
	private ConfigurationManager cfgManager;
	private HardwareManager hwManager;
	private RFConsumer consumer;
	private KeepAliveThread keepAliveThread;
	private final Lock lock = new ReentrantLock();
	private Condition connectionClosed = lock.newCondition();
	private boolean keepWaiting;
	private TagSmoothingHandler tagSmoothingHandler;

	private boolean logFilterCountWarning = true;

	static void init() {
		if (clazz == null) {
			try {
				String name = Environment.HARDWARE_MANAGER_CLASS;
				log.log(Level.FINER, "Loading hardware manager instance class {0}", name);
				clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
			} catch (ClassNotFoundException e) {
				LogRecord logRec = new LogRecord(Level.SEVERE, "Initialization failed: {0}");
				logRec.setThrown(e);
				logRec.setParameters(new Object[] { e });
				logRec.setLoggerName(log.getName());
				log.log(logRec);
			}
		}
	}

	static void dispose() {
		if (instance != null && instance.hwManager != null) {
			try {
				instance.hwManager.closeConnection();
			} catch (ConnectionException e) {
				// ignore
			}
		}
		clazz = null;
		instance = null;
	}

	protected static synchronized MainController getInstance() throws ImplementationException {
		if (instance == null) {
			init();
			instance = new MainController();
		}
		return instance;
	}

	private MainController() throws ImplementationException {
		super();

		try {
			log.log(Level.FINER, "Creating new hardware manager instance of type {0}", clazz.getName());
			this.hwManager = (HardwareManager) clazz.newInstance();

			this.hwManager.openConnection();

			/* Build configuration manager */
			this.cfgManager = new ConfigurationManager(this);
			this.cfgManager.loadConfiguration();

		} catch (Exception e) {
			LogRecord logRec = new LogRecord(Level.SEVERE, "Failed to instantiate hardware manager: {0}");
			logRec.setThrown(e);
			logRec.setParameters(new Object[] { e });
			logRec.setLoggerName(log.getName());
			log.log(logRec);
			throw new ImplementationException(e);
		}
	}

	void openConnection(RFConsumer consumer, int timeout) throws ConnectionException, ImplementationException {
		log.entering(this.getClass().getName(), "openConnection", new Object[] { consumer, timeout, Thread.currentThread().getName() });

		lock.lock();
		try {

			if (this.consumer != null) {
				log.log(Level.FINER, "Signaling current consumer: {0} (Thread: {1})", new Object[] { this.consumer, Thread.currentThread().getName() });
				this.consumer.connectionAttempted();
				log.log(Level.FINER, "Signal to current consumer sent, current consumer is now {0} (Thread: {1})", new Object[] { this.consumer,
						Thread.currentThread().getName() });

				if (this.consumer != null)
					try {
						log.log(Level.FINER, "Going into waiting state, current consumer is still {0} (Thread: {1})", new Object[] { this.consumer,
								Thread.currentThread().getName() });

						keepWaiting = true;
						Date waitUntil = new Date(new Date().getTime() + timeout);
						while (keepWaiting)
							if (!connectionClosed.awaitUntil(waitUntil)) {
								log.log(Level.FINER, "Timeout reached with no response from consumer: {0} (Thread: {1})", new Object[] { this.consumer,
										Thread.currentThread().getName() });
								throw new ConnectionException("Timeout reached.");
							}
					} catch (InterruptedException e) {
						throw new ConnectionException("Timeout interrupted.");
					}
			}
			log.log(Level.FINER, "Replacing consumer with {0} (Thread: {1})", new Object[] { consumer, Thread.currentThread().getName() });

			this.consumer = consumer;
			this.logFilterCountWarning = true;

			this.startKeepAliveThread(consumer, this.cfgManager.getKeepAliveConfiguration());
		} finally {
			lock.unlock();
		}

	}

	void closeConnection() {
		lock.lock();
		try {
			this.stopKeepAliveThread();
			this.consumer = null;
			keepWaiting = false;
			connectionClosed.signal();
		} finally {
			lock.unlock();
		}
	}

	List<Configuration> getConfiguration(ConfigurationType type, short antennaID, short gpiPort, short gpoPort) throws ImplementationException {
		lock.lock();
		try {
			return this.cfgManager.getConfiguration(type, antennaID);
		} finally {
			lock.unlock();
		}
	}

	void setConfiguration(List<Configuration> configurations) throws ImplementationException, ParameterException {
		lock.lock();
		try {
			for (Configuration cfg : configurations)
				this.cfgManager.setConfiguration(cfg);

			if (this.cfgManager.hasUnsavedChanges())
				this.cfgManager.saveConfig();
		} finally {
			lock.unlock();
		}
	}

	void resetConfiguration() throws ImplementationException, ParameterException {
		lock.lock();
		try {
			this.cfgManager.resetConfig();
			this.cfgManager.loadConfiguration();
		} finally {
			lock.unlock();
		}
	}

	List<TagData> execute(List<Short> antennas, List<Filter> filters, List<TagOperation> operations) throws ImplementationException, ParameterException {
		lock.lock();
		try {
			if ((filters == null || filters.isEmpty())) {
				filters = new ArrayList<>();
				for (SelectionMask sMask : this.cfgManager.getDefaultFilters())
					filters.add(RFUtils.createFilter(sMask));
			}

			if (filters.size() > 6) {
				if (logFilterCountWarning) {
					log.warning("Maximum number of filters is 6 but " + filters.size() + " filters have been specified. Aborting execution.");
					logFilterCountWarning = false;
				}
				return new ArrayList<>();
			}

			TagDataList tdl = this.hwManager.execute(antennas, filters, operations, consumer);
			if (tagSmoothingHandler != null && tagSmoothingHandler.isEnabled()) {
				tagSmoothingHandler.process(tdl);
				return tagSmoothingHandler.getResultList();
			}
			return tdl.getEntryList();

		} finally {
			lock.unlock();
		}
	}

	List<Capabilities> getCapabilities(CapabilityType type) throws ConnectionException, ImplementationException {
		lock.lock();
		try {
			return this.cfgManager.getCapabilitiesByType(type);
		} finally {
			lock.unlock();
		}
	}

	List<String> getSupportedRegions() {
		lock.lock();
		try {
			return new ArrayList<>(this.cfgManager.getSupportedRegions());
		} finally {
			lock.unlock();
		}

	}

	String getRegion() {
		return this.cfgManager.getRegion();
	}

	void setRegion(String id) throws ParameterException, ImplementationException {
		lock.lock();
		try {
			this.cfgManager.setRegion(id);
		} finally {
			lock.unlock();
		}
	}

	HardwareManager getHardwareManager() {
		return this.hwManager;
	}

	void installFirmware() throws ImplementationException {
		this.hwManager.installFirmware();
	}

	void restartKeepAliveThread(KeepAliveConfiguration config) {
		this.startKeepAliveThread(this.consumer, config);
	}

	private void startKeepAliveThread(RFConsumer consumer, KeepAliveConfiguration config) {
		this.stopKeepAliveThread();

		if (log.isLoggable(Level.FINER))
			log.entering(this.getClass().getName(), "startKeepAliveThread", new Object[] { consumer, RFUtils.serialize(config) });

		if (config != null && consumer != null && config.isEnable() && config.getInterval() > 0) {

			this.keepAliveThread = new KeepAliveThread(consumer, config.getInterval());
			new Thread(this.keepAliveThread).start();
		}

		log.exiting(this.getClass().getName(), "startKeepAliveThread");
	}

	private void stopKeepAliveThread() {
		if (this.keepAliveThread != null) {
			this.keepAliveThread.stop();
		}

		this.keepAliveThread = null;
	}

	public void updateTagSmoothingHandler(TagSmoothingSettings settings) throws ParameterException {
		tagSmoothingHandler = new TagSmoothingHandler(settings);
	}

}