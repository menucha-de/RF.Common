package havis.device.rf.common;

import havis.util.monitor.Broker;

/**
 * Abstract connector factory
 */
public abstract class Connector {

	private static Connector instance;

	/**
	 * @return the current factory
	 */
	public static Connector getFactory() {
		if (instance == null)
			throw new IllegalStateException("Connector factory has not been initialized");
		return instance;
	}

	/**
	 * @param connector
	 *            the factory to set
	 */
	public static void createFactory(Connector connector) {
		if (connector == null)
			throw new NullPointerException("connector must not be null");
		instance = connector;
	}

	/**
	 * Clear the current factory
	 */
	public static void clearFactory() {
		instance = null;
	}

	/**
	 * @return the broker for monitoring
	 */
	public abstract Broker getBroker();
}