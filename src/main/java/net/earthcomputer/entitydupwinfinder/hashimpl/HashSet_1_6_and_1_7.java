package net.earthcomputer.entitydupwinfinder.hashimpl;

import java.util.Map;

public class HashSet_1_6_and_1_7<E> extends MapBackedSet<E> {

	@Override
	protected Map<E, Object> createInternalMap() {
		return new HashMap_1_6_and_1_7<E, Object>();
	}

}
