package havis.device.rf.common;

import havis.device.rf.RFConsumer;
import havis.device.rf.capabilities.RegulatoryCapabilities;
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

public interface HardwareManager {
	void openConnection() throws ConnectionException, ImplementationException;

	void closeConnection() throws ConnectionException;

	TagDataList execute(List<Short> antennas, List<Filter> filter,
			List<TagOperation> operations, RFConsumer consumer) 
					throws ImplementationException, ParameterException;

	String getRegion();

	void setRegion(RFRegion rfcRegion,
			AntennaConfigurationList antennaConfigurationList)
			throws ParameterException, ImplementationException;

	void setAntennaConfiguration(AntennaConfiguration antennaConfiguration,
			RegulatoryCapabilities regulatoryCapabilities, boolean forceTune)
			throws ParameterException, ImplementationException;

	AntennaPropertyList getAntennaProperties(Map<Short, ConnectType> connectTypeMap)
			throws ImplementationException;

	String getFirmwareVersion() throws ImplementationException;

	void installFirmware() throws ImplementationException;

	RssiFilter getRssiFilter();

	void setRssiFilter(RssiFilter rssiFilter) throws ImplementationException;
	
	SingulationControl getSingulationControl();

	void setSingulationControl(SingulationControl singulationControl) throws ImplementationException;

	int getMaxAntennas() throws ImplementationException;
}
