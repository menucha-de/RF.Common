package havis.device.rf.common;

public enum Baudrate {
	
	BAUDRATE_38400(38400),  
	BAUDRATE_115200(115200), 
	BAUDRATE_230400(230400), 
	BAUDRATE_500000(500000),	
	BAUDRATE_1000000(1000000),
	BAUDRATE_1500000(1500000);
	
	
	private final int value;
	
	Baudrate(int value)
	{
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	
}
