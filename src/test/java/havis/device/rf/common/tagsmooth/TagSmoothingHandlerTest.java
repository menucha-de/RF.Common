package havis.device.rf.common.tagsmooth;

import havis.device.rf.configuration.TagSmoothingSettings;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.TagDataList;
import havis.device.rf.tag.result.OperationResult;
import havis.device.rf.tag.result.ReadResult;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mockit.Deencapsulation;

import org.junit.Assert;
import org.junit.Test;

public class TagSmoothingHandlerTest {
	private static Integer IGNORE_VALUE = 999;
	private static int NANO_TO_MILLI_FACTOR = 1000000;

	private static TagSmoothingEntry GetLastEntryFromMap(Map<TagSmoothingEntry, TagSmoothingEntry> entries) {
		TagSmoothingEntry last = null;
		for (Iterator<TagSmoothingEntry> it = entries.keySet().iterator(); it.hasNext();)
			last = it.next();
		return last;
	}

	/**
	 * This test case tests the functionality of the TagSmoothingHandler
	 * constructor, by checking that the given parameters from the configuration
	 * file are bound to the corresponding members
	 * 
	 * @throws Exception
	 */
	@Test
	public void tagSmoothingHandler() throws Exception {
		Integer glimpsedTimeout = Integer.valueOf(1);
		Integer observedTimeThreshold = Integer.valueOf(2);
		Integer observedCountThreshold = Integer.valueOf(3);
		Integer lostTimeout = Integer.valueOf(4);
		TagSmoothingSettings config = new TagSmoothingSettings(glimpsedTimeout, observedCountThreshold, observedTimeThreshold, lostTimeout);

		TagSmoothingHandler handler = new TagSmoothingHandler(config);

		Assert.assertFalse((boolean) Deencapsulation.getField(handler, "enabled"));
		Assert.assertEquals(glimpsedTimeout, Deencapsulation.getField(handler, "glimpsedTimeout"));
		Assert.assertEquals(observedTimeThreshold, Deencapsulation.getField(handler, "observedTimeThreshold"));
		Assert.assertEquals(observedCountThreshold, Deencapsulation.getField(handler, "observedCountThreshold"));
		Assert.assertEquals(lostTimeout, Deencapsulation.getField(handler, "lostTimeout"));
		Assert.assertNotNull(Deencapsulation.getField(handler, "observedEntries"));
		Assert.assertNotNull(Deencapsulation.getField(handler, "glimpsedEntries"));
	}

	/**
	 * This test case that an entry that already exists in the given map is
	 * moved to the end of the map
	 *
	 * @throws Exception
	 */
	@Test
	public void addOrMoveToEndWithMovingExistingElementToEnd() throws Exception {
		LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> entries = new LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry>();
		TagData dummyTag1 = new TagData();
		dummyTag1.setEpc(new byte[] { 0x01 });
		TagSmoothingEntry dummy1 = new TagSmoothingEntry(dummyTag1);
		entries.put(dummy1, dummy1);
		TagData tag2 = new TagData();
		tag2.setEpc(new byte[] { 0x02 });
		TagSmoothingEntry entry = new TagSmoothingEntry(tag2);
		entries.put(entry, entry);
		TagData dummyTag3 = new TagData();
		dummyTag3.setEpc(new byte[] { 0x03 });
		TagSmoothingEntry dummy3 = new TagSmoothingEntry(dummyTag3);
		entries.put(dummy3, dummy3);

		Assert.assertSame(dummy3, GetLastEntryFromMap(entries));

		TagSmoothingEntry result = Deencapsulation.invoke(new TagSmoothingHandler(new TagSmoothingSettings(Integer.valueOf(30), IGNORE_VALUE, null, null)),
				"addOrMoveToEnd", entry, entries);

		// Check that returned element is given and at the end of the map
		Assert.assertSame(entry, result);
		Assert.assertSame(entry, GetLastEntryFromMap(entries));
	}

