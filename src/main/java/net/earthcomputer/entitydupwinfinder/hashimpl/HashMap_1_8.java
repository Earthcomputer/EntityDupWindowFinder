package net.earthcomputer.entitydupwinfinder.hashimpl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

// See http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/util/HashMap.java
public class HashMap_1_8<K, V> implements Map<K, V> {

	private static final int INITIAL_CAPACITY = 16;
	private static final int MAXIMUM_CAPACITY = 1 << 30;
	private static final float LOAD_FACTOR = 0.75f;
	private static final int TREEIFY_THRESHOLD = 8;
	private static final int UNTREEIFY_THRESHOLD = 6;
	private static final int MIN_TREEIFY_CAPACITY = 64;

	private static class Node<K, V> implements Map.Entry<K, V> {
		K key;
		V value;
		Node<K, V> next;
		int hash;

		public Node(K key, V value, Node<K, V> next, int hash) {
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
		public String toString() {
			return key + "=" + value;
		}

		@Override
		public int hashCode() {
			return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
		}

		@Override
		public V setValue(V newValue) {
			V oldValue = value;
			value = newValue;
			return oldValue;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Node))
				return false;
			@SuppressWarnings("unchecked")
			Node<K, V> other = (Node<K, V>) obj;
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
	}

	private static final int hash(Object key) {
		if (key == null) {
			return 0;
		}
		int h = key.hashCode();
		return h ^ (h >>> 16);
	}

