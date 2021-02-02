package havis.device.rf.common.util;

import static mockit.Deencapsulation.getField;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.result.ReadResult;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

public class JsonSerializerTest {

	@Test
	public void testJsonSerializer() {
		JsonSerializer jsonizer = new JsonSerializer(ReadResult.class);
		assertEquals(ReadResult.class, getField(jsonizer, "clazz"));
	}

	@Test
	public void testSetPrettyPrint() {
		assert (true);
	}

	@Test
	public void testSerializeDeserialize() {

		Random rand = new Random();

		final byte[] data = new byte[8];
		final byte[] mask = new byte[8];
		rand.nextBytes(data);
		rand.nextBytes(mask);

		Filter testInstance = new Filter();
		testInstance.setBank(RFUtils.BANK_EPC);
		testInstance.setData(data);
		testInstance.setMask(mask);
		testInstance.setBitLength((short) 16);
		testInstance.setBitOffset((short) 4);
		testInstance.setMatch(true);

		JsonSerializer jsonizer = new JsonSerializer(Filter.class);

		try {
			jsonizer.setPrettyPrint(false);
			String serialized = jsonizer.serialize(testInstance);
			Filter deserialized = jsonizer.deserialize(serialized);

			assertEquals(testInstance.getBank(), deserialized.getBank());
			assertEquals(testInstance.getBitLength(),
					deserialized.getBitLength());
			assertEquals(testInstance.getBitOffset(),
					deserialized.getBitOffset());
			assertEquals(testInstance.isMatch(), deserialized.isMatch());
			assertArrayEquals(testInstance.getData(), deserialized.getData());
			assertArrayEquals(testInstance.getMask(), deserialized.getMask());

			jsonizer.setPrettyPrint(true);
			String serializedPretty = jsonizer.serialize(deserialized);
			Filter deserializedPretty = jsonizer.deserialize(serializedPretty);

			assertEquals(testInstance.getBank(), deserializedPretty.getBank());
			assertEquals(testInstance.getBitLength(),
					deserializedPretty.getBitLength());
			assertEquals(testInstance.getBitOffset(),
					deserializedPretty.getBitOffset());
			assertEquals(testInstance.isMatch(), deserializedPretty.isMatch());
			assertArrayEquals(testInstance.getData(),
					deserializedPretty.getData());
			assertArrayEquals(testInstance.getMask(),
					deserializedPretty.getMask());

		} catch (IOException e) {
			fail(e.toString());
		}

	}

}
