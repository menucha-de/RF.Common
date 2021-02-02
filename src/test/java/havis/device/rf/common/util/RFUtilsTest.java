package havis.device.rf.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import havis.device.rf.common.util.RFUtils.OperationListInspectionResult;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.KillOperation;
import havis.device.rf.tag.operation.LockOperation;
import havis.device.rf.tag.operation.LockOperation.Field;
import havis.device.rf.tag.operation.LockOperation.Privilege;
import havis.device.rf.tag.operation.ReadOperation;
import havis.device.rf.tag.operation.TagOperation;
import havis.device.rf.tag.operation.WriteOperation;
import havis.device.rf.tag.result.KillResult;
import havis.device.rf.tag.result.LockResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Test;

public class RFUtilsTest {

	@Test
	public void testOperationListInspectionResultClass() {
		OperationListInspectionResult res1 = new OperationListInspectionResult(
				OperationListInspectionResult.LIST_INSPECTION_TID_READ_OPERATION);
		assertNull(res1.getTidReadOperation());
		assertTrue(res1.getFlags() == OperationListInspectionResult.LIST_INSPECTION_TID_READ_OPERATION);

		ReadOperation rdOp = new ReadOperation();
		OperationListInspectionResult res2 = new OperationListInspectionResult(
				OperationListInspectionResult.LIST_INSPECTION_OPERATION_ID_NOT_UNIQUE,
				rdOp);

		assertEquals(res2.getTidReadOperation(), rdOp);
		assertTrue(res2.getFlags() == OperationListInspectionResult.LIST_INSPECTION_OPERATION_ID_NOT_UNIQUE);
	}

	@Test
	public void testInspectOperationList() {
		List<TagOperation> operations = new ArrayList<>();
		OperationListInspectionResult res = RFUtils
				.inspectOperationList(operations);
		assertTrue(res.getFlags() == OperationListInspectionResult.LIST_INSPECTION_EMPTY);

		operations.add(new WriteOperation());
		res = RFUtils.inspectOperationList(operations);
		assertTrue(res.getFlags() == OperationListInspectionResult.LIST_INSPECTION_NONE);

		ReadOperation rdOp1 = new ReadOperation() {
			{
				setOperationId("rdOp-1");
			}
		};
		operations.add(rdOp1);

		res = RFUtils.inspectOperationList(operations);
		assertTrue(res.getFlags() == OperationListInspectionResult.LIST_INSPECTION_NONE);

		rdOp1.setBank(RFUtils.BANK_TID);
		res = RFUtils.inspectOperationList(operations);
		assertTrue(res.getFlags() == OperationListInspectionResult.LIST_INSPECTION_TID_READ_OPERATION);
		assertTrue(res.getTidReadOperation() != null);
		assertEquals(res.getTidReadOperation(), rdOp1);

		ReadOperation rdOp2 = new ReadOperation() {
			{
				setOperationId("rdOp-2");
				setBank(RFUtils.BANK_TID);
			}
		};
		operations.add(rdOp2);
		res = RFUtils.inspectOperationList(operations);
		assertTrue(res.getFlags() == OperationListInspectionResult.LIST_INSPECTION_TID_READ_OPERATION);
		assertTrue(res.getTidReadOperation() != null);
		assertEquals(res.getTidReadOperation(), rdOp1);

		rdOp1.setLength((short) 4);
		rdOp1.setOffset((short) 16);
		rdOp2.setLength((short) 8);
		rdOp2.setOffset((short) 2);
		res = RFUtils.inspectOperationList(operations);
		assertEquals(res.getTidReadOperation(), rdOp1);

		rdOp1.setLength((short) 4);
		rdOp1.setOffset((short) 0);
		rdOp2.setLength((short) 0);
		rdOp2.setOffset((short) 0);
		res = RFUtils.inspectOperationList(operations);
		assertEquals(res.getTidReadOperation(), rdOp2);

		rdOp1.setLength((short) 8);
		rdOp1.setOffset((short) 2);
		rdOp2.setLength((short) 16);
		rdOp2.setOffset((short) 4);
		res = RFUtils.inspectOperationList(operations);
		assertEquals(res.getTidReadOperation(), rdOp2);

		ReadOperation rdOp3 = new ReadOperation() {
			{
				setOperationId("rdOp-2");
				setBank(RFUtils.BANK_TID);
			}
		};
		operations.add(rdOp3);
		res = RFUtils.inspectOperationList(operations);
		assertEquals(res.getTidReadOperation(), rdOp3);
		assertTrue(res.getFlags() == (OperationListInspectionResult.LIST_INSPECTION_TID_READ_OPERATION | OperationListInspectionResult.LIST_INSPECTION_OPERATION_ID_NOT_UNIQUE));

	}

