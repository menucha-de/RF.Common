package havis.device.rf.common.test;

import havis.device.rf.RFConsumer;
import havis.device.rf.capabilities.RegulatoryCapabilities;
import havis.device.rf.common.HardwareManager;
import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.AntennaConfigurationList;
import havis.device.rf.configuration.AntennaPropertyList;
import havis.device.rf.configuration.ConnectType;
import havis.device.rf.configuration.RFRegion;
import havis.device.rf.configuration.RssiFilter;
import havis.device.rf.configuration.SingulationControl;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagDataList;
import havis.device.rf.tag.operation.TagOperation;

import java.util.List;
import java.util.Map;

public class TestHardwareManager implements HardwareManager {

	@Override
	public void setRegion(RFRegion rfcRegion,
			AntennaConfigurationList antennaConfigurationList)
			throws ParameterException, ImplementationException {
	}

	@Override
	public void setAntennaConfiguration(
			AntennaConfiguration antennaConfiguration,
			RegulatoryCapabilities regulatoryCapabilities, boolean forceTune)
			throws ParameterException, ImplementationException {
	}

	@Override
	public void openConnection() throws ConnectionException,
			ImplementationException {
	}

	@Override
	public String getRegion() {
		return null;
	}

	@Override
	public AntennaPropertyList getAntennaProperties(Map<Short, ConnectType> connectTypes)
			throws ImplementationException {
		return null;
	}

	@Override
	public TagDataList execute(List<Short> antennas, List<Filter> filter,
			List<TagOperation> operations, RFConsumer consumer) 
					throws ImplementationException, ParameterException {
		return null;
	}
	
	@Override
	public void closeConnection() throws ConnectionException {
	}

	@Override
	public String getFirmwareVersion() throws ImplementationException {
		return null;
	}

	@Override
	public void installFirmware() throws ImplementationException {

	}

	@Override
	public RssiFilter getRssiFilter() {
		return null;
	}

	@Override
	public SingulationControl getSingulationControl() {
		return null;
	}

	@Override
	public void setRssiFilter(RssiFilter rssiFilter) throws ImplementationException {
		
	}

	@Override
	public void setSingulationControl(SingulationControl singulationControl) throws ImplementationException {
		
	}

	@Override
	public int getMaxAntennas() throws ImplementationException {
		/* high number to avoid antennas to be removed from config */
		return 99;
	}

}
