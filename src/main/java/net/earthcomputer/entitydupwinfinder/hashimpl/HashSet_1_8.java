package net.earthcomputer.entitydupwinfinder.hashimpl;

import java.util.Map;

public class HashSet_1_8<E> extends MapBackedSet<E> {

	@Override
	protected Map<E, Object> createInternalMap() {
		return new HashMap_1_8<E, Object>();
	}

}
