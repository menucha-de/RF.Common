package havis.device.rf.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BaudrateTest {

	@Test
	public void testBaudrate() {
		
		assertEquals(Baudrate.BAUDRATE_38400.getValue(), 38400);
		assertEquals(Baudrate.BAUDRATE_115200.getValue(), 115200);
		assertEquals(Baudrate.BAUDRATE_230400.getValue(), 230400);
		assertEquals(Baudrate.BAUDRATE_500000.getValue(), 500000);
		assertEquals(Baudrate.BAUDRATE_1000000.getValue(), 1000000);
		assertEquals(Baudrate.BAUDRATE_1500000.getValue(), 1500000);
		
		
	}

}
