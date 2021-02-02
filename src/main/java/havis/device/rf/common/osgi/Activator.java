package havis.device.rf.common.osgi;

import havis.device.rf.common.Connector;
import havis.util.monitor.Broker;
import havis.util.monitor.Event;
import havis.util.monitor.Source;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {

	@Override
	public void start(final BundleContext context) throws Exception {
		Connector.clearFactory();
		// create connector factory
		Connector.createFactory(new Connector() {

			private Broker broker = null;
			private Lock lock = new ReentrantLock();

			@Override
			public Broker getBroker() {
				if (broker == null) {
					lock.lock();
					try {
						if (broker == null) {
							try {
								for (ServiceReference<Broker> reference : context
										.getServiceReferences(Broker.class, null)) {
									ServiceObjects<Broker> objects = context
											.getServiceObjects(reference);
									if (objects != null) {
										broker = objects.getService();
										break;
									}
								}
							} catch (InvalidSyntaxException e) {
								// ignore
							}
						}
					} finally {
						lock.unlock();
					}
				}
				if (broker == null) {
					return new Broker() {
						@Override
						public void notify(Source source, Event event) {
						}
					};
				}
				return broker;
			}
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}