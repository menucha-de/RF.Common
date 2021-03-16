package havis.device.rf.common;

import havis.device.rf.RFConsumer;
import havis.device.rf.RFDevice;
import havis.device.rf.capabilities.Capabilities;
import havis.device.rf.capabilities.CapabilityType;
import havis.device.rf.common.util.RFUtils;
import havis.device.rf.configuration.Configuration;
import havis.device.rf.configuration.ConfigurationType;
import havis.device.rf.exception.CommunicationException;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.TagOperation;
import havis.util.monitor.DeviceCapabilities;
import havis.util.monitor.ReaderSource;
import havis.util.monitor.TagEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements {@link RFDevice}.
 */

public class CommunicationHandler implements RFDevice {
	private static final Logger log = Logger.getLogger(RFDevice.class.getName());

	private MainController mainController;
	private static final String ERR_MSG_NO_CONN = "No open connection found. Please call openConnection first.";

	private ReaderSource readerSource = new ReaderSource() {
		@Override
		public void setConfiguration(List<havis.util.monitor.Configuration> configuration) {
		}

		@Override
		public List<havis.util.monitor.Configuration> getConfiguration(
				havis.util.monitor.ConfigurationType type, short antennaId) {
			return null;
		}

		@Override
		public List<havis.util.monitor.Capabilities> getCapabilities(
				havis.util.monitor.CapabilityType type) {
			List<havis.util.monitor.Capabilities> ret = new ArrayList<>();

			if (type == havis.util.monitor.CapabilityType.ALL
					|| type == havis.util.monitor.CapabilityType.DEVICE_CAPABILITIES)
				// TODO: add firmware
				ret.add(new DeviceCapabilities("Built-In",
						"Menucha Team", "RF-R300", null));

			return ret;
		}
	};

	public CommunicationHandler() {
		log.log(Level.FINER, "{0} instantiated.", this.getClass().getName());
		MainController.init();
	}

	@Override
	public List<Capabilities> getCapabilities(CapabilityType type) throws ConnectionException,
			ImplementationException {
		log.entering(this.getClass().getName(), "getCapabilities", type);

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);
		List<Capabilities> result = this.mainController.getCapabilities(type);

		if (log.isLoggable(Level.FINER))
			log.exiting(this.getClass().getName(), "getCapabilities",
					RFUtils.serializeList(result, Capabilities.class));

		return result;
	}

	@Override
	public List<Configuration> getConfiguration(ConfigurationType type,

	short antennaID, short gpiPort, short gpoPort) throws ConnectionException,
			ImplementationException {

		log.entering(this.getClass().getName(), "getConfiguration", new Object[] { type, antennaID,
				gpiPort, gpoPort });

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);

		List<Configuration> result = this.mainController.getConfiguration(type, antennaID, gpiPort,
				gpoPort);

		if (log.isLoggable(Level.FINER))
			log.exiting(this.getClass().getName(), "getConfiguration",
					RFUtils.serializeList(result, Configuration.class));

		return result;

	}

	@Override
	public void setConfiguration(List<Configuration> configuration) throws ImplementationException,
			ConnectionException, ParameterException {
		if (log.isLoggable(Level.FINER))
			log.entering(this.getClass().getName(), "setConfiguration",
					RFUtils.serializeList(configuration, Configuration.class));

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);

		this.mainController.setConfiguration(configuration);
		log.exiting(this.getClass().getName(), "setConfiguration");
	}

	@Override
	public void resetConfiguration() throws ImplementationException, ConnectionException,
			ParameterException {
		log.entering(this.getClass().getName(), "resetConfiguration");

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);

		mainController.resetConfiguration();
		log.exiting(this.getClass().getName(), "resetConfiguration");
	}

	@Override
	public void openConnection(RFConsumer consumer, int timeout) throws ConnectionException,
			ImplementationException {
		log.entering(this.getClass().getName(), "openConnection",
				new Object[] { consumer, timeout });

		MainController mainController = MainController.getInstance();
		mainController.openConnection(consumer, timeout);
		this.mainController = mainController;

		log.exiting(this.getClass().getName(), "openConnection");
	}

	@Override
	public void closeConnection() throws ConnectionException {
		log.entering(this.getClass().getName(), "closeConnection");

		if (mainController != null) {
			this.mainController.closeConnection();
			this.mainController = null;
		}

		log.exiting(this.getClass().getName(), "closeConnection");
	}

	@Override
	public List<TagData> execute(List<Short> antennas, List<Filter> filters,
			List<TagOperation> operations) throws ConnectionException, CommunicationException,
			ParameterException, ImplementationException {

		if (log.isLoggable(Level.FINER))
			log.entering(
					this.getClass().getName(),
					"execute",
					new Object[] { RFUtils.serializeList(antennas, Short.class),
							RFUtils.serializeList(filters, Filter.class),
							RFUtils.serializeList(operations, TagOperation.class) });

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);

		List<TagData> result = this.mainController.execute(antennas, filters, operations);

		Date currentTime = new Date();
		for (TagData tag : result)
			Connector
					.getFactory()
					.getBroker()
					.notify(this.readerSource,
							new TagEvent(currentTime, tag.getEpc(), tag.getAntennaID(), tag
									.getRssi()));

		if (log.isLoggable(Level.FINER))
			log.exiting(this.getClass().getName(), "execute",
					RFUtils.serializeList(result, TagData.class));

		return result;
	}

	@Override
	public List<String> getSupportedRegions() throws ConnectionException {
		log.entering(this.getClass().getName(), "getSupportedRegions");

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);

		List<String> result = this.mainController.getSupportedRegions();

		if (log.isLoggable(Level.FINER))
			log.exiting(this.getClass().getName(), "getSupportedRegions",
					RFUtils.serializeList(result, String.class));

		return result;
	}

	@Override
	public void setRegion(String id) throws ParameterException, ImplementationException,
			ConnectionException {

		log.entering(this.getClass().getName(), "setRegion", id);

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);

		this.mainController.setRegion(id);

		log.exiting(this.getClass().getName(), "setRegion");
	}

	@Override
	public String getRegion() throws ConnectionException {
		log.entering(this.getClass().getName(), "getRegion");

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);

		String result = this.mainController.getRegion();
		log.exiting(this.getClass().getName(), "getRegion", result);
		return result;
	}

	@Override
	public void installFirmware() throws ImplementationException, ConnectionException {
		log.entering(this.getClass().getName(), "installFirmware");

		if (this.mainController == null)
			throw new ConnectionException(ERR_MSG_NO_CONN);

		this.mainController.installFirmware();
		log.exiting(this.getClass().getName(), "installFirmware");

	}

	public static void dispose() {
		MainController.dispose();
	}
}