	/**
	 * This test case tests that an entry that is not present in the given map,
	 * is added to the end of the given map
	 *
	 * @throws Exception
	 */
	@Test
	public void addOrMoveToEndWithAddingNewElement() throws Exception {
		LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> entries = new LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry>();
		TagData dummyTag1 = new TagData();
		dummyTag1.setEpc(new byte[] { 0x01 });
		TagSmoothingEntry dummy1 = new TagSmoothingEntry(dummyTag1);
		entries.put(dummy1, dummy1);
		TagData dummyTag2 = new TagData();
		dummyTag2.setEpc(new byte[] { 0x02 });
		TagSmoothingEntry dummy2 = new TagSmoothingEntry(dummyTag2);
		entries.put(dummy2, dummy2);
		TagData tag3 = new TagData();
		tag3.setEpc(new byte[] { 0x03 });
		TagSmoothingEntry entry = new TagSmoothingEntry(tag3);

		Assert.assertSame(dummy2, GetLastEntryFromMap(entries));
		TagSmoothingEntry result = Deencapsulation.invoke(new TagSmoothingHandler(new TagSmoothingSettings(Integer.valueOf(30), IGNORE_VALUE, null, null)),
				"addOrMoveToEnd", entry, entries);
		// Check that returned element is given and at the end of the map
		Assert.assertSame(entry, result);
		Assert.assertSame(entry, GetLastEntryFromMap(entries));
	}

