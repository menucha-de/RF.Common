package havis.device.rf.common.tagsmooth;

import havis.device.rf.configuration.TagSmoothingSettings;
import havis.device.rf.exception.ParameterException;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.TagDataList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class TagSmoothingHandler {

	/**
	 * State of the TagSmoothingHandler
	 */
	private final boolean enabled;

	/**
	 * Tag smoothing glimpsed timeout which is defined for this reader
	 */
	private final Integer glimpsedTimeout;

	/**
	 * Tag smoothing observed time threshold which is defined for this reader
	 */
	private final Integer observedTimeThreshold;

	/**
	 * Tag smoothing observed count threshold which is defined for this reader
	 */
	private final Integer observedCountThreshold;

	/**
	 * Tag smoothing lost timeout which is defined for this reader
	 */
	private final Integer lostTimeout;

	/**
	 * Set of observed TagSmoothingEntries
	 */
	private LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> observedEntries = new LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry>();

	/**
	 * Set of glimpsed TagSmoothingEntries
	 */
	private LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> glimpsedEntries = new LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry>();

	public TagSmoothingHandler(TagSmoothingSettings settings)
			throws ParameterException {
		if (settings.getObservedCountThreshold() == null
				&& settings.getObservedTimeThreshold() == null)
			throw new ParameterException(
					"Either property Observed Count Threshold or Observed Time"
							+ "Threshold must be set when using Tag Smoothing!");

		this.enabled = settings.isEnabled();
		this.glimpsedTimeout = settings.getGlimpsedTimeout();
		this.observedTimeThreshold = settings.getObservedTimeThreshold();
		this.observedCountThreshold = settings.getObservedCountThreshold();
		this.lostTimeout = settings.getLostTimeout();
	}

	// Disabling default contructor
	@SuppressWarnings("unused")
	private TagSmoothingHandler() {
		this.enabled = false;
		this.glimpsedTimeout = null;
		this.observedTimeThreshold = null;
		this.observedCountThreshold = null;
		this.lostTimeout = null;
	}

	/**
	 * Moves existing observed entry to the head of the map, otherwise adds an
	 * entry as unobserved Moving the existing entry to the head of the map is
	 * done out of preparation for {@link removeExpiredEntries}
	 * 
	 * @param entry
	 *            the entry which is to update
	 * @param entries
	 *            the map which contains the entry to update
	 * @return the processed entry
	 */
	private TagSmoothingEntry addOrMoveToEnd(TagSmoothingEntry entry,
			LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> entries) {
		// TODO add link to function in javadoc
		synchronized (entries) {
			TagSmoothingEntry element = entries.remove(entry);
			if (element != null) {
				entries.put(element, element);
				return element;
			} else {
				entries.put(entry, entry);
				return entry;
			}
		}
	}

	/**
	 * @return a list of tags which are currently marked as observed
	 */
	public List<TagData> getResultList() {
		List<TagData> result = new ArrayList<TagData>();
		for (Iterator<TagSmoothingEntry> it = observedEntries.values()
				.iterator(); it.hasNext();) {
			result.add(it.next().getTag());
		}
		return result;
	}

	/**
	 * @return true if the handler is active, false otherwise
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Checks that the given entry is to remove
	 * 
	 * @param entry
	 *            the entry to check for removal
	 * @return true if the tag can be removed, false otherwise
	 */
	private boolean isExpired(TagSmoothingEntry entry) {
		if (entry.isObserved()) {
			return this.lostTimeout != null
					&& (System.currentTimeMillis() - entry.getLastSeen()) > this.lostTimeout
							.longValue();
		} else {
			return this.glimpsedTimeout != null
					&& (System.currentTimeMillis() - entry.getLastSeen()) > this.glimpsedTimeout
							.longValue();
		}
	}

	/**
	 * Processes a list of given tags and updates existing entries, when the
	 * list is empty, only the existing entries are updated
	 * 
	 * @param tagList
	 *            the list of tags which is to process
	 */
	public void process(TagDataList tagList) {
		List<TagData> tagsList = tagList.getEntryList();
		if (!tagsList.isEmpty()) {
			for (TagData tag : tagsList) {
				process(tag);
			}
		}

		removeExpiredEntries();
	}

	/**
	 * Processes the given tag and updates existing entries. It also check a
	 * glimpsed entry for exceeding one of both thresholds and marking it as
	 * observed then.
	 * 
	 * @param tag
	 *            the tag which is to process
	 */
	private void process(TagData tag) {
		TagSmoothingEntry entry = new TagSmoothingEntry(tag);

		if (observedEntries.containsKey(entry)) {
			entry = addOrMoveToEnd(entry, observedEntries);
		} else {
			entry = addOrMoveToEnd(entry, glimpsedEntries);
		}

		entry.seen();

		if (!entry.isObserved()) {
			if (((this.observedCountThreshold != null) && (entry.getSeenCount() > this.observedCountThreshold
					.intValue()))
					|| ((this.observedTimeThreshold != null) && ((System
							.currentTimeMillis() - entry.getFirstSeen()) > this.observedTimeThreshold
							.longValue()))) {
				setObserved(entry);
			}
		}

		updateTagOfEntry(tag, entry);
	}

	/**
	 * Cleans up all expired entries, regardless if it is a glimpsed or a
	 * observed one
	 */
	private void removeExpiredEntries() {
		removeExpiredEntries(glimpsedEntries);
		removeExpiredEntries(observedEntries);
	}

	/**
	 * Updates the given set of entries, removes entries when they are seasoned
	 * Removes the invalid entries at top of the map, stops on first valid entry
	 * Map is sorted by a addOrMoveToEnd for performance
	 * 
	 * @param entries
	 *            the set of entries which is to update
	 */
	private void removeExpiredEntries(
			LinkedHashMap<TagSmoothingEntry, TagSmoothingEntry> entries) {
		// TODO add link to function in javadoc
		synchronized (entries) {
			while (true) {
				Iterator<TagSmoothingEntry> iterator = entries.values()
						.iterator();
				TagSmoothingEntry entry;
				if (iterator.hasNext() && (entry = iterator.next()) != null
						&& isExpired(entry)) {
					iterator.remove();
				} else {
					break;
				}
			}
		}
	}

	/**
	 * Marks the given entry as observed and moves it from the unobserved to the
	 * observed list
	 * 
	 * @param entry
	 *            the entry which should be set as observed
	 */
	private void setObserved(TagSmoothingEntry entry) {
		glimpsedEntries.remove(entry);
		entry.setObserved(true);
		addOrMoveToEnd(entry, observedEntries);
	}

	/**
	 * Updates the tag information of an existing TagSmoothingEntry with that of
	 * the new seen Tag instance. The epc is not to update.
	 * 
	 * @param newTag
	 *            the newer version of the tag which belongs to "entry"
	 * @param entry
	 *            the TagSmoothingEntry which is to update
	 */
	private void updateTagOfEntry(TagData newTag, TagSmoothingEntry entry) {
		TagData existingTag = entry.getTag();
		existingTag.setAntennaID(newTag.getAntennaID());
		existingTag.setChannel(newTag.getChannel());
		existingTag.setCrc(newTag.getCrc());
		existingTag.setPc(newTag.getPc());
		existingTag.setResultList(newTag.getResultList());
		existingTag.setRssi(newTag.getRssi());
		existingTag.setTagDataId(newTag.getTagDataId());
		existingTag.setXpc(newTag.getXpc());
	}
}