	@Test
	public void testBytesToHex() {
		String hexStr = "BEEFCAFE";
		byte[] bytes = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length(); i += 2)
			bytes[i / 2] = Integer.decode(
					"0x" + hexStr.charAt(i) + hexStr.charAt(i + 1)).byteValue();

		assertEquals(hexStr, RFUtils.bytesToHex(bytes));
		assertNull(RFUtils.bytesToHex(null));
	}

	@Test
	public void testHexToBytes() {
		byte[] bytes = { (byte) 0xbe, (byte) 0xef, (byte) 0xca, (byte) 0xfe };
		String hex = "beefcafe";
		assertArrayEquals(bytes, RFUtils.hexToBytes(hex.toLowerCase()));
		assertArrayEquals(bytes, RFUtils.hexToBytes(hex.toUpperCase()));
		
		try {
			RFUtils.hexToBytes("beefc");
			fail("Exception expected.");
		} catch (IllegalArgumentException e) { }		
	}

	@Test
	public void testApplyMaskByteArrayByteArray() {

		byte[] xffffffff = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
		byte[] x00000000 = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		assertArrayEquals(RFUtils.applyMask(xffffffff, xffffffff), xffffffff);
		assertArrayEquals(RFUtils.applyMask(x00000000, x00000000), x00000000);
		assertArrayEquals(RFUtils.applyMask(xffffffff, x00000000), x00000000);
		assertArrayEquals(RFUtils.applyMask(x00000000, xffffffff), x00000000);

	}

	@Test
	public void testApplyMaskFilter() {
		Filter f = new Filter();
		f.setData(new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101 });
		f.setBitLength((short) (f.getData().length * 8));
		f.setBitOffset((short) 0);

		List<Filter> subFilters = null;

		f.setMask(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111 });
		subFilters = RFUtils.applyMask(f);
		assertEquals(1, subFilters.size());
		assertEquals(f, subFilters.get(0));

		f.setMask(new byte[] { (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000 });
		subFilters = RFUtils.applyMask(f);
		assertNull(subFilters);

		f.setMask(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b00000000, (byte) 0b00000000 });
		subFilters = RFUtils.applyMask(f);
		assertTrue(subFilters.size() == 1);

		f.setMask(new byte[] { (byte) 0b11111111, (byte) 0b00000000, (byte) 0b11111111, (byte) 0b00000000 });
		subFilters = RFUtils.applyMask(f);
		assertTrue(subFilters.size() == 2);
		assertArrayEquals(subFilters.get(0).getData(), new byte[] { f.getData()[0] });
		assertArrayEquals(subFilters.get(1).getData(), new byte[] { f.getData()[2] });
		assertTrue(subFilters.get(0).getBitLength() == 8);
		assertTrue(subFilters.get(0).getBitOffset() == 0);
		assertTrue(subFilters.get(1).getBitLength() == 8);
		assertTrue(subFilters.get(1).getBitOffset() == 16);

		f.setMask(new byte[] { (byte) 0b11110000, (byte) 0b00001111, (byte) 0b11110000, (byte) 0b00001111 });
		subFilters = RFUtils.applyMask(f);
		assertTrue(subFilters.size() == 3);
		assertTrue(subFilters.get(0).getBitLength() == 4);
		assertTrue(subFilters.get(0).getBitOffset() == 0);
		assertArrayEquals(subFilters.get(0).getData(), new byte[] { (byte) 0b10100000 });
		assertTrue(subFilters.get(1).getBitLength() == 8);
		assertTrue(subFilters.get(1).getBitOffset() == 12);
		assertArrayEquals(subFilters.get(1).getData(), new byte[] { (byte) 0b11001110 });
		assertTrue(subFilters.get(2).getBitLength() == 4);
		assertTrue(subFilters.get(2).getBitOffset() == 28);
		assertArrayEquals(subFilters.get(2).getData(), new byte[] { (byte) 0b01010000 });
	}

	@Test
	public void testApplyMaskFilterExcessLength() {
		// 256bit EPC
		Filter f = new Filter();
		f.setBank((short) 1);
		f.setMatch(true);
		f.setData(new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101 });
		f.setMask(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111 });
		f.setBitLength((short) (f.getData().length * 8));
		f.setBitOffset((short) 32);

		List<Filter> subFilters = null;
		subFilters = RFUtils.applyMask(f);
		assertEquals(2, subFilters.size());
		assertEquals((short) 1, subFilters.get(0).getBank());
		assertTrue(subFilters.get(0).isMatch());
		assertEquals(32, subFilters.get(0).getBitOffset());
		assertEquals(255, subFilters.get(0).getBitLength());
		assertArrayEquals(new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010100 }, subFilters.get(0).getData());
		assertArrayEquals(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111110 }, subFilters.get(0).getMask());

		assertEquals((short) 1, subFilters.get(1).getBank());
		assertTrue(subFilters.get(1).isMatch());
		assertEquals(287, subFilters.get(1).getBitOffset());
		assertEquals(1, subFilters.get(1).getBitLength());
		assertArrayEquals(new byte[] { (byte) 0b10000000 }, subFilters.get(1).getData());
		assertArrayEquals(new byte[] { (byte) 0b10000000 }, subFilters.get(1).getMask());
		
		f.setData(new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101 });
		f.setMask(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111 });
		f.setBitLength((short) (f.getData().length * 8));
		f.setBitOffset((short) 32);

		//496bit EPC
		f.setData(new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001101 });
		f.setMask(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111 });
		f.setBitLength((short) (f.getData().length * 8));
		f.setBitOffset((short) 32);

		subFilters = null;
		subFilters = RFUtils.applyMask(f);
		assertEquals(2, subFilters.size());
		assertEquals((short) 1, subFilters.get(0).getBank());
		assertTrue(subFilters.get(0).isMatch());
		assertEquals(32, subFilters.get(0).getBitOffset());
		assertEquals(255, subFilters.get(0).getBitLength());
		assertArrayEquals(new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010100 }, subFilters.get(0).getData());
		assertArrayEquals(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111110 }, subFilters.get(0).getMask());

		assertEquals((short) 1, subFilters.get(1).getBank());
		assertTrue(subFilters.get(1).isMatch());
		assertEquals(287, subFilters.get(1).getBitOffset());
		assertEquals(241, subFilters.get(1).getBitLength());
		assertArrayEquals(new byte[] { (byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110,
				(byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101,
				(byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010,
				(byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111,
				(byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110, (byte) 0b10000000 }, subFilters.get(1).getData());
		assertArrayEquals(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b10000000 }, subFilters.get(1).getMask());
		
		//496bit EPC with scattered mask
		f.setData(new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001101 });
		f.setMask(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b00000000, (byte) 0b11111111, (byte) 0b11111111 });
		f.setBitLength((short) (f.getData().length * 8));
		f.setBitOffset((short) 32);

		subFilters = null;
		subFilters = RFUtils.applyMask(f);
		assertEquals(3, subFilters.size());
		assertEquals((short) 1, subFilters.get(0).getBank());
		assertTrue(subFilters.get(0).isMatch());
		assertEquals(32, subFilters.get(0).getBitOffset());
		assertEquals(255, subFilters.get(0).getBitLength());
		assertArrayEquals(new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100,
				(byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010,
				(byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101,
				(byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110,
				(byte) 0b01010101, (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11101110, (byte) 0b01010100 }, subFilters.get(0).getData());
		assertArrayEquals(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111110 }, subFilters.get(0).getMask());

		assertEquals((short) 1, subFilters.get(1).getBank());
		assertTrue(subFilters.get(1).isMatch());
		assertEquals(287, subFilters.get(1).getBitOffset());
		assertEquals(217, subFilters.get(1).getBitLength());
		assertArrayEquals(new byte[] { (byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110,
				(byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101,
				(byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010,
				(byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111, (byte) 0b00101010, (byte) 0b11010101, (byte) 0b01100110, (byte) 0b01110111,
				(byte) 0b00000000 }, subFilters.get(1).getData());
		assertArrayEquals(new byte[] { (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
				(byte) 0b10000000 }, subFilters.get(1).getMask());

		assertEquals((short) 1, subFilters.get(2).getBank());
		assertTrue(subFilters.get(2).isMatch());
		assertEquals(512, subFilters.get(2).getBitOffset());
		assertEquals(16, subFilters.get(2).getBitLength());
		assertArrayEquals(new byte[] { (byte) 0b10101010, (byte) 0b11001101 }, subFilters.get(2).getData());
		assertArrayEquals(new byte[] { (byte) 0b11111111, (byte) 0b11111111 }, subFilters.get(2).getMask());
	}

	@Test
	public void testBytesToBin() {
		String binStr = RFUtils.bytesToBin(new byte[] { (byte) 0b10101010,
				(byte) 0b01010101, (byte) 0b11110000, (byte) 0b11110000 });

		assertEquals(binStr, "1010 1010 0101 0101 1111 0000 1111 0000");

		assertNull(RFUtils.bytesToBin(null));
	}

	@Test
	public void testBytesToInt() {
		int iMax = RFUtils.bytesToInt(new byte[] { (byte) 0x7f, (byte) 0xff,
				(byte) 0xff, (byte) 0xff });
		// 0111 1111 1111 1111
		// ^ positive sign

		int iMin = RFUtils.bytesToInt(new byte[] { (byte) 0x80, (byte) 0x00,
				(byte) 0x00, (byte) 0x00 });
		// 1000 0000 0000 0000
		// ^ negative sign
		assertTrue(iMax == Integer.MAX_VALUE);
		assertTrue(iMin == Integer.MIN_VALUE);

		try {
			RFUtils.bytesToInt(new byte[] { (byte) 0xaa, (byte) 0xbb,
					(byte) 0xcc, (byte) 0xdd, (byte) 0xee });
			fail("Exception expected but none thrown.");
		} catch (IllegalArgumentException ex) {
		}

	}

	@Test
	public void testBytesToShort() {
		short sMax = RFUtils
				.bytesToShort(new byte[] { (byte) 0x7f, (byte) 0xff });
		// 0111 1111
		// ^ positive sign

		short sMin = RFUtils
				.bytesToShort(new byte[] { (byte) 0x80, (byte) 0x00 });
		// 1000 0000
		// ^ negative sign
		assertTrue(sMax == Short.MAX_VALUE);
		assertTrue(sMin == Short.MIN_VALUE);

		try {
			RFUtils.bytesToShort(new byte[] { (byte) 0xaa, (byte) 0xbb,
					(byte) 0xcc });
			fail("Exception expected but none thrown.");
		} catch (IllegalArgumentException ex) {
		}

	}

	@Test
	public void testIntToBytes() {
		byte[] iMax = new byte[] { (byte) 0x7f, (byte) 0xff, (byte) 0xff,
				(byte) 0xff };
		// 0111 1111 1111 1111
		// ^ positive sign

		byte[] iMin = new byte[] { (byte) 0x80, (byte) 0x00, (byte) 0x00,
				(byte) 0x00 };
		// 1000 0000 0000 0000
		// ^ negative sign

		assertArrayEquals(iMin, RFUtils.intToBytes(Integer.MIN_VALUE));
		assertArrayEquals(iMax, RFUtils.intToBytes(Integer.MAX_VALUE));
	}

	@Test
	public void testShortToBytes() {
		byte[] sMax = new byte[] { (byte) 0x7f, (byte) 0xff };
		// 0111 1111
		// ^ positive sign

		byte[] sMin = new byte[] { (byte) 0x80, (byte) 0x00 };
		// 1000 0000
		// ^ negative sign

		assertArrayEquals(sMin, RFUtils.shortToBytes(Short.MIN_VALUE));
		assertArrayEquals(sMax, RFUtils.shortToBytes(Short.MAX_VALUE));
	}

	@Test
	public void testNewReadOperation() {
		String id = "id";
		short bank = RFUtils.BANK_EPC;
		short wordAddr = (short) 4;
		short wordCnt = (short) 8;
		int password = 12345678;

		ReadOperation rdOp = RFUtils.newReadOperation(id, bank, wordAddr,
				wordCnt, password);

		assertEquals(rdOp.getOperationId(), id);
		assertTrue(rdOp.getBank() == bank);
		assertTrue(rdOp.getOffset() == wordAddr);
		assertTrue(rdOp.getLength() == wordCnt);
		assertTrue(rdOp.getPassword() == password);

	}

	@Test
	public void testNewWriteOperation() {
		String id = "id";
		short bank = RFUtils.BANK_EPC;
		short wordAddr = (short) 4;
		byte[] data = new byte[] { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc,
				(byte) 0xdd };
		int password = 12345678;

		WriteOperation wrOp = RFUtils.newWriteOperation(id, bank, wordAddr,
				data, password);

		assertEquals(wrOp.getOperationId(), id);
		assertArrayEquals(wrOp.getData(), data);
		assertTrue(wrOp.getBank() == bank);
		assertTrue(wrOp.getOffset() == wordAddr);
		assertTrue(wrOp.getPassword() == password);
	}

	@Test
	public void testNewLockOperation() {
		String id = "id";
		Field field = Field.EPC_MEMORY;
		Privilege priv = Privilege.PERMALOCK;
		int password = 12345678;

		LockOperation lkOp = RFUtils
				.newLockOperation(id, field, priv, password);

		assertEquals(lkOp.getOperationId(), id);
		assertTrue(lkOp.getField() == field);
		assertTrue(lkOp.getPrivilege() == priv);
		assertTrue(lkOp.getPassword() == password);
	}

	@Test
	public void testNewKillOperation() {
		String id = "id";
		int killPwd = 12345678;

		KillOperation klOp = RFUtils.newKillOperation(id, killPwd);
		assertEquals(klOp.getOperationId(), id);
		assertTrue(klOp.getKillPassword() == killPwd);
	}

	@Test
	public void testNewFilter() {

		final short bank = RFUtils.BANK_EPC;
		final short bitLength = 32;
		final short bitOffset = 16;
		final byte[] data = { (byte) 0xca, (byte) 0xfe, (byte) 0xba,
				(byte) 0xbe };
		final byte[] mask = { (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
				(byte) 0xaa };
		final boolean match = true;

		Filter filter = RFUtils.newFilter(bank, bitOffset, data, mask,
				bitLength, match);

		assertTrue(filter.getBank() == bank);
		assertTrue(filter.getBitLength() == bitLength);
		assertTrue(filter.getBitOffset() == bitOffset);
		assertTrue(filter.isMatch() == match);
		assertArrayEquals(filter.getData(), data);
		assertArrayEquals(filter.getMask(), mask);
	}

	@Test
	public void testPrintResult() {
		PrintStream sysOut = System.out;

		try {
			List<TagData> tagData = new ArrayList<>();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			System.setOut(new PrintStream(bos));

			Random rand = new Random();

			final byte[] _epc = new byte[8];
			final byte[] _crc = new byte[2];
			final byte[] _pc = new byte[2];
			final int _xpc = rand.nextInt();

			rand.nextBytes(_epc);
			rand.nextBytes(_crc);
			rand.nextBytes(_pc);

			TagData td1 = new TagData() {
				{
					setAntennaID((short) 1);
					setChannel((short) 2);
					setCrc(RFUtils.bytesToShort(_crc));
					setEpc(_epc);
					setPc(RFUtils.bytesToShort(_pc));
					setRssi(-52);
					setTagDataId(1L);
					setXpc(_xpc);

				}
			};

			tagData.add(td1);

			final byte[] rdData = new byte[8];
			rand.nextBytes(rdData);

			final ReadResult rdRes = new ReadResult() {
				{
					setOperationId("RdOp-1");
					setReadData(rdData);
					setResult(ReadResult.Result.SUCCESS);
				}
			};

			td1.getResultList().add(rdRes);

			final WriteResult wrRes = new WriteResult() {
				{
					setOperationId("WrOp-1");
					setWordsWritten((short) 8);
					setResult(WriteResult.Result.SUCCESS);
				}
			};

			td1.getResultList().add(wrRes);

			final LockResult lkRes = new LockResult() {
				{
					setOperationId("LkOp-1");
					setResult(LockResult.Result.SUCCESS);
				}
			};

			td1.getResultList().add(lkRes);

			final KillResult klRes = new KillResult() {
				{
					setOperationId("KlOp-1");
					setResult(KillResult.Result.SUCCESS);
				}
			};

			td1.getResultList().add(klRes);
			RFUtils.printResult(tagData, true);
			String output = bos.toString();
			System.setOut(sysOut);

			assertTrue(Pattern.compile("^AntennaID: 1$", Pattern.MULTILINE)
					.matcher(output).find());
			assertTrue(Pattern.compile("^Channel: 2$", Pattern.MULTILINE)
					.matcher(output).find());
			assertTrue(Pattern
					.compile(
							String.format("^CRC: %s$", RFUtils.bytesToHex(_crc)),
							Pattern.MULTILINE).matcher(output).find());
			assertTrue(Pattern
					.compile(
							String.format("^EPC: %s$", RFUtils.bytesToHex(_epc)),
							Pattern.MULTILINE).matcher(output).find());
			assertTrue(Pattern
					.compile(String.format("^PC: %s$", RFUtils.bytesToBin(_pc)),
							Pattern.MULTILINE).matcher(output).find());
			assertTrue(Pattern.compile("^RSSI: -52", Pattern.MULTILINE)
					.matcher(output).find());
			assertTrue(Pattern
					.compile(
							String.format("^XPC: %s$",
									RFUtils.bytesToHex(RFUtils.intToBytes(_xpc))),
							Pattern.MULTILINE).matcher(output).find());
			assertTrue(Pattern
					.compile(
							String.format("^Read Result\\(\"%s\", %s\\): %s$",
									rdRes.getOperationId(), rdRes.getResult()
											.toString(), RFUtils
											.bytesToBin(rdData)),
							Pattern.MULTILINE).matcher(output).find());
			assertTrue(Pattern
					.compile(
							String.format(
									"^Write Result\\(\"%s\", %s\\): %d word\\(s\\) written$",
									wrRes.getOperationId(), wrRes.getResult()
											.toString(), wrRes
											.getWordsWritten()),
							Pattern.MULTILINE).matcher(output).find());
			assertTrue(Pattern
					.compile(
							String.format("^Lock Result\\(\"%s\"\\): %s$",
									lkRes.getOperationId(), lkRes.getResult()
											.toString()), Pattern.MULTILINE)
					.matcher(output).find());
			assertTrue(Pattern
					.compile(
							String.format("^Kill Result\\(\"%s\"\\): %s$",
									klRes.getOperationId(), klRes.getResult()
											.toString()), Pattern.MULTILINE)
					.matcher(output).find());
		} finally {
			System.setOut(sysOut);
		}

	}
	
	@Test
	public void testSerializeList(@Mocked final JsonSerializer jsonizer) throws IOException {
		/*
		 * Test: 
		 * 	- serializeList is called with null argument
		 * Expected:
		 * 	- null is returned		
		 */
		assertNull(RFUtils.serializeList(null, TagData.class));
		
		/*
		 * Test: 
		 * 	- serializeList is called with a list containing three TagData objects
		 * Expected:
		 * 	- JsonSerializer's serialize method is called once per object (three times in total)
		 */
		final TagData td1 = new TagData();		
		final TagData td2 = new TagData();
		final TagData td3 = new TagData();
		List<TagData> tdList = Arrays.asList(td1, td2, td3);
		RFUtils.serializeList(tdList, TagData.class);
		
		new Verifications() {{ 
			jsonizer.serialize(withSameInstance(td1));
			jsonizer.serialize(withSameInstance(td2));
			jsonizer.serialize(withSameInstance(td3));
		}};
		
		/*
		 * Test:
		 * 	- serializeList is called with a list containing three TagData objects
		 * 	- JsonSerializer's serialize method throws an IOException
		 * Expected:
		 * 	- for each object in the list the string "FAILED_TO_SERIALIZE" is returned from the serializer, 
		 * 	  resulting in the string "[FAILED_TO_SERIALIZE,FAILED_TO_SERIALIZE,FAILED_TO_SERIALIZE]"
		 */
		new NonStrictExpectations() {{
			jsonizer.serialize(any);
			result = new IOException();
		}};
		
		assertEquals("[FAILED_TO_SERIALIZE,FAILED_TO_SERIALIZE,FAILED_TO_SERIALIZE]", 
				RFUtils.serializeList(tdList, TagData.class));		
		
	}
	
	@Test
	public void testSerialize(@Mocked final JsonSerializer jsonizer) throws IOException {
		/*
		 * Test: 
		 * 	- serialize is called with null argument
		 * Expected:
		 * 	- null is returned		
		 */
		assertNull(RFUtils.serialize(null));
		
		/*
		 * Test: 
		 * 	- serialize is called with a TagData object
		 * Expected:
		 * 	- JsonSerializer's serialize method is called once 
		 */
		final TagData td = new TagData();		
		RFUtils.serialize(td);
		
		new Verifications() {{ 
			jsonizer.serialize(withSameInstance(td));
		}};
		
		/*
		 * Test:
		 * 	- serializeList is called with a TagData object
		 * 	- JsonSerializer's serialize method throws an IOException
		 * Expected:
		 * 	- the string "FAILED_TO_SERIALIZE" is returned 
		 */
		new NonStrictExpectations() {{
			jsonizer.serialize(any);
			result = new IOException();
		}};
		
		assertEquals("FAILED_TO_SERIALIZE", RFUtils.serialize(td));		
		
	}

}