	/**
	 * This test case validates that a tag which is not marked as observed is
	 * not reported in the result list
	 * 
	 * @throws Exception
	 */
	@Test
	public void getResultListWithNoResults() throws Exception {
		// prepare
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, new Integer(5), null, null));
		TagDataList tagList = new TagDataList();
		TagData tag = new TagData();
		tag.setEpc(new byte[] { 0x00 });
		tagList.getEntryList().add(tag);

		// execute
		handler.process(tagList);

		// check
		Assert.assertTrue("Expecting empty list because tag did not match count threshold and " + "is therefore not marked as " + "observed", handler
				.getResultList().isEmpty());
	}

	@Test
	public void getResultListWithResults() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, new Integer(1), null, null));
		TagDataList tagList = new TagDataList();
		TagData tag = new TagData();
		tag.setEpc(new byte[] { 0x00 });
		tagList.getEntryList().add(tag);

		handler.process(tagList);
		Assert.assertTrue("Expecting empty list because tag did not match count threshold and " + "is therefore not marked as observed", handler
				.getResultList().isEmpty());
		handler.process(tagList);
		List<TagData> resultList = handler.getResultList();
		Assert.assertFalse("Expecting tag in list because it matches the count threshold and " + "should therefore be marked as observed", resultList.isEmpty());
		Assert.assertEquals(1, resultList.size());
		Assert.assertSame(tag, resultList.get(0));
	}

	@Test
	public void isEnabled() throws Exception {
		TagSmoothingSettings settings = new TagSmoothingSettings(null, IGNORE_VALUE, null, null);

		TagSmoothingHandler handler = new TagSmoothingHandler(settings);
		Assert.assertFalse(handler.isEnabled());

		settings.setEnabled(true);
		TagSmoothingHandler handler2 = new TagSmoothingHandler(settings);
		Assert.assertTrue(handler2.isEnabled());
	}

	/**
	 * This test cases validates that an observed entry is not removed if it has
	 * not exceeded the LostTimeout
	 * 
	 * @throws Exception
	 */
	@Test
	public void isExpiredWithLostTimeoutNotExceeded() throws Exception {
		TagData t1 = new TagData();
		t1.setEpc(new byte[] { 0x01 });
		TagSmoothingEntry entry = new TagSmoothingEntry(t1);
		entry.setObserved(true);
		Deencapsulation.setField(entry, "lastSeen", System.currentTimeMillis());

		Assert.assertFalse((boolean) Deencapsulation.invoke(new TagSmoothingHandler(new TagSmoothingSettings(null, IGNORE_VALUE, null, Integer.valueOf(30))),
				"isExpired", entry));
	}

	/**
	 * This test cases validates that an observed entry is removed if it has
	 * exceeded the LostTimeout
	 *
	 * @throws Exception
	 */
	@Test
	public void isExpiredWithLostTimeoutExceeded() throws Exception {
		TagData t1 = new TagData();
		t1.setEpc(new byte[] { 0x01 });
		TagSmoothingEntry entry = new TagSmoothingEntry(t1);
		entry.setObserved(true);
		Deencapsulation.setField(entry, "lastSeen", System.currentTimeMillis());

		Thread.sleep(31);
		Assert.assertTrue((boolean) Deencapsulation.invoke(new TagSmoothingHandler(new TagSmoothingSettings(null, IGNORE_VALUE, null, Integer.valueOf(30))),
				"isExpired", entry));
	}

	/**
	 * This test cases validates that an glimpsed entry is not removed if it has
	 * not exceeded the glimpsedTimeout
	 *
	 * @throws Exception
	 */
	@Test
	public void isExpiredWithGlimpsedTimeoutNotExceeded() throws Exception {
		TagData t1 = new TagData();
		t1.setEpc(new byte[] { 0x01 });
		TagSmoothingEntry entry = new TagSmoothingEntry(t1);
		Deencapsulation.setField(entry, "lastSeen", System.currentTimeMillis());

		Assert.assertFalse((boolean) Deencapsulation.invoke(new TagSmoothingHandler(new TagSmoothingSettings(Integer.valueOf(30), IGNORE_VALUE, null, null)),
				"isExpired", entry));
	}

	/**
	 * This test cases validates that an glimpsed entry is removed if it has
	 * exceeded the glimpsedTimeout
	 *
	 * @throws Exception
	 */
	@Test
	public void isExpiredWithGlimpsedTimeoutExceeded() throws Exception {
		TagData t1 = new TagData();
		t1.setEpc(new byte[] { 0x01 });
		TagSmoothingEntry entry = new TagSmoothingEntry(t1);
		Deencapsulation.setField(entry, "lastSeen", System.currentTimeMillis());
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(Integer.valueOf(30), IGNORE_VALUE, null, null));

		Thread.sleep(31);
		Assert.assertTrue((boolean) Deencapsulation.invoke(handler, "isExpired", entry));
	}

	/**
	 * This test case validates that every entry in the given list of tags is
	 * processed and expired tag entries are removed
	 * 
	 * @throws Exception
	 */
	@Test
	public void processTagList() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, new Integer(0), null, new Integer(30)));
		TagDataList tagList = new TagDataList();
		List<TagData> tags = tagList.getEntryList();
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		tags.add(tag1);
		TagData tag2 = new TagData();
		tag2.setEpc(new byte[] { 0x02 });
		tags.add(tag2);

		handler.process(tagList);

		Map<TagSmoothingEntry, TagSmoothingEntry> observedEntries = Deencapsulation.getField(handler, "observedEntries");
		Assert.assertEquals(2, observedEntries.size());
		Assert.assertSame(tag1, observedEntries.keySet().iterator().next().getTag());
		Assert.assertSame(tag2, GetLastEntryFromMap(observedEntries).getTag());

		Thread.sleep(31);

		handler.process(new TagDataList());
		Map<TagSmoothingEntry, TagSmoothingEntry> observedEntries2 = Deencapsulation.getField(handler, "observedEntries");
		Assert.assertTrue(observedEntries2.isEmpty());
	}

	/**
	 * This test case validates that every property of the TagData of an
	 * existing TagSmoothingEntry, except the epc, is updated when it was seen
	 * again
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void processTagData() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, IGNORE_VALUE, null, null));
		TagData tag = new TagData();
		tag.setAntennaID((short) 1);
		tag.setChannel((short) 2);
		tag.setCrc((short) 3);
		tag.setPc((short) 4);
		tag.setRssi(5);
		tag.setTagDataId(6);
		tag.setXpc(7);
		byte[] epc = new byte[0x00];
		tag.setEpc(epc);
		TagSmoothingEntry existingEntry = new TagSmoothingEntry(tag);
		((Map<TagSmoothingEntry, TagSmoothingEntry>) Deencapsulation.getField(handler, "glimpsedEntries")).put(existingEntry, existingEntry);

		TagData newTag = new TagData();
		newTag.setAntennaID((short) 8);
		newTag.setChannel((short) 9);
		newTag.setCrc((short) 10);
		newTag.setPc((short) 11);
		newTag.setRssi(12);
		newTag.setTagDataId(13);
		newTag.setXpc(14);
		byte[] epcNew = new byte[0x00];
		newTag.setEpc(epcNew);
		TagDataList tagList = new TagDataList();
		tagList.getEntryList().add(newTag);

		handler.process(tagList);

		Assert.assertEquals(newTag.getAntennaID(), tag.getAntennaID());
		Assert.assertEquals(newTag.getChannel(), tag.getChannel());
		Assert.assertEquals(newTag.getCrc(), tag.getCrc());
		Assert.assertEquals(newTag.getPc(), tag.getPc());
		Assert.assertEquals(newTag.getRssi(), tag.getRssi());
		Assert.assertEquals(newTag.getTagDataId(), tag.getTagDataId());
		Assert.assertEquals(newTag.getXpc(), tag.getXpc());
		Assert.assertSame(newTag.getResultList(), tag.getResultList());
		Assert.assertArrayEquals(epc, tag.getEpc());
		Assert.assertSame(epc, tag.getEpc());
	}

	/**
	 * This test case validates that a tag that does not met the observing
	 * thresholds is processed as a glimpsed tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void processTagDataWithGlimpsedEntry() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, new Integer(1), null, new Integer(30)));
		TagData tag = new TagData();
		tag.setEpc(new byte[] { 0x00 });

		Deencapsulation.invoke(handler, "process", tag);

		Map<TagSmoothingEntry, TagSmoothingEntry> glimpsedEntries = Deencapsulation.getField(handler, "glimpsedEntries");
		Assert.assertEquals(1, glimpsedEntries.size());
		TagSmoothingEntry entry = glimpsedEntries.keySet().iterator().next();
		Assert.assertEquals(1, entry.getSeenCount());
		Assert.assertSame(tag, entry.getTag());
	}

	/**
	 * This test case validates that a tag that is already observed gets updated
	 * when it is processed
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void processTagDataWithObservedTag() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, new Integer(1), null, new Integer(30)));
		TagData tag = new TagData();
		tag.setEpc(new byte[] { 0x00 });
		Map<TagSmoothingEntry, TagSmoothingEntry> observedEntries = Deencapsulation.getField(handler, "observedEntries");
		TagSmoothingEntry entry = new TagSmoothingEntry(tag);
		entry.setObserved(true);
		long lastSeenTimeBefore = entry.getLastSeen();
		// Sleeping so that lastSeenTime is different after processing
		Thread.sleep(1);
		observedEntries.put(entry, entry);

		Deencapsulation.invoke(handler, "process", tag);

		Map<TagSmoothingEntry, TagSmoothingEntry> observedEntriesAfter = Deencapsulation.getField(handler, "observedEntries");
		Assert.assertEquals(1, observedEntriesAfter.size());
		TagSmoothingEntry entryAfter = observedEntriesAfter.keySet().iterator().next();
		Assert.assertNotEquals(lastSeenTimeBefore, entryAfter.getLastSeen());
		Assert.assertEquals(1, entryAfter.getSeenCount());
		Assert.assertSame(tag, entryAfter.getTag());
		Assert.assertTrue(((Map<TagSmoothingEntry, TagSmoothingEntry>) Deencapsulation.getField(handler, "glimpsedEntries")).isEmpty());
	}

	/**
	 * This test case validates that all expired entries at the head of the
	 * given map are removed
	 *
	 * @throws Exception
	 */
	@Test
	public void removeExpiredEntries() throws Exception {
		Integer glimpsedTimeout = Integer.valueOf(30);
		LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> entries = new LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry>();
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagSmoothingEntry entry1 = new TagSmoothingEntry(tag1);
		Deencapsulation.setField(entry1, "lastSeen", System.currentTimeMillis() - glimpsedTimeout - 1);
		entries.put(entry1, entry1);
		TagData tag2 = new TagData();
		tag2.setEpc(new byte[] { 0x02 });
		TagSmoothingEntry entry2 = new TagSmoothingEntry(tag2);
		Deencapsulation.setField(entry2, "lastSeen", System.currentTimeMillis() - glimpsedTimeout - 1);
		entries.put(entry2, entry2);
		TagData tag3 = new TagData();
		tag3.setEpc(new byte[] { 0x03 });
		TagSmoothingEntry entry3 = new TagSmoothingEntry(tag3);
		entries.put(entry3, entry3);

		Assert.assertEquals(3, entries.size());
		Deencapsulation.invoke(new TagSmoothingHandler(new TagSmoothingSettings(glimpsedTimeout, IGNORE_VALUE, null, null)), "removeExpiredEntries", entries);

		Assert.assertEquals(1, entries.size());
		Assert.assertSame(entry3, entries.keySet().iterator().next());
	}

	/**
	 * This test cases validates that an given entry is removed from the
	 * glimpsedEntries map, is marked as observed, and is moved to the
	 * observedEntries map
	 *
	 * @throws Exception
	 */
	@Test
	public void setObserved() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, IGNORE_VALUE, null, null));
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagSmoothingEntry entry = new TagSmoothingEntry(tag1);
		Map<TagSmoothingEntry, TagSmoothingEntry> glimpsedEntries = Deencapsulation.getField(handler, "glimpsedEntries");
		glimpsedEntries.put(entry, entry);

		Assert.assertEquals(1, glimpsedEntries.size());
		Deencapsulation.invoke(handler, "setObserved", entry);

		Assert.assertTrue(glimpsedEntries.isEmpty());
		Map<TagSmoothingEntry, TagSmoothingEntry> observedEntries = Deencapsulation.getField(handler, "observedEntries");
		Assert.assertEquals(1, observedEntries.size());
		TagSmoothingEntry observedEntry = observedEntries.keySet().iterator().next();
		Assert.assertSame(entry, observedEntry);
		Assert.assertTrue(observedEntry.isObserved());
	}

	/**
	 * This test case validates that the TagData of the given TagSmoothingEntry
	 * is updated with the given TagData. The epc should not be updated.
	 * 
	 * @throws Exception
	 */
	@Test
	public void updateTagOfEntry() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, null, IGNORE_VALUE, null));
		TagData tag = new TagData();
		tag.setAntennaID((short) 1);
		tag.setChannel((short) 2);
		tag.setCrc((short) 3);
		tag.setPc((short) 4);
		tag.setRssi(5);
		tag.setTagDataId(6);
		tag.setXpc(7);
		byte[] epc = new byte[0x00];
		tag.setEpc(epc);
		List<OperationResult> resultList = new ArrayList<OperationResult>();
		ReadResult readResult = new ReadResult();
		readResult.setOperationId("ABC");
		tag.setResultList(resultList);
		TagSmoothingEntry entry = new TagSmoothingEntry(tag);
		TagData newTag = new TagData();
		newTag.setAntennaID((short) 8);
		newTag.setChannel((short) 9);
		newTag.setCrc((short) 10);
		newTag.setPc((short) 11);
		newTag.setRssi(12);
		newTag.setTagDataId(13);
		newTag.setXpc(14);
		newTag.setEpc(new byte[0x01]);
		List<OperationResult> resultListNew = new ArrayList<OperationResult>();
		ReadResult readResultNew = new ReadResult();
		readResultNew.setOperationId("CBE");
		newTag.setResultList(resultListNew);

		Deencapsulation.invoke(handler, "updateTagOfEntry", newTag, entry);

		Assert.assertEquals(newTag.getAntennaID(), tag.getAntennaID());
		Assert.assertEquals(newTag.getChannel(), tag.getChannel());
		Assert.assertEquals(newTag.getCrc(), tag.getCrc());
		Assert.assertEquals(newTag.getPc(), tag.getPc());
		Assert.assertEquals(newTag.getRssi(), tag.getRssi());
		Assert.assertEquals(newTag.getTagDataId(), tag.getTagDataId());
		Assert.assertEquals(newTag.getXpc(), tag.getXpc());
		Assert.assertSame(newTag.getResultList(), tag.getResultList());
		Assert.assertArrayEquals(epc, tag.getEpc());
		Assert.assertSame(epc, tag.getEpc());
	}

	// ***********************************************************************
	// ************************Integration tests *****************************
	// ***********************************************************************
	/**
	 * This test case tests the functionality of the observedCountThreshold, by
	 * validating that a tag is only reported as seen if the number of
	 * tagSightings for tag t1 > observedCountThreshold
	 *
	 * @throws Exception
	 */
	@Test
	public void tagIsSeenAfterObservedCountThreshold() throws Exception {
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(Integer.valueOf(30), Integer.valueOf(1), null, Integer.valueOf(60)));
		TagDataList tagList = new TagDataList();
		tagList.getEntryList().add(tag1);

		handler.process(tagList);
		Assert.assertTrue("Not seen because tag was only seen once", handler.getResultList().isEmpty());
		handler.process(tagList);
		Assert.assertFalse("Seen because countThreshold was reached", handler.getResultList().isEmpty());
		Assert.assertSame(tag1, handler.getResultList().get(0));
	}

	/**
	 * This test case tests the functionality of the observedTimeThreshold, by
	 * validating that a tag is only reported as seen if the time since the
	 * first sighting of tag t1 > observedTimeThreshold
	 *
	 * @throws Exception
	 */
	@Test
	public void tagIsSeenAfterObservedTimeThreshold() throws Exception {
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagDataList tagList = new TagDataList();
		tagList.getEntryList().add(tag1);
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(Integer.valueOf(50), null, Integer.valueOf(30), Integer.valueOf(60)));

		handler.process(tagList);
		Assert.assertTrue("Not seen because timeThreshold was not reached", handler.getResultList().isEmpty());
		Thread.sleep(31);
		handler.process(tagList);
		Assert.assertFalse("Seen because timeThreshold was reached", handler.getResultList().isEmpty());
	}

	/**
	 * This test case tests the functionality when both thresholds are defined.
	 * This is tested by validating that tag t1 is only reported as seen after
	 * observedTimeTreshold and tag t2 is only reported as seen after
	 * observedCountThreshold
	 *
	 * @throws Exception
	 */
	@Test
	public void tagsAreSeenThroughBothTresholds() throws Exception {
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagDataList tagListTimeTreshold = new TagDataList();
		tagListTimeTreshold.getEntryList().add(tag1);

		TagData tag2 = new TagData();
		tag2.setEpc(new byte[] { 0x02 });
		TagDataList tagListCountThreshold = new TagDataList();
		tagListCountThreshold.getEntryList().add(tag2);
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(null, Integer.valueOf(1), Integer.valueOf(30), null));

		// TimeThreshold
		handler.process(tagListTimeTreshold);
		Assert.assertTrue("Not seen because timeThreshold was not reached", handler.getResultList().isEmpty());
		Thread.sleep(31);
		handler.process(tagListTimeTreshold);
		List<TagData> resultList1 = handler.getResultList();
		Assert.assertFalse("Seen because timeThreshold was reached", resultList1.isEmpty());
		Assert.assertEquals(1, resultList1.size());
		Assert.assertSame(tag1, resultList1.get(0));

		// CountThreshold
		handler.process(tagListCountThreshold);
		List<TagData> resultList2 = handler.getResultList();
		Assert.assertFalse("Tag from before is still seen because no lost timeout was defined", resultList2.isEmpty());
		Assert.assertEquals(1, resultList1.size());
		Assert.assertSame("Only first tag was seen", tag1, resultList1.get(0));
		handler.process(tagListCountThreshold);
		List<TagData> resultList3 = handler.getResultList();
		Assert.assertEquals(2, resultList3.size());
		Assert.assertSame("Second tag was seen because count treshold was reached", tag2, resultList3.get(1));

	}

	/**
	 * This test case tests the basic functionality of the TagSmoothing
	 * algorithm. It also proves that a seen tag is not affected by the
	 * glimpsedTimeout but by the lostTimeout
	 *
	 * @throws Exception
	 */
	@Test
	public void observedTagRemovedByLostTimeout() throws Exception {
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagDataList tagList = new TagDataList();
		tagList.getEntryList().add(tag1);
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(Integer.valueOf(50), null, Integer.valueOf(30), Integer.valueOf(60)));

		handler.process(tagList);
		Assert.assertTrue("Tag should not be seen yet", handler.getResultList().isEmpty());
		Thread.sleep(31);
		handler.process(tagList);
		Assert.assertFalse("Tag should be seen now, because observedTimeThreshold was exceeded", handler.getResultList().isEmpty());
		Thread.sleep(51);
		handler.process(tagList);
		Assert.assertFalse("Tag is still seen, and not affected by glimpsedTimeout because it is " + "observed", handler.getResultList().isEmpty());
		Thread.sleep(61);
		handler.process(new TagDataList());
		Assert.assertTrue("Tag should not be seen anymore, because it was removed due lostTimeOut", handler.getResultList().isEmpty());
	}

	/**
	 * This test case tests the functionality of the glimpsedTimeout, by
	 * validating that tag t1 is removed from the list of glimpsed tags when
	 * it's not reported again for a time t > glimpsedTimeout
	 *
	 * @throws Exception
	 */
	@Test
	public void tagRemovedByGlimspedTimeout() throws Exception {
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagDataList tagList = new TagDataList();
		tagList.getEntryList().add(tag1);
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(Integer.valueOf(30), Integer.valueOf(3), null, Integer.valueOf(60)));

		handler.process(tagList);
		Assert.assertTrue("Tag should not be seen because observedCountThreshhold is 3", handler.getResultList().isEmpty());
		Map<TagSmoothingEntry, TagSmoothingEntry> unobservedEntries = Deencapsulation.getField(handler, "glimpsedEntries");
		Assert.assertEquals(1, unobservedEntries.size());
		Assert.assertSame(tag1, unobservedEntries.keySet().iterator().next().getTag());

		// Sleeping till glimspedTimeout is exceeded
		Thread.sleep(31);

		handler.process(new TagDataList());
		Assert.assertEquals(0, unobservedEntries.size());
		Assert.assertTrue(handler.getResultList().isEmpty());
	}

	// ***********************************************************************
	// ************************Performance tests *****************************
	// ***********************************************************************
	/**
	 * This test case validates that processing 500 EQUAL tags is done under
	 * 25ms
	 *
	 * @throws Exception
	 */
	//@Test
	public void performanceOfFiveHundredEqualTags() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(30, 2, 30, 30));

		long sum = 0;
		int numberOfRuns = 500;
		int expectedTime = 25;

		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagDataList tagList = new TagDataList();
		tagList.getEntryList().add(tag1);

		for (int i = 0; i < numberOfRuns; i++) {
			long start = System.nanoTime();
			handler.process(tagList);
			sum += System.nanoTime() - start;
		}
		Assert.assertTrue("Expected " + expectedTime + "ms, but was " + (sum / NANO_TO_MILLI_FACTOR) + "ms", sum / NANO_TO_MILLI_FACTOR < expectedTime);
	}

	/**
	 * This test case validates that processing 500 DIFFERENT tags is done under
	 * 25ms
	 *
	 * @throws Exception
	 */
	//@Test
	public void performanceOfFiveHundredDifferentTags() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(30, 30, 2, 30));

		long sum = 0;
		int numberOfRuns = 500;
		int expectedTime = 25;

		TagDataList[] tagListList = new TagDataList[numberOfRuns];
		for (int j = 0; j < numberOfRuns; j++) {
			TagDataList tagList = new TagDataList();
			TagData tag = new TagData();
			tag.setEpc(ByteBuffer.allocate(4).putInt(j).array());
			tagList.getEntryList().add(tag);
			tagListList[j] = tagList;
		}

		for (int i = 0; i < numberOfRuns; i++) {
			long start = System.nanoTime();
			handler.process(tagListList[i]);
			sum += System.nanoTime() - start;
		}
		Assert.assertTrue("Expected " + expectedTime + "ms, but was " + (sum / NANO_TO_MILLI_FACTOR) + "ms", sum / NANO_TO_MILLI_FACTOR < expectedTime);
	}

	/**
	 * This test case validates that processing 1000 EQUAL tags is done under
	 * 50ms (time of one inventory)
	 *
	 * @throws Exception
	 */
	//@Test
	public void performanceOfThousandEqualsTags() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(30, 30, 2, 30));

		long sum = 0;
		int numberOfRuns = 1000;
		int expectedTime = 50;
		TagData tag1 = new TagData();
		tag1.setEpc(new byte[] { 0x01 });
		TagDataList tagList = new TagDataList();
		tagList.getEntryList().add(tag1);

		for (int i = 0; i < numberOfRuns; i++) {
			long start = System.nanoTime();
			handler.process(tagList);
			sum += System.nanoTime() - start;
		}
		Assert.assertTrue("Expected " + expectedTime + "ms, but was " + (sum / NANO_TO_MILLI_FACTOR) + "ms", sum / NANO_TO_MILLI_FACTOR < expectedTime);
	}

	/**
	 * This test case validates that processing 1000 DIFFERENT tags is done
	 * under 50ms (time of one inventory)
	 *
	 * @throws Exception
	 */
	//@Test
	public void performanceOfThousandDifferentTags() throws Exception {
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(30, 30, 2, 30));

		long sum = 0;
		int numberOfRuns = 1000;
		int expectedTime = 50;
		TagDataList[] tagListList = new TagDataList[numberOfRuns];
		for (int j = 0; j < numberOfRuns; j++) {
			TagDataList tagList = new TagDataList();
			TagData tag = new TagData();
			tag.setEpc(ByteBuffer.allocate(4).putInt(j).array());
			tagList.getEntryList().add(tag);
			tagListList[j] = tagList;
		}

		for (int i = 0; i < numberOfRuns; i++) {
			long start = System.nanoTime();
			handler.process(tagListList[i]);
			sum += System.nanoTime() - start;
		}
		Assert.assertTrue("Expected " + expectedTime + "ms, but was " + (sum / NANO_TO_MILLI_FACTOR) + "ms", sum / NANO_TO_MILLI_FACTOR < expectedTime);
	}

	/**
	 * This test case validates that processing 500 DIFFERENT tags, when
	 * removing 500 already existing tags, is done under 50ms (time of one
	 * inventory)
	 *
	 * @throws Exception
	 */
	//@Test
	public void performanceWhenFiveHundredOldEntriesAreRemovedAndFiveHundredNewAdded() throws Exception {
		int numberOfTags = 500;
		Integer glimpsedTimeout = Integer.valueOf(numberOfTags / 10);
		int numberOfTagsBetweenWaitingTimes = numberOfTags / glimpsedTimeout;
		int expectedTime = 50;

		// prepare [numberOfTags] entries that will already exists at the start
		LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> entries = new LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry>();
		for (int i = 0; i < numberOfTags; i++) {
			for (int j = 0; j < numberOfTagsBetweenWaitingTimes; j++) {
				TagData tag = new TagData();
				tag.setEpc(ByteBuffer.allocate(4).putInt(j).array());

				TagSmoothingEntry entry = new TagSmoothingEntry(tag);
				entries.put(entry, entry);
			}
			Thread.sleep(10);
		}
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(glimpsedTimeout, 60, 2, 30));
		Deencapsulation.setField(handler, "glimpsedEntries", entries);

		// prepare [numberOfTags] entries to process
		TagDataList[] tagListList = new TagDataList[numberOfTags];
		for (int i = 0; i < numberOfTags; i++) {
			TagDataList tagList = new TagDataList();
			TagData tag = new TagData();
			tag.setEpc(ByteBuffer.allocate(4).putInt(numberOfTags + i).array());
			tagList.getEntryList().add(tag);
			tagListList[i] = tagList;
		}

		// measure processing time
		long sum = 0;

		for (int i = 0; i < numberOfTags; i++) {
			long start = System.nanoTime();
			handler.process(tagListList[i]);
			sum += System.nanoTime() - start;
		}
		Assert.assertTrue("Expected " + expectedTime + "ms, but was " + (sum / NANO_TO_MILLI_FACTOR) + "ms", sum / NANO_TO_MILLI_FACTOR < expectedTime);
	}

	/**
	 * This test case validates that processing 500 DIFFERENT tags, when 500
	 * glimpsed and 500 observed tags exist which must be partially removed, is
	 * done under 50ms (time of one inventory)
	 *
	 * @throws Exception
	 */
	//@Test
	public void performanceWhenFiveHundredGlimAndFiveHundredObsEntriesAlreadyExists() throws Exception {
		int numberOfTags = 500;
		Integer timeout = Integer.valueOf(numberOfTags / 10);
		int numberOfTagsBetweenWaitingTimes = numberOfTags / timeout;
		int expectedTime = 50;

		// prepare [numberOfTags] entries that will already exists at the start
		LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> glimpsedEntries = new LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry>();
		LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> observedEntries = new LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry>();
		boolean observed = false;
		for (int i = 0; i < numberOfTags * 2; i++) {
			for (int j = 0; j < numberOfTagsBetweenWaitingTimes; j++) {
				TagData tag = new TagData();
				tag.setEpc(ByteBuffer.allocate(4).putInt(j).array());
				TagSmoothingEntry entry = new TagSmoothingEntry(tag);
				if (observed)
					observedEntries.put(entry, entry);
				else
					glimpsedEntries.put(entry, entry);

				observed = observed ? false : true;
			}
			Thread.sleep(5);
		}
		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(timeout, 60, 2, timeout));
		Deencapsulation.setField(handler, "glimpsedEntries", glimpsedEntries);
		Deencapsulation.setField(handler, "observedEntries", observedEntries);

		// prepare [numberOfTags] entries to process
		TagDataList[] tagListList = new TagDataList[numberOfTags];
		for (int i = 0; i < numberOfTags; i++) {
			TagDataList tagList = new TagDataList();
			TagData tag = new TagData();
			tag.setEpc(ByteBuffer.allocate(4).putInt(numberOfTags + i).array());
			tagList.getEntryList().add(tag);
			tagListList[i] = tagList;
		}

		// measure processing time
		long sum = 0;
		for (int i = 0; i < numberOfTags; i++) {
			long start = System.nanoTime();
			handler.process(tagListList[i]);
			sum += System.nanoTime() - start;
		}
		Assert.assertTrue("Expected " + expectedTime + "ms, but was " + (sum / NANO_TO_MILLI_FACTOR) + "ms", sum / NANO_TO_MILLI_FACTOR < expectedTime);
	}

	/**
	 * This test case validates that processing 1000 DIFFERENT tags in one call,
	 * to emulate one inventory with 1000 tags, is done under 50ms (time of one
	 * inventory)
	 *
	 * @throws Exception
	 */
	//@Test
	public void performanceWhenThousandTagsAreToProcessInOne() throws Exception {
		int numberOfTags = 1000;
		Integer timeout = Integer.valueOf(numberOfTags / 10);
		int expectedTime = 50;

		// prepare [numberOfTags] entries to process
		TagDataList tagList = new TagDataList();
		for (int i = 0; i < numberOfTags; i++) {
			TagData tag = new TagData();
			tag.setEpc(ByteBuffer.allocate(4).putInt(i).array());
			tagList.getEntryList().add(tag);
		}

		TagSmoothingHandler handler = new TagSmoothingHandler(new TagSmoothingSettings(timeout, 60, 2, timeout));

		long start = System.nanoTime();
		handler.process(tagList);
		long sum = System.nanoTime() - start;
		Assert.assertTrue("Expected " + expectedTime + "ms, but was " + (sum / NANO_TO_MILLI_FACTOR) + "ms", sum / NANO_TO_MILLI_FACTOR < expectedTime);
	}
}
