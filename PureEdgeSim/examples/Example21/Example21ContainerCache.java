package examples;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A least-recently-used cache of application containers, bounded by a storage
 * capacity in megabytes.
 * <p>
 * The cache is the piece of the storage budget that a node dedicates to
 * containers. Looking up an application counts as a request: a present
 * container is a hit and becomes the most recently used one, a missing
 * container is a miss. Storing a container that does not fit evicts the least
 * recently used entries until it does, and a container larger than the whole
 * cache is refused instead of flushing every other entry.
 */
public class Example21ContainerCache {

	private final double capacityMB;
	// Access order keeps the least recently used container first.
	private final Map<Integer, Double> containers = new LinkedHashMap<>(16, 0.75f, true);
	private double usedMB;
	private int hits;
	private int misses;
	private int evictions;
	private int rejected;

	/**
	 * Creates a cache with the given capacity.
	 *
	 * @param capacityMB the storage dedicated to containers, in megabytes.
	 */
	public Example21ContainerCache(double capacityMB) {
		this.capacityMB = Math.max(0, capacityMB);
	}

	/**
	 * Looks an application up and records the request as a hit or a miss.
	 *
	 * @param applicationId the application identifier.
	 * @return true when the container is in the cache.
	 */
	public boolean lookup(int applicationId) {
		// get(), unlike containsKey(), refreshes the access order.
		if (containers.get(applicationId) != null) {
			hits++;
			return true;
		}
		misses++;
		return false;
	}

	/**
	 * Tells whether a container is cached without counting a request.
	 *
	 * @param applicationId the application identifier.
	 * @return true when the container is in the cache.
	 */
	public boolean contains(int applicationId) {
		return containers.containsKey(applicationId);
	}

	/**
	 * Stores or refreshes a container, evicting the least recently used entries
	 * until it fits.
	 *
	 * @param applicationId the application identifier.
	 * @param sizeMB        the container size, in megabytes.
	 * @return true when the container is cached, false when it is larger than the
	 *         whole cache.
	 */
	public boolean store(int applicationId, double sizeMB) {
		if (sizeMB > capacityMB) {
			rejected++;
			return false;
		}
		Double previous = containers.remove(applicationId);
		if (previous != null)
			usedMB -= previous;
		while (usedMB + sizeMB > capacityMB && !containers.isEmpty())
			evictLeastRecentlyUsed();
		containers.put(applicationId, sizeMB);
		usedMB += sizeMB;
		return true;
	}

	private void evictLeastRecentlyUsed() {
		Integer eldest = containers.keySet().iterator().next();
		usedMB -= containers.remove(eldest);
		evictions++;
	}

	/** The storage dedicated to containers, in megabytes. */
	public double getCapacityMB() {
		return capacityMB;
	}

	/** The storage currently used by cached containers, in megabytes. */
	public double getUsedStorageMB() {
		return usedMB;
	}

	/** The number of containers currently cached. */
	public int size() {
		return containers.size();
	}

	/** The number of lookups served from the cache. */
	public int getHits() {
		return hits;
	}

	/** The number of lookups that had to download the container. */
	public int getMisses() {
		return misses;
	}

	/** The number of containers removed to make room. */
	public int getEvictions() {
		return evictions;
	}

	/** The number of containers refused because they exceed the whole cache. */
	public int getRejected() {
		return rejected;
	}

	/** The share of lookups served from the cache, or zero before the first one. */
	public double getHitRatio() {
		int requests = hits + misses;
		return requests == 0 ? 0 : (double) hits / requests;
	}
}
