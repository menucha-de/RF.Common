package havis.device.rf.common.util;

import havis.device.rf.configuration.SelectionMask;
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
import havis.device.rf.tag.result.OperationResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class RFUtils {

	public final static short BANK_PSW = 0b00;
	public final static short BANK_EPC = 0b01;
	public final static short BANK_TID = 0b10;
	public final static short BANK_USR = 0b11;

	/**
	 * Inspects an operation list. Checks if the list is empty, contains read
	 * operations against the TID bank and if there are non-unique operation IDs
	 * in the list. Returned is an instance of
	 * {@link OperationListInspectionResult} containing LIST_INSPECTION_xyz
	 * flags representing the result of the inspection and also containing a
	 * reference to the read operation reading most data if the list contains a
	 * ReadOperation against the TID bank.
	 * 
	 * @param operations
	 *            a list of TagOperation instances
	 * 
	 * @return a number consisting of LIST_INSPECTION flags
	 */
	public static OperationListInspectionResult inspectOperationList(
			List<TagOperation> operations) {

		if (operations.size() == 0)
			return new OperationListInspectionResult(
					OperationListInspectionResult.LIST_INSPECTION_EMPTY);

		int flags = OperationListInspectionResult.LIST_INSPECTION_NONE;

		ReadOperation tidRdOp = null;
		Collection<String> uniqueSet = new HashSet<>();

		for (TagOperation operation : operations) {
			if (operation instanceof ReadOperation) {
				ReadOperation rdOp = (ReadOperation) operation;
				if (rdOp.getBank() == RFUtils.BANK_TID) {
					flags |= OperationListInspectionResult.LIST_INSPECTION_TID_READ_OPERATION;

					// if no read operation has been found before
					if (tidRdOp == null)
						tidRdOp = rdOp;

					// otherwise check if found read operation reads from a
					// bigger address than the one previously found
					// unless previously found operation is a read-complete-bank
					// operation
					else if (tidRdOp.getLength() != 0
							&& (rdOp.getLength() == 0 || (rdOp.getOffset() + rdOp
									.getLength()) > (tidRdOp.getOffset() + tidRdOp
									.getLength()))) {
						tidRdOp = rdOp;
					}
				}
			}

			if (!uniqueSet.add(operation.getOperationId()))
				flags |= OperationListInspectionResult.LIST_INSPECTION_OPERATION_ID_NOT_UNIQUE;
		}

		return new OperationListInspectionResult(flags, tidRdOp);
	}

	public static class OperationListInspectionResult {

		public static final int LIST_INSPECTION_NONE = 0b0000;
		public static final int LIST_INSPECTION_EMPTY = 0b0001;
		public static final int LIST_INSPECTION_TID_READ_OPERATION = 0b0010;
		public static final int LIST_INSPECTION_OPERATION_ID_NOT_UNIQUE = 0b0100;

		private int flags;
		private ReadOperation tidReadOperation;

		public OperationListInspectionResult(int flags) {
			this(flags, null);
		}

		public OperationListInspectionResult(int flags,
				ReadOperation tidReadOperation) {
			super();
			this.flags = flags;
			this.tidReadOperation = tidReadOperation;
		}

		public int getFlags() {
			return flags;
		}

		public ReadOperation getTidReadOperation() {
			return tidReadOperation;
		}
	}

	/**
	 * Converts a byte array to a hexadecimal string.
	 * 
	 * @param bytes
	 *            any array of bytes
	 * @return a hexadecimal string
	 */
	public static String bytesToHex(byte[] bytes) {
		if (bytes == null)
			return null;

		char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };
		char[] resChars = new char[bytes.length * 2];

		for (int iByte = 0; iByte < bytes.length; iByte++) {
			byte b = bytes[iByte];
			int b0 = (b & 0xf0) >> 4;
			int b1 = b & 0x0f;

			resChars[2 * iByte] = hexChars[b0];
			resChars[2 * iByte + 1] = hexChars[b1];
		}
		return new String(resChars);

	}

	/**
	 * Applies a bit mask as array of bytes to another array of bytes by binary
	 * AND-ing each byte.
	 * 
	 * @param dataBytes
	 *            an array of payload bytes
	 * @param maskBytes
	 *            an array of mask bytes
	 * @return an array of bytes containing the masked data
	 */
	public static byte[] applyMask(byte[] dataBytes, byte[] maskBytes) {
		byte[] filterBytes = new byte[dataBytes.length];
		byte maskByte;
		for (int i = 0; i < filterBytes.length; i++) {
			maskByte = (maskBytes != null && i < maskBytes.length) ? maskBytes[i]
					: 0;
			filterBytes[i] = (byte) (dataBytes[i] & maskByte);
		}

		return filterBytes;
	}

	/**
	 * Applies the mask stored in the given filter object leading to new
	 * sub-filter objects. For each section of the mask where the mask bits a
	 * set (i.e. 1), a new sub-filter is created and added to the list.
	 * 
	 * @param filter
	 *            a {@link Filter} instance
	 * @return a list of sub filters
	 */
	public static List<Filter> applyMask(Filter filter) {
		BitSet data = bytesToBitSet(filter.getData());
		BitSet mask = bytesToBitSet(filter.getMask());
		List<Filter> subFilters = null;

		int l = 0, r = 0;
		BitSet subSet = null;
		while (true) {
			// get the next set bit starting from the position of the first
			// clear bit from the previous round
			l = mask.nextSetBit(r);

			// No more set mask bits found or set bit lies behind filters bit
			// count boundaries
			if (l == -1 || l >= filter.getBitLength())
				break;

			// get the next clear bit starting from the position of the set bit
			// found above
			r = mask.nextClearBit(l);

			// if first clear bit lies behind filters bit count boundaries, set
			// r to maximum bit count
			// to avoid that bits are used that are excluded in the filter.
			if (r > filter.getBitLength())
				r = filter.getBitLength();

			// Mask has only set bits, then the whole filter applies an cannot
			// be split into sub-filters
			if (l == 0 && r == filter.getBitLength()) {
				subFilters = new ArrayList<>();
				// split filters beyond 255 bits
				if (filter.getBitLength() > 255) {
					int remaining = filter.getBitLength();
					int offset = 0;
					while (remaining > 0) {
						int length = Math.min(255, remaining);
						Filter subFilter = new Filter();
						subFilter.setMatch(filter.isMatch());
						subFilter.setBank(filter.getBank());
						subFilter.setBitLength((short) length);
						subFilter.setBitOffset((short) (filter.getBitOffset() + offset));
						subFilter.setData(bitSetToBytes(data.get(offset, offset + length), subFilter.getBitLength(), true));
						subFilter.setMask(bitSetToBytes(mask.get(offset, offset + length), subFilter.getBitLength(), true));
						subFilters.add(subFilter);
						remaining -= length;
						offset += length;
					}
				} else {
					subFilters.add(filter);
				}
				return subFilters;
			}

			// Initialize the result list if still null
			if (subFilters == null)
				subFilters = new ArrayList<>();

			// If the block is greater than 255, which is the maximum filter
			// size, reduce to 255.
			if ((r - l) > 255)
				r = r - ((r - l) - 255);

			// Create the subfilter from the bit positions (start and end index
			// of a block of 1s) determined above
			subSet = data.get(l, r);
			Filter subFilter = new Filter();
			subFilter.setMatch(filter.isMatch());
			subFilter.setBank(filter.getBank());
			subFilter.setBitLength((short) (r - l));
			subFilter.setBitOffset((short) (filter.getBitOffset() + l));
			subFilter.setData(bitSetToBytes(subSet, subFilter.getBitLength(), true));
			subSet.set(0, subFilter.getBitLength(), true);
			subFilter.setMask(bitSetToBytes(subSet, subFilter.getBitLength(), true));
			subFilters.add(subFilter);
		}
		return subFilters;
	}

	/**
	 * Converts a given bit set to a byte array by adding each bit to a new byte
	 * and adding a new byte for each 8th bit. Result is a byte array containing
	 * the bit set's bits in the same order they originally were (in contrast to
	 * using the BitSet method toByteArray which returns the bytes in reversed
	 * bit order.)
	 * 
	 * @param data
	 *            a bit set
	 * @param bitCnt
	 *            the total number of bits to written
	 * @param fill
	 *            whether to fill the last incomplete byte with data from the
	 *            bitset independent from the bit count <code>bitCnt</code>. If
	 *            <code>fill</code> is set to false and <code>bitCnt % 8</code>
	 *            does not equal 0, an incomplete byte will result. By setting
	 *            <code>fill</code> to true, such byte will be filled with the
	 *            data from the bit set.
	 * @return an array of bytes representing the bit set.
	 */
	public static byte[] bitSetToBytes(BitSet data, int bitCnt, boolean fill) {
		int byteCnt = (int) Math.ceil(bitCnt / 8.0f);
		byte[] ret = new byte[byteCnt];

		for (int bytes = 0; bytes < byteCnt; bytes++) {
			byte b = 0;
			for (int bit = 0; fill ? (bit < 8) : (bit < 8 && (bytes * 8 + bit) < bitCnt); bit++) {
				int bitNo = bytes * 8 + bit;
				b <<= 1;
				b |= (data.get(bitNo) ? 1 : 0);
			}
			ret[bytes] = b;
		}
		return ret;
	}

	/**
	 * Converts a byte array to a BitSet instance preserving the bit order (in
	 * contrast to using the BitSet method valueOf).
	 * 
	 * @param data
	 *            an array of bytes
	 * @return a {@link BitSet} instance containing the bits from the
	 *         <code>data</code> array.
	 */
	public static BitSet bytesToBitSet(byte[] data) {
		BitSet bs = new BitSet();
		for (int bytes = 0; bytes < data.length; bytes++) {
			boolean b;
			for (int bit = 0; bit < 8; bit++) {
				b = ((data[bytes]) & (1 << 7 - bit)) > 0;
				bs.set(bytes * 8 + bit, b);
			}
		}
		return bs;
	}

	/**
	 * Writes the bits of the given byte array to std. out in 4-bit clusters.
	 * 
	 * @param bytes
	 *            an array of bytes
	 * @return a string showing the specified bytes as bits in well-readable
	 *         4-bit-clusters
	 */
	public static String bytesToBin(byte[] bytes) {
		if (bytes == null)
			return null;

		String[] binStrings = { "0000", "0001", "0010", "0011", "0100", "0101",
				"0110", "0111", "1000", "1001", "1010", "1011", "1100", "1101",
				"1110", "1111" };
		StringBuffer resultBuffer = new StringBuffer(bytes.length * 8 + 16);

		for (int iByte = 0; iByte < bytes.length; iByte++) {
			byte b = bytes[iByte];
			int b0 = (b & 0xf0) >> 4;
			int b1 = b & 0x0f;

			resultBuffer.append(binStrings[b0]);
			resultBuffer.append(" ");
			resultBuffer.append(binStrings[b1]);
			resultBuffer.append(" ");

		}
		return resultBuffer.toString().trim();

	}

	/**
	 * Converts the given hexadecimal string to a byte array.
	 * 
	 * @param hexStr
	 *            a hexadecimal string
	 * @return an array of bytes
	 * @throws IllegalArgumentException
	 *             if the hex string specified has an odd number of characters.
	 */
	public static byte[] hexToBytes(String hexStr)
			throws IllegalArgumentException {
		hexStr = hexStr.replaceAll("\\s|_", "");
		if (hexStr.length() % 2 != 0)
			throw new IllegalArgumentException(
					"Hex string must have an even number of characters.");

		byte[] result = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length(); i += 2)
			result[i / 2] = Integer.decode(
					"0x" + hexStr.charAt(i) + hexStr.charAt(i + 1)).byteValue();

		return result;
	}

	/**
	 * Converts an array of bytes to an integer. This array must not contain
	 * more that 4 bytes (32 bits), to make sure the result can be stored in an
	 * integer.
	 * 
	 * @param maxFourBytes
	 *            4 bytes of data
	 * @return the bytes of data as one integer
	 * @throws IllegalArgumentException
	 *             if the byte array contains more than 4 bytes.
	 */
	public static int bytesToInt(byte[] maxFourBytes)
			throws IllegalArgumentException {
		if (maxFourBytes.length > 4)
			throw new IllegalArgumentException(
					"Byte array must not contain more than 4 bytes.");
		int ret = 0;
		for (int i = 0; i < maxFourBytes.length; i++) {
			ret |= maxFourBytes[i] & 0xff;
			if (i + 1 < maxFourBytes.length)
				ret <<= 8;
		}
		return ret;
	}

	/**
	 * Converts an array of bytes to a short. This array must not contain more
	 * that 2 bytes (16 bits), to make sure the result can be stored in a short.
	 * 
	 * @param maxTwoBytes
	 *            2 bytes of data
	 * @return the bytes of data as one short
	 * @throws IllegalArgumentException
	 *             if the byte array contains more than 2 bytes.
	 */
	public static short bytesToShort(byte[] maxTwoBytes)
			throws IllegalArgumentException {
		if (maxTwoBytes.length > 2)
			throw new IllegalArgumentException(
					"Byte array must not contain more than 2 bytes.");
		short ret = 0;
		for (int i = 0; i < maxTwoBytes.length; i++) {
			ret |= maxTwoBytes[i] & 0xff;
			if (i + 1 < maxTwoBytes.length)
				ret <<= 8;
		}
		return ret;
	}

	/**
	 * Converts an integer to a byte array (of 4 bytes).
	 * 
	 * @param num
	 *            an integer
	 * @return 4 bytes of data representing the integer <code>num</code>
	 */
	public static byte[] intToBytes(int num) {
		byte[] arr = new byte[4];
		byte b = 0;
		for (int i = 4; i > 0; i--) {
			b = (byte) (num & 0xff);
			num >>= 8;
			arr[i - 1] = b;
		}
		return arr;
	}

	/**
	 * Converts a short to a byte array (of 2 bytes).
	 * 
	 * @param num
	 *            a short
	 * @return 2 bytes of data representing the short <code>num</code>
	 */
	public static byte[] shortToBytes(short num) {
		byte[] arr = new byte[2];
		byte b = 0;
		for (int i = 2; i > 0; i--) {
			b = (byte) (num & 0xff);
			num >>= 8;
			arr[i - 1] = b;
		}
		return arr;
	}

	/**
	 * Creates a new read operation object.
	 * 
	 * @param id
	 * @param bank
	 * @param wordAddr
	 * @param wordCnt
	 * @param password
	 * @return a {@link ReadOperation} instance
	 */
	public static ReadOperation newReadOperation(String id, int bank,
			int wordAddr, int wordCnt, Integer password) {
		ReadOperation ret = new ReadOperation();
		ret.setOperationId(id);
		ret.setBank((short) bank);
		ret.setOffset((short) wordAddr);
		ret.setLength((short) wordCnt);
		if (password != null)
			ret.setPassword(password.intValue());

		return ret;
	}

	/**
	 * Creates a new write operation object.
	 * 
	 * @param id
	 * @param bank
	 * @param wordAddr
	 * @param data
	 * @param password
	 * @return a {@link WriteOperation} instance
	 */
	public static WriteOperation newWriteOperation(String id, int bank,
			int wordAddr, byte[] data, Integer password) {
		WriteOperation ret = new WriteOperation();
		ret.setOperationId(id);
		ret.setBank((short) bank);
		ret.setOffset((short) wordAddr);
		ret.setData(data);
		if (password != null)
			ret.setPassword(password.intValue());

		return ret;
	}

	/**
	 * Creates a new lock operation object.
	 * 
	 * @param id
	 * @param field
	 * @param priv
	 * @param password
	 * @return a {@link LockOperation} instance
	 */
	public static LockOperation newLockOperation(String id, Field field,
			Privilege priv, int password) {
		LockOperation ret = new LockOperation();
		ret.setOperationId(id);
		ret.setField(field);
		ret.setPrivilege(priv);
		ret.setPassword(password);
		return ret;
	}

	/**
	 * Creates a new kill operation object.
	 * 
	 * @param id
	 * @param killPsw
	 * @return a {@link KillOperation} instance
	 */
	public static KillOperation newKillOperation(String id, int killPsw) {
		KillOperation ret = new KillOperation();
		ret.setOperationId(id);
		ret.setKillPassword(killPsw);
		return ret;
	}

	/**
	 * Creates a new filter object.
	 * 
	 * @param bank
	 * @param bitOffset
	 * @param data
	 * @param mask
	 * @param bitCount
	 * @param match
	 * @return a {@link Filter} instance
	 */
	public static Filter newFilter(int bank, int bitOffset, byte[] data,
			byte[] mask, int bitCount, boolean match) {
		Filter f = new Filter();
		f.setBank((short) bank);
		f.setBitOffset((short) bitOffset);
		f.setData(data);
		f.setMask(mask);
		f.setBitLength((short) bitCount);
		f.setMatch(match);
		return f;
	}

	/**
	 * Writes a given result list to std. out. Binary data is either written as
	 * hex numbers (if binary == false) or in binary numbers (if binary == true)
	 * 
	 * @param result
	 *            a list of {@link TagData} objects
	 * @param binary
	 *            whether to output binary data in binary or in hex format
	 */
	public static void printResult(List<TagData> result, boolean binary) {
		for (TagData td : result) {
			System.out.printf("AntennaID: %d\n", td.getAntennaID());
			System.out.printf("Channel: %d\n", td.getChannel());
			System.out.printf("CRC: %s\n",
					bytesToHex(shortToBytes(td.getCrc())));
			System.out.printf("EPC: %s\n", bytesToHex(td.getEpc()));
			System.out.printf("PC: %s\n", bytesToBin(shortToBytes(td.getPc())));
			System.out.printf("RSSI: %d\n", td.getRssi());
			System.out.printf("XPC: %s\n", bytesToHex(intToBytes(td.getXpc())));

			for (OperationResult opRes : td.getResultList()) {
				if (opRes instanceof ReadResult) {
					ReadResult rdRes = (ReadResult) opRes;
					System.out.printf("Read Result(\"%s\", %s): %s\n", rdRes
							.getOperationId(), rdRes.getResult(),
							binary ? bytesToBin(rdRes.getReadData())
									: bytesToHex(rdRes.getReadData()));

				} else if (opRes instanceof WriteResult) {
					WriteResult wrRes = (WriteResult) opRes;
					System.out.printf(
							"Write Result(\"%s\", %s): %d word(s) written\n",
							wrRes.getOperationId(), wrRes.getResult(),
							wrRes.getWordsWritten());
				} else if (opRes instanceof LockResult) {
					LockResult lRes = (LockResult) opRes;
					System.out.printf("Lock Result(\"%s\"): %s\n",
							lRes.getOperationId(), lRes.getResult());
				} else if (opRes instanceof KillResult) {
					KillResult kRes = (KillResult) opRes;
					System.out.printf("Kill Result(\"%s\"): %s\n",
							kRes.getOperationId(), kRes.getResult());
				}
			}
		}
	}

	/**
	 * Serializes a list of <T> objects into a JSON string. This method is
	 * intended to be used for logging purposes. Therefore it throws no
	 * exception. If an object cannot be serialized, it will appear as
	 * FAILED_TO_SERIALIZE.
	 * 
	 * @param list
	 *            a list of objects of type T
	 * @param clazz
	 *            the objects' class
	 * @return a JSON string or null if list is null
	 */

	public static <T> String serializeList(List<T> list, Class<T> clazz) {
		if (list == null)
			return null;

		JsonSerializer json = new JsonSerializer(clazz);
		json.setPrettyPrint(true);
		String result = "[";
		for (int i = 0; i < list.size(); i++) {
			try {
				result += json.serialize(list.get(i));
			} catch (IOException e) {
				result += "FAILED_TO_SERIALIZE";
			}
			if (i + 1 < list.size())
				result += ",";
		}
		result += "]";

		return result;
	}

	/**
	 * Serializes an instance of <T> into a JSON string. This method is intended
	 * to be used for logging purposes. Therefore it throws no exception. If an
	 * object cannot be serialized, it will appear as FAILED_TO_SERIALIZE.
	 * 
	 * @param instance
	 *            an object of type T
	 * @return a JSON string or null if instance is null
	 */
	public static <T> String serialize(T instance) {
		if (instance == null)
			return null;

		JsonSerializer json = new JsonSerializer(instance.getClass());
		json.setPrettyPrint(true);
		String result = null;
		try {
			result = json.serialize(instance);
		} catch (IOException e) {
			result = "FAILED_TO_SERIALIZE";
		}

		return result;
	}

	 /**
	  * Creates a new filter instance from a given SelectionMask object.
	  * @param sMask an instance of {@link SelectionMask}
	  * @return a {@link Filter} instance with match property set to true and the mask property set to 0xFF for each byte of sMasks mask property.
	  */
	public static Filter createFilter(SelectionMask sMask) {		
		Filter ret = new Filter();
		ret.setBank(sMask.getBank());
		ret.setBitLength(sMask.getBitLength());
		ret.setBitOffset(sMask.getBitOffset());
		ret.setMatch(true);
		
		/* No mistake: selection mask's mask property becomes data property in Filter object */
		ret.setData(sMask.getMask());				
		byte[] mask = new byte[sMask.getMask().length];
		for (int i = 0; i < mask.length; i++) mask[i] = (byte)0xff;
		ret.setMask(mask);
		
		return ret;
	}

	/**
	 * Returns the ceiling byte size by bit length;
	 * 
	 * @param length
	 *            The bit length
	 * @return The byte size
	 * @throws IllegalArgumentException
	 *             If length is not positive or length + 8 is greater then max
	 *             value of integer
	 */
	static int size(int length) {
		return size(length, 8);
	}

	/**
	 * Returns the ceiling byte size by bit length;
	 * 
	 * @param length
	 *            The bit length
	 * @param size
	 *            The bit size, default 8
	 * @return The byte size
	 * @throws IllegalArgumentException
	 *             If length is not positive or length + 8 is greater then max
	 *             value of integer
	 */
	static int size(int length, int size) {
		if ((length > -1) && (length < Integer.MAX_VALUE)) {
			return (length + (size - length % size) % size) / size;
		} else {
			throw new IllegalArgumentException("Length should be greater or equal to zero and lower then max value of integer minus size");
		}
	}

	/**
	 * Compares the first n-th bits of left and right array
	 * 
	 * @param left
	 *            The left array
	 * @param right
	 *            The right array
	 * @param n
	 *            The bit length
	 * @param offset
	 *            The bit offset
	 * @return Positiv number if the first n-th bits of left array are greater
	 *         than bits of right array. Negative number if the first n-th bits
	 *         of left are lower than bits of right array. Zero if the first
	 *         n-th bits of both arrays are equal.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the size of left or right array is to low for comparison
	 */
	static int compare(byte[] left, byte[] right, int n, int offset) {
		if (offset > 0) {
			int c = compare(left[offset / 8], right[offset / 8], n > 8 - offset % 8 ? 8 - offset % 8 : n, offset % 8);
			if (c != 0)
				return c;
		}
		for (int i = size(offset); i < n / 8; i++) {
			if ((left[i] & 0xFF) > (right[i] & 0xFF)) {
				return 1;
			}
			if ((left[i] & 0xFF) < (right[i] & 0xFF)) {
				return -1;
			}
		}
		if (n % 8 > 0) {
			return compare(left[n / 8], right[n / 8], n % 8, n > 8 ? 0 : offset);
		}
		return 0;
	}

	/**
	 * Compares the first n-th bits of two bytes
	 * 
	 * @param left
	 *            The left byte
	 * @param right
	 *            The right byte
	 * @param n
	 *            Th bit length
	 * @param offset
	 *            The bit offset
	 * @return Positiv number if the first n-th bits of left byte is greater
	 *         than bits of right byte. Negative number if the first n-th bits
	 *         of left byte is lower than bits of right byte. Zero if the first
	 *         n-th bits of both bytes are equal.
	 */
	static int compare(byte left, byte right, int n, int offset) {
		byte mask = (byte) ((0xFF << (8 - n % 8) - offset % 8) & (0xFF >> (offset % 8)));
		int l = 0, r = 0;
		if (mask != 0) {
			l = left & 0xFF & mask;
			r = right & 0xFF & mask;
		} else {
			// don't use the mask if the whole byte is compared
			l = left & 0xFF;
			r = right & 0xFF;
		}

		if (l > r) {
			return 1;
		} else if (l < r) {
			return -1;
		}
		return 0;
	}

	public static boolean equal(byte[] left, byte[] right, int n) {
		return equal(left, right, n, 0);
	}

	/**
	 * Returns if the first n-th bits of array left and right are equal
	 * 
	 * @param left
	 *            The left array
	 * @param right
	 *            The right array
	 * @param n
	 *            The bit length
	 * @param offset
	 *            The bit offset
	 * @return True if the first n-th bits of array left and right are equal,
	 *         false otherwise
	 * @throws ArrayIndexOutOfBoundsException
	 *             - if bit size of left or right array is lower then n
	 */
	public static boolean equal(byte[] left, byte[] right, int n, int offset) {
		return compare(left, right, n, offset) == 0;
	}

	/**
	 * Shifts byte array n bits left
	 * 
	 * @param bytes
	 *            The byte array
	 * @param n
	 *            The bit count
	 * @return The shifted byte array
	 * @throws IllegalArgumentException
	 *             if n is not lower than bytes size
	 */
	public static byte[] shift(byte[] bytes, int n) {
		return shift(bytes, bytes.length * 8, n);
	}

	/**
	 * Shifts byte array n bits left
	 * 
	 * @param bytes
	 *            The byte array
	 * @param length
	 *            The bit length of bytes
	 * @param n
	 *            The bit count
	 * @return The shifted byte array
	 * @throws IllegalArgumentException
	 *             if n is not lower than bytes size
	 */
	public static byte[] shift(byte[] bytes, int length, int n) {
		if (n <= bytes.length * 8) {
			byte[] result = new byte[size(bytes.length * 8 - n)];
			if (bytes.length > 0) {
				if (n % 8 > 0) {
					for (int i = 0; i < result.length - 1; i++) {
						result[i] = (byte) (((bytes[i + n / 8] & 0xFF) << (n % 8)) + ((bytes[i + n / 8 + 1] & 0xFF) >> (8 - (n % 8))));
					}
					result[result.length - 1] = (byte) ((bytes[result.length - 1 + n / 8] & 0xFF) << (n % 8));
				} else if (n % 8 < 0) {
					result[-n / 8] = (byte) ((bytes[0] & 0xFF) >> (-n % 8));
					for (int i = 0; i < bytes.length - 1; i++) {
						result[i - n / 8 + 1] = (byte) (((bytes[i] & 0xFF) << (8 + (n % 8))) + ((bytes[i + 1] & 0xFF) >> (-n % 8)));
					}
					result[result.length - 1] = (byte) ((bytes[bytes.length - 1] & 0xFF) << (8 + (n % 8)));
				} else {
					if (n < 0) {
						for (int i = 0; i < bytes.length; i++) {
							result[i - n / 8] = bytes[i];
						}
					} else {
						for (int i = 0; i < result.length; i++) {
							result[i] = bytes[i + n / 8];
						}
					}
				}
			}
			return strip(result, 0, (((length - n) / 8) + (((length - n) % 8 > 0) ? 1 : 0)) * 8);
		} else {
			throw new IllegalArgumentException("The bit count should be lower or equal to bit length of bytes");
		}
	}

	/**
	 * Strips the byte array. Shifts byte array offset bits left and cuts data
	 * by length
	 * 
	 * @param bytes
	 *            The byte array
	 * @param offset
	 *            The bit offset
	 * @param length
	 *            The bit length
	 * @return The striped byte array
	 */
	public static byte[] strip(byte[] bytes, int offset, int length) {
		if ((offset + length) / 8 + ((offset + length) % 8 == 0 ? 0 : 1) <= bytes.length) {
			int l = length == 0 ? bytes.length - offset / 8 : size(length);
			// number of bytes of the result data
			byte[] result = new byte[l];

			// skip bytes depending on field offset
			int byteIndex = offset / 8;
			int byteLength = byteIndex + (length == 0 ? bytes.length - offset / 8 : size(offset % 8 + length));

			// number of bits to move within a byte
			offset = offset % 8;
			if (byteIndex < byteLength) {
				for (int i = 0; i < result.length; i++) {
					// move current byte offset bits left and add next byte, if
					// offset greater then zero move next byte (8 - offset) bits
					// right
					result[i] = (byte) (((bytes[byteIndex] & 0xFF) << offset) + ((++byteIndex < byteLength) && offset > 0 ? (bytes[byteIndex] & 0xFF) >> (8 - offset)
							: 0));
				}
				if (length % 8 > 0) {
					// blank last bits
					int current = result[result.length - 1] & 0xFF;
					current &= 0xFF << (8 - length % 8);
					result[result.length - 1] = (byte) current;
				}
			}
			return result;
		} else {
			throw new IndexOutOfBoundsException("Offset plus length must not be greater than bytes.lenght");
		}
	}
}
