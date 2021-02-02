package havis.device.rf.common.tagsmooth;

import havis.device.rf.tag.TagData;

public class TagSmoothingEntry {

	private boolean isObserved;
	private int seenCount;
	private long firstSeen;
	private long lastSeen;
	private TagData tag;

	public TagSmoothingEntry(TagData tag) {
		seenCount = 0;
		firstSeen = System.currentTimeMillis();
		lastSeen = firstSeen;

		this.tag = tag;

		isObserved = false;
	}

	public long getLastSeen() {
		return lastSeen;
	}

	public boolean isObserved() {
		return isObserved;
	}

	public void seen() {
		seenCount++;
		lastSeen = System.currentTimeMillis();
	}

	public long getFirstSeen() {
		return firstSeen;
	}

	public int getSeenCount() {
		return seenCount;
	}

	public void setObserved(boolean isObserved) {
		this.isObserved = isObserved;
	}

	public TagData getTag() {
		return tag;
	}

	public void setTag(TagData tag) {
		this.tag = tag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + java.util.Arrays.hashCode(tag.getEpc());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}

		TagSmoothingEntry t = (TagSmoothingEntry) obj;
		TagData other = t.getTag();

		if (tag == other)
			return true;
		if (other == null)
			return false;
		if (!java.util.Arrays.equals(tag.getEpc(), other.getEpc()))
			return false;
		return true;
	}
}
