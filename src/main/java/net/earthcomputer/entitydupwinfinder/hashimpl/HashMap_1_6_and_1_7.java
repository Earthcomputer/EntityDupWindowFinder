package net.earthcomputer.entitydupwinfinder.hashimpl;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

// See http://hg.openjdk.java.net/jdk6/jdk6/jdk/file/ac1d168048bd/src/share/classes/java/util/HashMap.java
public class HashMap_1_6_and_1_7<K, V> implements Map<K, V> {

	private static final int INITIAL_CAPACITY = 16;
	private static final int MAXIMUM_CAPACITY = 1 << 30;
	private static final float LOAD_FACTOR = 0.75f;

	private transient Entry<K, V>[] table;
	private transient int size;
	private int nextResize;
	private transient int modCount;

	@SuppressWarnings("unchecked")
	public HashMap_1_6_and_1_7() {
		nextResize = (int) (INITIAL_CAPACITY * LOAD_FACTOR);
		table = new Entry[INITIAL_CAPACITY];
	}

	private static int hash(int h) {
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}

	private static int indexFor(int h, int length) {
		return h & (length - 1);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public V get(Object key) {
		Entry<K, V> entry = getEntry(key);
		return entry == null ? null : entry.value;
	}

	@Override
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	private Entry<K, V> getEntry(Object key) {
		if (key == null) {
			Entry<K, V> entry = table[0];
			while (entry != null) {
				if (entry.key == null) {
					return entry;
				}
				entry = entry.next;
			}
			return null;
		} else {
			int hash = hash(key.hashCode());
			Entry<K, V> entry = table[indexFor(hash, table.length)];
			while (entry != null) {
				if (entry.hash != hash) {
					entry = entry.next;
					continue;
				}
				Object entryKey = entry.key;
				if (entryKey == key || key.equals(entryKey)) {
					return entry;
				}
				entry = entry.next;
			}
			return null;
		}
	}

	@Override
	public V put(K key, V value) {
		Entry<K, V> entry = getEntry(key);
		if (entry != null) {
			V oldValue = entry.value;
			entry.value = value;
			return oldValue;
		} else {
			modCount++;
			int hash = key == null ? 0 : hash(key.hashCode());
			int index = key == null ? 0 : indexFor(hash, table.length);
			addEntry(hash, key, value, index);
			return null;
		}
	}

	private void resize(int newCapacity) {
		Entry<K, V>[] oldTable = table;
		int oldCapacity = oldTable.length;
		if (oldCapacity == MAXIMUM_CAPACITY) {
			nextResize = Integer.MAX_VALUE;
			return;
		}

		@SuppressWarnings("unchecked")
		Entry<K, V>[] newTable = new Entry[newCapacity];
		transfer(newTable);
		table = newTable;
		nextResize = (int) (newCapacity * LOAD_FACTOR);
	}

	private void transfer(Entry<K, V>[] newTable) {
		Entry<K, V>[] src = table;
		int newCapacity = newTable.length;
		for (int i = 0; i < src.length; i++) {
			Entry<K, V> entry = src[i];
			if (entry != null) {
				src[i] = null;
				do {
					Entry<K, V> nextEntry = entry.next;
					int index = indexFor(entry.hash, newCapacity);
					entry.next = newTable[index];
					newTable[index] = entry;
					entry = nextEntry;
				} while (entry != null);
			}
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		int otherMapSize = map.size();
		if (otherMapSize == 0) {
			return;
		}

		// don't really understand why this is otherMapSize and not otherMapSize
		// + size, but we have to go with the exact same algorithm or we won't
		// get the same results
		if (otherMapSize > nextResize) {
			int targetCapacity = (int) (otherMapSize / LOAD_FACTOR + 1);
			if (targetCapacity > MAXIMUM_CAPACITY) {
				targetCapacity = MAXIMUM_CAPACITY;
			}
			int newCapacity = table.length;
			while (newCapacity < targetCapacity) {
				newCapacity <<= 1;
			}
			if (newCapacity > table.length) {
				resize(newCapacity);
			}
		}

		Iterator<?> itr = map.entrySet().iterator();
		while (itr.hasNext()) {
			@SuppressWarnings("unchecked")
			Map.Entry<? extends K, ? extends V> entry = (Map.Entry<? extends K, ? extends V>) itr.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V remove(Object key) {
		Entry<K, V> entry = removeEntry(key);
		return entry == null ? null : entry.value;
	}

	private Entry<K, V> removeEntry(Object key) {
		int hash = key == null ? 0 : hash(key.hashCode());
		int index = indexFor(hash, table.length);
		Entry<K, V> previous = null;
		Entry<K, V> entry = table[index];

		while (entry != null) {
			Entry<K, V> next = entry.next;
			realLoopBody: {
				if (entry.hash != hash) {
					break realLoopBody;
				}
				Object entryKey = entry.key;
				if (entryKey == key || (key != null && key.equals(entryKey))) {
					modCount++;
					size--;
					if (previous == null) {
						table[index] = next;
					} else {
						previous.next = next;
					}
					return entry;
				}
			}
			previous = entry;
			entry = next;
		}

		return entry;
	}

	@Override
	public void clear() {
		modCount++;
		Arrays.fill(table, null);
		size = 0;
	}

	@Override
	public boolean containsValue(Object value) {
		Entry<K, V>[] table = this.table;
		for (int i = 0; i < table.length; i++) {
			Entry<K, V> entry = table[i];
			while (entry != null) {
				if (value == null ? entry.value == null : value.equals(entry.value)) {
					return true;
				}
				entry = entry.next;
			}
		}
		return false;
	}

	private static class Entry<K, V> implements Map.Entry<K, V> {

		K key;
		V value;
		Entry<K, V> next;
		int hash;

		public Entry(K key, V value, Entry<K, V> next, int hash) {
			this.key = key;
			this.value = value;
			this.next = next;
			this.hash = hash;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V oldValue = value;
			this.value = value;
			return oldValue;
		}

		@Override
		public int hashCode() {
			return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Entry))
				return false;
			@SuppressWarnings("unchecked")
			Entry<K, V> other = (Entry<K, V>) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}

	}

	private void addEntry(int hash, K key, V value, int tableIndex) {
		Entry<K, V> entry = table[tableIndex];
		table[tableIndex] = new Entry<K, V>(key, value, entry, hash);
		size++;
		if (size >= nextResize) {
			resize(table.length << 1);
		}
	}

	private abstract class HashItr<E> implements Iterator<E> {
		Entry<K, V> next;
		int expectedModCount;
		int index;
		Entry<K, V> current;

		HashItr() {
			expectedModCount = modCount;
			if (size != 0) {
				Entry<K, V>[] table = HashMap_1_6_and_1_7.this.table;
				while (index < table.length) {
					next = table[index];
					index++;
					if (next != null) {
						break;
					}
				}
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		Entry<K, V> nextEntry() {
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			Entry<K, V> entry = next;
			if (entry == null) {
				throw new NoSuchElementException();
			}

			next = entry.next;
			if (next == null) {
				Entry<K, V>[] table = HashMap_1_6_and_1_7.this.table;
				while (index < table.length) {
					next = table[index];
					index++;
					if (next != null) {
						break;
					}
				}
			}
			current = entry;
			return entry;
		}

		@Override
		public void remove() {
			if (current == null) {
				throw new IllegalStateException();
			}
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			Object currentKey = current.key;
			current = null;
			removeEntry(currentKey);
			expectedModCount = modCount;
		}
	}

	private class ValueItr extends HashItr<V> {
		@Override
		public V next() {
			return nextEntry().value;
		}
	}

	private class KeyItr extends HashItr<K> {
		@Override
		public K next() {
			return nextEntry().key;
		}
	}

	private class EntryItr extends HashItr<Map.Entry<K, V>> {
		@Override
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
	}

	private transient Set<K> cachedKeySet = null;
	private transient Collection<V> cachedValues = null;
	private transient Set<Map.Entry<K, V>> cachedEntrySet = null;

	@Override
	public Set<K> keySet() {
		return cachedKeySet == null ? (cachedKeySet = new KeySet()) : cachedKeySet;
	}

	@Override
	public Collection<V> values() {
		return cachedValues == null ? (cachedValues = new Values()) : cachedValues;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return cachedEntrySet == null ? (cachedEntrySet = new EntrySet()) : cachedEntrySet;
	}

	private class KeySet extends AbstractSet<K> {
		@Override
		public Iterator<K> iterator() {
			return new KeyItr();
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}

		@Override
		public boolean remove(Object o) {
			return removeEntry(o) != null;
		}

		@Override
		public void clear() {
			HashMap_1_6_and_1_7.this.clear();
		}
	}

	private class Values extends AbstractCollection<V> {
		@Override
		public Iterator<V> iterator() {
			return new ValueItr();
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}

		@Override
		public void clear() {
			HashMap_1_6_and_1_7.this.clear();
		}
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryItr();
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry)) {
				return false;
			}
			@SuppressWarnings("unchecked")
			Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
			Entry<K, V> existingEntry = getEntry(entry.getKey());
			return existingEntry != null && existingEntry.equals(entry);
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(Object o) {
			if (!contains(o)) {
				return false;
			}
			removeEntry(((Map.Entry<K, V>) o).getKey());
			return true;
		}

		@Override
		public void clear() {
			HashMap_1_6_and_1_7.this.clear();
		}
	}

}