	private static Class<?> comparableClassFor(Object x) {
		if (!(x instanceof Comparable)) {
			return null;
		}
		Class<?> clazz = x.getClass();
		if (clazz == String.class) {
			return clazz;
		}
		Type[] genericInterfaces = clazz.getGenericInterfaces();
		if (genericInterfaces != null) {
			for (int i = 0; i < genericInterfaces.length; i++) {
				Type genericInterface = genericInterfaces[i];
				if (genericInterface instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
					if (parameterizedType.getRawType() == Comparable.class) {
						Type[] typeArguments = parameterizedType.getActualTypeArguments();
						if (typeArguments != null && typeArguments.length == 1 && typeArguments[0] == clazz) {
							return clazz;
						}
					}
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static int compareComparables(Class<?> keyClass, Object key, Object x) {
		return x == null || x.getClass() != keyClass ? 0 : ((Comparable<Object>) key).compareTo(x);
	}

	private static final int tableSizeFor(int capacity) {
		int n = capacity - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return n < 0 ? 1 : (n >= MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : n + 1);
	}

	private transient Node<K, V>[] table;
	private transient int size;
	private transient int modCount;
	private int nextResize;

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
		Node<K, V> node = getNode(hash(key), key);
		return node == null ? null : node.value;
	}

	private Node<K, V> getNode(int hash, Object key) {
		Node<K, V>[] table = this.table;
		Node<K, V> first, entry;
		int tableSize = table == null ? 0 : table.length;
		K currentKey;

		if (tableSize > 0) {
			first = table[(tableSize - 1) & hash];
			if (first != null) {
				if (first.hash == hash) {
					currentKey = first.key;
					if (currentKey == key || (key != null && key.equals(currentKey))) {
						return first;
					}
				}
				entry = first.next;
				if (entry != null) {
					if (first instanceof TreeNode) {
						return ((TreeNode<K, V>) first).getTreeNode(hash, key);
					}
					do {
						if (entry.hash == hash) {
							currentKey = entry.key;
							if (currentKey == key || (key != null && key.equals(currentKey))) {
								return entry;
							}
						}
					} while ((entry = entry.next) != null);
				}
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(Object key) {
		return getNode(hash(key), key) != null;
	}

	@Override
	public V put(K key, V value) {
		return putVal(hash(key), key, value, false);
	}

	private V putVal(int hash, K key, V value, boolean onlyIfAbsent) {
		Node<K, V>[] table = this.table;
		if (table == null || table.length == 0) {
			table = resize();
		}
		int tableSize = table.length;
		int index = (tableSize - 1 & hash);
		Node<K, V> previous = table[index];
		if (previous == null) {
			table[index] = new Node<K, V>(key, value, null, hash);
		} else {
			Node<K, V> entry;
			K currentKey = previous.key;
			if (previous.hash == hash && (currentKey == key || (key != null && key.equals(currentKey)))) {
				entry = previous;
			} else if (previous instanceof TreeNode) {
				entry = ((TreeNode<K, V>) previous).putTreeVal(table, hash, key, value);
			} else {
				for (int binCount = 0;; binCount++) {
					entry = previous.next;
					if (entry == null) {
						previous.next = new Node<K, V>(key, value, null, hash);
						if (binCount >= TREEIFY_THRESHOLD - 1) {
							treeifyBin(table, hash);
						}
						break;
					}
					currentKey = entry.key;
					if (entry.hash == hash && (currentKey == key || (key != null && key.equals(currentKey)))) {
						break;
					}
					previous = entry;
				}
			}
			if (entry != null) {
				V oldValue = entry.value;
				if (!onlyIfAbsent || oldValue == null) {
					entry.value = value;
				}
				return oldValue;
			}
		}
		modCount++;
		size++;
		if (size > nextResize) {
			resize();
		}
		return null;
	}

	private Node<K, V>[] resize() {
		Node<K, V>[] oldTable = this.table;
		int oldCapacity = oldTable == null ? 0 : oldTable.length;
		int oldNextResize = nextResize;
		int newCapacity;
		int newNextResize = 0;
		if (oldCapacity > 0) {
			if (oldCapacity >= MAXIMUM_CAPACITY) {
				nextResize = Integer.MAX_VALUE;
				return oldTable;
			}
			newCapacity = oldCapacity << 1;
			if (newCapacity < MAXIMUM_CAPACITY && oldCapacity >= INITIAL_CAPACITY) {
				newNextResize = oldNextResize << 1;
			}
		} else if (oldNextResize > 0) {
			newCapacity = oldNextResize;
		} else {
			newCapacity = INITIAL_CAPACITY;
			newNextResize = (int) (LOAD_FACTOR * INITIAL_CAPACITY);
		}
		if (newNextResize == 0) {
			float newNextResizeFloat = newCapacity * LOAD_FACTOR;
			newNextResize = newCapacity < MAXIMUM_CAPACITY && newNextResizeFloat < MAXIMUM_CAPACITY
					? (int) newNextResizeFloat : Integer.MAX_VALUE;
		}
		nextResize = newNextResize;
		@SuppressWarnings("unchecked")
		Node<K, V>[] newTable = new Node[newCapacity];
		table = newTable;
		if (oldTable != null) {
			for (int index = 0; index < oldCapacity; index++) {
				Node<K, V> entry = oldTable[index];
				if (entry != null) {
					oldTable[index] = null;
					if (entry.next == null) {
						newTable[entry.hash & (newCapacity - 1)] = entry;
					} else if (entry instanceof TreeNode) {
						((TreeNode<K, V>) entry).split(newTable, index, oldCapacity);
					} else {
						Node<K, V> loHead = null;
						Node<K, V> loTail = null;
						Node<K, V> hiHead = null;
						Node<K, V> hiTail = null;
						Node<K, V> next;
						do {
							next = entry.next;
							if ((entry.hash & oldCapacity) == 0) {
								if (loTail == null) {
									loHead = entry;
								} else {
									loTail.next = entry;
								}
								loTail = entry;
							} else {
								if (hiTail == null) {
									hiHead = entry;
								} else {
									hiTail.next = entry;
								}
								hiTail = entry;
							}
						} while ((entry = next) != null);
						if (loTail != null) {
							loTail.next = null;
							newTable[index] = loHead;
						}
						if (hiTail != null) {
							hiTail.next = null;
							newTable[index + oldCapacity] = hiHead;
						}
					}
				}
			}
		}
		return newTable;
	}

	private void treeifyBin(Node<K, V>[] table, int hash) {
		if (table == null) {
			resize();
			return;
		}
		int tableSize = table.length;
		if (tableSize < MIN_TREEIFY_CAPACITY) {
			resize();
			return;
		}

		int index = (tableSize - 1) & hash;
		Node<K, V> entry = table[index];
		if (entry != null) {
			TreeNode<K, V> head = null, tail = null;
			do {
				TreeNode<K, V> node = new TreeNode<K, V>(entry.key, entry.value, null, entry.hash);
				if (tail == null) {
					head = node;
				} else {
					node.prev = tail;
					tail.next = node;
				}
				tail = node;
			} while ((entry = entry.next) != null);
			table[index] = head;
			if (head != null) {
				head.treeify(table);
			}
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		int size = map.size();
		if (size > 0) {
			if (table == null) {
				float capacityFloat = ((float) size / LOAD_FACTOR) + 1.0F;
				int capacity = ((capacityFloat < (float) MAXIMUM_CAPACITY) ? (int) capacityFloat : MAXIMUM_CAPACITY);
				if (capacity > nextResize)
					nextResize = tableSizeFor(capacity);
			} else if (size > nextResize) {
				resize();
			}
			for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
				K key = entry.getKey();
				V value = entry.getValue();
				putVal(hash(key), key, value, false);
			}
		}
	}

	@Override
	public V remove(Object key) {
		Node<K, V> node = removeNode(hash(key), key, null, false, true);
		return node == null ? null : node.value;
	}

	private Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
		Node<K, V>[] table = this.table;
		if (table == null) {
			return null;
		}
		int tableSize = table.length;
		if (tableSize == 0) {
			return null;
		}
		int index = (tableSize - 1) & hash;
		Node<K, V> previous = table[index];
		if (previous == null) {
			return null;
		}
		Node<K, V> node = null;
		Node<K, V> entry = previous.next;
		K currentKey = previous.key;
		V currentValue;

		if (previous.hash == hash && (currentKey == key || (key != null && key.equals(currentKey)))) {
			node = previous;
		} else if (entry != null) {
			if (previous instanceof TreeNode) {
				node = ((TreeNode<K, V>) previous).getTreeNode(hash, key);
			} else {
				do {
					currentKey = entry.key;
					if (entry.hash == hash && (currentKey == key || (key != null && key.equals(currentKey)))) {
						node = entry;
						break;
					}
					previous = entry;
				} while ((entry = entry.next) != null);
			}
		}
		currentValue = node == null ? null : node.value;
		if (node != null && (!matchValue || currentValue == value || (value != null && value.equals(currentValue)))) {
			if (node instanceof TreeNode) {
				((TreeNode<K, V>) node).removeTreeNode(table, movable);
			} else if (node == previous) {
				table[index] = node.next;
			} else {
				previous.next = node.next;
			}
			modCount++;
			size--;
			return node;
		}
		return null;
	}

	@Override
	public void clear() {
		modCount++;
		if (table != null) {
			size = 0;
			Arrays.fill(table, null);
		}
	}

	@Override
	public boolean containsValue(Object value) {
		Node<K, V>[] table = this.table;
		if (table == null || table.length == 0) {
			return false;
		}
		V currentValue;
		for (int index = 0; index < table.length; index++) {
			Node<K, V> entry = table[index];
			while (entry != null) {
				currentValue = entry.value;
				if (currentValue == value || (value != null && value.equals(currentValue))) {
					return true;
				}
				entry = entry.next;
			}
		}
		return false;
	}

	private Set<K> cachedKeySet;
	private Collection<V> cachedValues;
	private Set<Map.Entry<K, V>> cachedEntrySet;

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
		public int size() {
			return size;
		}

		@Override
		public void clear() {
			HashMap_1_8.this.clear();
		}

		@Override
		public Iterator<K> iterator() {
			return new KeyItr();
		}

		@Override
		public boolean contains(Object key) {
			return containsKey(key);
		}

		@Override
		public boolean remove(Object key) {
			return removeNode(hash(key), key, null, false, true) != null;
		}
	}

	private class Values extends AbstractCollection<V> {
		@Override
		public int size() {
			return size;
		}

		@Override
		public void clear() {
			HashMap_1_8.this.clear();
		}

		@Override
		public Iterator<V> iterator() {
			return new ValueItr();
		}

		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public int size() {
			return size;
		}

		@Override
		public void clear() {
			HashMap_1_8.this.clear();
		}

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryItr();
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry)) {
				return false;
			}
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
			Object key = entry.getKey();
			Node<K, V> existingNode = getNode(hash(key), key);
			return existingNode != null && existingNode.equals(entry);
		}

		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry)) {
				return false;
			}
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
			Object key = entry.getKey();
			Object value = entry.getValue();
			return removeNode(hash(key), key, value, true, true) != null;
		}
	}

	abstract class HashItr {
		Node<K, V> next;
		Node<K, V> current;
		int expectedModCount;
		int index;

		HashItr() {
			expectedModCount = modCount;
			Node<K, V>[] table = HashMap_1_8.this.table;
			current = next = null;
			index = 0;
			if (table != null && size != 0) {
				while (index < table.length) {
					next = table[index];
					index++;
					if (next != null) {
						break;
					}
				}
			}
		}

		public boolean hasNext() {
			return next != null;
		}

		Node<K, V> nextNode() {
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			Node<K, V> entry = next;
			if (entry == null) {
				throw new NoSuchElementException();
			}
			current = entry;
			next = entry.next;
			Node<K, V>[] table = HashMap_1_8.this.table;
			if (next == null && table != null) {
				while (index < table.length) {
					next = table[index];
					index++;
					if (next != null) {
						break;
					}
				}
			}
			return entry;
		}

		public void remove() {
			Node<K, V> previous = current;
			if (previous == null) {
				throw new IllegalStateException();
			}
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			current = null;
			K key = previous.key;
			removeNode(hash(key), key, null, false, false);
			expectedModCount = modCount;
		}
	}

	private class KeyItr extends HashItr implements Iterator<K> {
		@Override
		public K next() {
			return nextNode().key;
		}
	}

	private class ValueItr extends HashItr implements Iterator<V> {
		@Override
		public V next() {
			return nextNode().value;
		}
	}

	private class EntryItr extends HashItr implements Iterator<Map.Entry<K, V>> {
		@Override
		public Map.Entry<K, V> next() {
			return nextNode();
		}
	}

	private static class TreeNode<K, V> extends Node<K, V> {
		// HashMap.TreeNode fields
		TreeNode<K, V> parent;
		TreeNode<K, V> left;
		TreeNode<K, V> right;
		TreeNode<K, V> prev;
		boolean red;

		TreeNode(K key, V value, Node<K, V> next, int hash) {
			super(key, value, next, hash);
		}

		TreeNode<K, V> root() {
			TreeNode<K, V> node = this;
			TreeNode<K, V> parent;
			while (true) {
				parent = node.parent;
				if (parent == null) {
					return node;
				}
				node = parent;
			}
		}

		static <K, V> void moveRootToFront(Node<K, V>[] table, TreeNode<K, V> root) {
			if (root == null) {
				return;
			}
			int tableSize = table == null ? 0 : table.length;
			if (tableSize == 0) {
				return;
			}
			int index = (tableSize - 1) & root.hash;
			TreeNode<K, V> first = (TreeNode<K, V>) table[index];
			if (root != first) {
				Node<K, V> rootNext = root.next;
				table[index] = root;
				TreeNode<K, V> rootPrev = root.prev;
				if (rootNext != null) {
					((TreeNode<K, V>) rootNext).prev = rootPrev;
				}
				if (rootPrev != null) {
					rootPrev.next = rootNext;
				}
				if (first != null) {
					first.prev = root;
				}
				root.next = first;
				root.prev = null;
			}
			assert checkInvariants(root);
		}

		TreeNode<K, V> find(int hash, Object key, Class<?> keyClass) {
			TreeNode<K, V> node = this;
			do {
				int nodeHash = node.hash;
				K nodeKey = node.key;
				int compareResult;
				TreeNode<K, V> nodeLeft = node.left;
				TreeNode<K, V> nodeRight = node.right;
				TreeNode<K, V> subNode;
				if (nodeHash > hash) {
					node = nodeLeft;
				} else if (nodeHash < hash) {
					node = nodeRight;
				} else if (nodeKey == key || (key != null && key.equals(nodeKey))) {
					return node;
				} else if (nodeLeft == null) {
					node = nodeRight;
				} else if (nodeRight == null) {
					node = nodeLeft;
				} else if ((keyClass != null || (keyClass = comparableClassFor(key)) != null)) {
					compareResult = compareComparables(keyClass, key, nodeKey);
					if (compareResult != 0) {
						node = compareResult < 0 ? nodeLeft : nodeRight;
					}
				} else if ((subNode = nodeRight.find(hash, key, keyClass)) != null) {
					return subNode;
				} else {
					node = nodeLeft;
				}
			} while (node != null);
			return null;
		}

		TreeNode<K, V> getTreeNode(int hash, Object key) {
			TreeNode<K, V> root = parent != null ? root() : this;
			return root.find(hash, key, null);
		}

		static int tieBreakOrder(Object a, Object b) {
			int result = 0;
			if (a != null && b != null) {
				result = a.getClass().getName().compareTo(b.getClass().getName());
			}
			if (result == 0) {
				result = System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1;
			}
			return result;
		}

		void treeify(Node<K, V>[] table) {
			TreeNode<K, V> root = null;
			TreeNode<K, V> node = this;
			TreeNode<K, V> next;
			while (node != null) {
				next = (TreeNode<K, V>) node.next;
				node.left = node.right = null;
				if (root == null) {
					node.parent = null;
					node.red = false;
					root = node;
				} else {
					K nodeKey = node.key;
					int nodeHash = node.hash;
					Class<?> keyClass = null;
					TreeNode<K, V> currentNode = root;
					while (true) {
						int compareResult;
						int currentNodeHash = currentNode.hash;
						K currentNodeKey = currentNode.key;
						if (currentNodeHash > nodeHash) {
							compareResult = -1;
						} else if (currentNodeHash < nodeHash) {
							compareResult = 1;
						} else if ((keyClass == null && (keyClass = comparableClassFor(nodeKey)) == null)
								|| (compareResult = compareComparables(keyClass, nodeKey, currentNodeKey)) == 0) {
							compareResult = tieBreakOrder(nodeKey, currentNodeKey);
						}

						TreeNode<K, V> prevCurrentNode = currentNode;
						currentNode = compareResult <= 0 ? currentNode.left : currentNode.right;
						if (currentNode == null) {
							node.parent = prevCurrentNode;
							if (compareResult <= 0) {
								prevCurrentNode.left = node;
							} else {
								prevCurrentNode.right = node;
							}
							root = balanceInsertion(root, node);
							break;
						}
					}
				}
				node = next;
			}
			moveRootToFront(table, root);
		}

		Node<K, V> untreeify() {
			Node<K, V> head = null;
			Node<K, V> tail = null;
			Node<K, V> treeNode = this;
			while (treeNode != null) {
				Node<K, V> replacement = new Node<K, V>(treeNode.key, treeNode.value, null, treeNode.hash);
				if (tail == null) {
					head = replacement;
				} else {
					tail.next = replacement;
				}
				tail = replacement;
				treeNode = treeNode.next;
			}
			return head;
		}

		TreeNode<K, V> putTreeVal(Node<K, V>[] table, int hash, K key, V value) {
			Class<?> keyClass = null;
			boolean searched = false;
			TreeNode<K, V> root = parent != null ? root() : this;
			TreeNode<K, V> node = root;
			while (true) {
				int compareResult;
				int nodeHash = node.hash;
				K nodeKey = node.key;
				if (nodeHash > hash) {
					compareResult = -1;
				} else if (nodeHash < hash) {
					compareResult = 1;
				} else if (nodeKey == key || (key != null && key.equals(nodeKey))) {
					return node;
				} else {
					// This line is only needed to stop the compiler complaining
					compareResult = 0;
					boolean trySearch = keyClass == null && (keyClass = comparableClassFor(key)) == null;
					trySearch = trySearch || (compareResult = compareComparables(keyClass, key, nodeKey)) == 0;
					if (trySearch) {
						if (!searched) {
							searched = true;
							TreeNode<K, V> foundNode;
							TreeNode<K, V> childNode = node.left;
							if (childNode != null) {
								foundNode = childNode.find(hash, key, keyClass);
								if (foundNode != null) {
									return foundNode;
								}
							}
							childNode = node.right;
							if (childNode != null) {
								foundNode = childNode.find(hash, key, keyClass);
								if (foundNode != null) {
									return foundNode;
								}
							}
						}
						compareResult = tieBreakOrder(key, nodeKey);
					}
				}
				TreeNode<K, V> nodePrev = node;
				node = compareResult <= 0 ? node.left : node.right;
				if (node == null) {
					Node<K, V> nodePrevNext = nodePrev.next;
					TreeNode<K, V> newTreeNode = new TreeNode<K, V>(key, value, nodePrevNext, hash);
					if (compareResult <= 0) {
						nodePrev.left = newTreeNode;
					} else {
						nodePrev.right = newTreeNode;
					}
					nodePrev.next = newTreeNode;
					newTreeNode.parent = newTreeNode.prev = nodePrev;
					if (nodePrevNext != null) {
						((TreeNode<K, V>) nodePrevNext).prev = newTreeNode;
					}
					moveRootToFront(table, balanceInsertion(root, newTreeNode));
					return null;
				}
			}
		}

		void removeTreeNode(Node<K, V>[] table, boolean movable) {
			int tableSize = table == null ? 0 : table.length;
			if (tableSize == 0) {
				return;
			}
			int index = (tableSize - 1) & hash;
			TreeNode<K, V> first = (TreeNode<K, V>) table[index];
			TreeNode<K, V> root = first;
			TreeNode<K, V> rootLeft;
			TreeNode<K, V> succ = (TreeNode<K, V>) next;
			TreeNode<K, V> pred = prev;
			if (pred == null) {
				table[index] = first = succ;
			} else {
				pred.next = succ;
			}
			if (succ != null) {
				succ.prev = pred;
			}
			if (first == null) {
				return;
			}
			if (root.parent != null) {
				root = root();
			}
			rootLeft = root.left;
			if (root == null || root.right == null || rootLeft == null || rootLeft.left == null) {
				table[index] = first.untreeify();
				return;
			}

			TreeNode<K, V> node = this;
			TreeNode<K, V> nodeLeft = left;
			TreeNode<K, V> nodeRight = right;
			TreeNode<K, V> replacement;
			if (nodeLeft != null && nodeRight != null) {
				TreeNode<K, V> successor = nodeLeft;
				TreeNode<K, V> successorLeft = successor.left;
				while (successorLeft != null) {
					successor = successorLeft;
					successorLeft = successor.left;
				}
				boolean tmp = successor.red;
				successor.red = node.red;
				node.red = tmp;
				TreeNode<K, V> successorRight = successor.right;
				TreeNode<K, V> nodeParent = node.parent;
				if (successor == nodeRight) {
					node.parent = successor;
					successor.right = node;
				} else {
					TreeNode<K, V> successorParent = successor.parent;
					node.parent = successorParent;
					if (successorParent != null) {
						if (successor == successorParent.left) {
							successorParent.left = node;
						} else {
							successorParent.right = node;
						}
					}
					successor.right = nodeRight;
					if (nodeRight != null) {
						nodeRight.parent = successor;
					}
				}
				node.left = null;
				node.right = successorRight;
				if (successorRight != null) {
					successorRight.parent = node;
				}
				successor.left = nodeLeft;
				if (nodeLeft != null) {
					nodeLeft.parent = successor;
				}
				successor.parent = nodeParent;
				if (nodeParent == null) {
					root = successor;
				} else if (node == nodeParent.left) {
					nodeParent.left = successor;
				} else {
					nodeParent.right = successor;
				}
				if (successorRight != null) {
					replacement = successorRight;
				} else {
					replacement = node;
				}
			} else if (nodeLeft != null) {
				replacement = nodeLeft;
			} else if (nodeRight != null) {
				replacement = nodeRight;
			} else {
				replacement = node;
			}
			if (replacement != node) {
				TreeNode<K, V> nodeParent = replacement.parent = node.parent;
				if (nodeParent == null) {
					root = replacement;
				} else if (node == nodeParent.left) {
					nodeParent.left = replacement;
				} else {
					nodeParent.right = replacement;
				}
				node.left = node.right = node.parent = null;
			}

			TreeNode<K, V> realRoot = node.red ? root : balanceDeletion(root, replacement);
			if (replacement == node) {
				TreeNode<K, V> nodeParent = node.parent;
				node.parent = null;
				if (nodeParent != null) {
					if (node == nodeParent.left) {
						nodeParent.left = null;
					} else if (node == nodeParent.right) {
						nodeParent.right = null;
					}
				}
			}
			if (movable) {
				moveRootToFront(table, realRoot);
			}
		}

		void split(Node<K, V>[] table, int index, int bit) {
			TreeNode<K, V> loHead = null;
			TreeNode<K, V> loTail = null;
			TreeNode<K, V> hiHead = null;
			TreeNode<K, V> hiTail = null;
			int loCount = 0, hiCount = 0;

			TreeNode<K, V> entry = this;
			TreeNode<K, V> next;
			while (entry != null) {
				next = (TreeNode<K, V>) entry.next;
				entry.next = null;
				if ((entry.hash & bit) == 0) {
					entry.prev = loTail;
					if (loTail == null) {
						loHead = entry;
					} else {
						loTail.next = entry;
					}
					loTail = entry;
					loCount++;
				} else {
					entry.prev = hiTail;
					if (hiTail == null) {
						hiHead = entry;
					} else {
						hiTail.next = entry;
					}
					hiTail = entry;
					hiCount++;
				}
				entry = next;
			}

			if (loHead != null) {
				if (loCount <= UNTREEIFY_THRESHOLD) {
					table[index] = loHead.untreeify();
				} else {
					table[index] = loHead;
					if (hiHead != null) {
						loHead.treeify(table);
					}
				}
			}
			if (hiHead != null) {
				if (hiCount <= UNTREEIFY_THRESHOLD) {
					table[index + bit] = hiHead.untreeify();
				} else {
					table[index + bit] = hiHead;
					if (loHead != null) {
						hiHead.treeify(table);
					}
				}
			}
		}

		static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> node) {
			if (node == null) {
				return root;
			}
			TreeNode<K, V> nodeRight = node.right;
			TreeNode<K, V> nodeParent;
			TreeNode<K, V> nodeRightLeft;
			if (nodeRight != null) {
				nodeRightLeft = node.right = nodeRight.left;
				if (nodeRightLeft != null) {
					nodeRightLeft.parent = node;
				}
				nodeParent = nodeRight.parent = node.parent;
				if (nodeParent == null) {
					root = nodeRight;
					root.red = false;
				} else if (nodeParent.left == node) {
					nodeParent.left = nodeRight;
				} else {
					nodeParent.right = nodeRight;
				}
				nodeRight.left = node;
				node.parent = nodeRight;
			}
			return root;
		}

		static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> node) {
			if (node == null) {
				return root;
			}
			TreeNode<K, V> nodeLeft = node.left;
			TreeNode<K, V> nodeParent;
			TreeNode<K, V> nodeLeftRight;
			if (nodeLeft != null) {
				nodeLeftRight = node.left = nodeLeft.right;
				if (nodeLeftRight != null) {
					nodeLeftRight.parent = node;
				}
				nodeParent = nodeLeft.parent = node.parent;
				if (nodeParent == null) {
					root = nodeLeft;
					root.red = false;
				} else if (nodeParent.right == node) {
					nodeParent.right = nodeLeft;
				} else {
					nodeParent.left = nodeLeft;
				}
				nodeLeft.right = node;
				node.parent = nodeLeft;
			}
			return root;
		}

		static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root, TreeNode<K, V> node) {
			node.red = true;
			TreeNode<K, V> nodeParent;
			TreeNode<K, V> nodeGrandparent;
			TreeNode<K, V> nodeGrandparentLeft;
			TreeNode<K, V> nodeGrandparentRight;
			while (true) {
				nodeParent = node.parent;
				if (nodeParent == null) {
					node.red = false;
					return node;
				} else if (!nodeParent.red) {
					return root;
				}
				nodeGrandparent = nodeParent.parent;
				if (nodeGrandparent == null) {
					return root;
				}
				nodeGrandparentLeft = nodeGrandparent.left;
				if (nodeParent == nodeGrandparentLeft) {
					nodeGrandparentRight = nodeGrandparent.right;
					if (nodeGrandparentRight != null && nodeGrandparentRight.red) {
						nodeGrandparentRight.red = false;
						nodeParent.red = false;
						nodeGrandparent.red = true;
						node = nodeGrandparent;
					} else {
						if (node == nodeParent.right) {
							node = nodeParent;
							root = rotateLeft(root, node);
							nodeParent = node.parent;
							nodeGrandparent = nodeParent == null ? null : nodeParent.parent;
						}
						if (nodeParent != null) {
							nodeParent.red = false;
							if (nodeGrandparent != null) {
								nodeGrandparent.red = true;
								root = rotateRight(root, nodeGrandparent);
							}
						}
					}
				} else {
					if (nodeGrandparentLeft != null && nodeGrandparentLeft.red) {
						nodeGrandparentLeft.red = false;
						nodeParent.red = false;
						nodeGrandparent.red = true;
						node = nodeGrandparent;
					} else {
						if (node == nodeParent.left) {
							node = nodeParent;
							root = rotateRight(root, node);
							nodeParent = node.parent;
							nodeGrandparent = nodeParent == null ? null : nodeParent.parent;
						}
						if (nodeParent != null) {
							nodeParent.red = false;
							if (nodeGrandparent != null) {
								nodeGrandparent.red = true;
								root = rotateLeft(root, nodeGrandparent);
							}
						}
					}
				}
			}
		}

		static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root, TreeNode<K, V> node) {
			TreeNode<K, V> nodeParent;
			TreeNode<K, V> nodeParentLeft;
			TreeNode<K, V> nodeParentRight;
			while (true) {
				if (node == null || node == root) {
					return root;
				}
				nodeParent = node.parent;
				if (nodeParent == null) {
					node.red = false;
					return node;
				} else if (node.red) {
					node.red = false;
					return root;
				}
				nodeParentLeft = nodeParent.left;
				if (nodeParentLeft == node) {
					nodeParentRight = nodeParent.right;
					if (nodeParentRight != null && nodeParentRight.red) {
						nodeParentRight.red = false;
						nodeParent.red = true;
						root = rotateLeft(root, nodeParent);
						nodeParent = node.parent;
						nodeParentRight = nodeParent == null ? null : nodeParent.right;
					}
					if (nodeParentRight == null) {
						node = nodeParent;
					} else {
						TreeNode<K, V> successorLeft = nodeParentRight.left;
						TreeNode<K, V> successorRight = nodeParentRight.right;
						if ((successorRight == null || !successorRight.red)
								&& (successorLeft == null || !successorLeft.red)) {
							nodeParentRight.red = true;
							node = nodeParent;
						} else {
							if (successorRight == null || !successorRight.red) {
								if (successorLeft != null) {
									successorLeft.red = false;
								}
								nodeParentRight.red = true;
								root = rotateRight(root, nodeParentRight);
								nodeParent = node.parent;
								nodeParentRight = nodeParent == null ? null : nodeParent.right;
							}
							if (nodeParentRight != null) {
								nodeParentRight.red = (nodeParent == null) ? false : nodeParent.red;
								successorRight = nodeParentRight.right;
								if (successorRight != null) {
									successorRight.red = false;
								}
							}
							if (nodeParent != null) {
								nodeParent.red = false;
								root = rotateLeft(root, nodeParent);
							}
							node = root;
						}
					}
				} else {
					if (nodeParentLeft != null && nodeParentLeft.red) {
						nodeParentLeft.red = false;
						nodeParent.red = true;
						root = rotateRight(root, nodeParent);
						nodeParent = node.parent;
						nodeParentLeft = nodeParent == null ? null : nodeParent.left;
					}
					if (nodeParentLeft == null) {
						node = nodeParent;
					} else {
						TreeNode<K, V> successorLeft = nodeParentLeft.left;
						TreeNode<K, V> successorRight = nodeParentLeft.right;
						if ((successorLeft == null || !successorLeft.red)
								&& (successorRight == null || !successorRight.red)) {
							nodeParentLeft.red = true;
							node = nodeParent;
						} else {
							if (successorLeft == null || !successorLeft.red) {
								if (successorRight != null) {
									successorRight.red = false;
								}
								nodeParentLeft.red = true;
								root = rotateLeft(root, nodeParentLeft);
								nodeParent = node.parent;
								nodeParentLeft = nodeParent == null ? null : nodeParent.left;
							}
							if (nodeParentLeft != null) {
								nodeParentLeft.red = (nodeParent == null) ? false : nodeParent.red;
								successorLeft = nodeParentLeft.left;
								if (successorLeft != null) {
									successorLeft.red = false;
								}
							}
							if (nodeParent != null) {
								nodeParent.red = false;
								root = rotateRight(root, nodeParent);
							}
							node = root;
						}
					}
				}
			}
		}

		static <K, V> boolean checkInvariants(TreeNode<K, V> node) {
			TreeNode<K, V> nodeParent = node.parent;
			TreeNode<K, V> nodeLeft = node.left;
			TreeNode<K, V> nodeRight = node.right;
			TreeNode<K, V> nodePrev = node.prev;
			TreeNode<K, V> nodeNext = (TreeNode<K, V>) node.next;
			if (nodePrev != null && nodePrev.next != node) {
				return false;
			}
			if (nodeNext != null && nodeNext.prev != node) {
				return false;
			}
			if (nodeParent != null && node != nodeParent.left && node != nodeParent.right) {
				return false;
			}
			if (nodeLeft != null && (nodeLeft.parent != node || nodeLeft.hash > node.hash)) {
				return false;
			}
			if (nodeRight != null && (nodeRight.parent != node || nodeRight.hash < node.hash)) {
				return false;
			}
			if (node.red && nodeLeft != null && nodeLeft.red && nodeRight != null && nodeRight.red) {
				return false;
			}
			if (nodeLeft != null && !checkInvariants(nodeLeft)) {
				return false;
			}
			if (nodeRight != null && !checkInvariants(nodeRight)) {
				return false;
			}
			return true;
		}
	}

}
