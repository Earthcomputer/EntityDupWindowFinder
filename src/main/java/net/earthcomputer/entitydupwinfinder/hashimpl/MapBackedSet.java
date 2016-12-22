package net.earthcomputer.entitydupwinfinder.hashimpl;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;

public abstract class MapBackedSet<E> extends AbstractSet<E> {

	private static final Object VALUE = new Object();

	private Map<E, Object> map;

	public MapBackedSet() {
		map = createInternalMap();
	}

	protected abstract Map<E, Object> createInternalMap();

	@Override
	public boolean add(E e) {
		return map.put(e, VALUE) == null;
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public boolean remove(Object o) {
		return map.remove(o) == VALUE;
	}

	@Override
	public int size() {
		return map.size();
	}

}
