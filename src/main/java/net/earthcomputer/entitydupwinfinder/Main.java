package net.earthcomputer.entitydupwinfinder;

import java.util.Set;

import net.earthcomputer.entitydupwinfinder.hashimpl.HashSet_1_8;

public class Main {

	public static void main(String[] args) {
		Set<Integer> set = new HashSet_1_8<Integer>();
		set.add(10);
		set.add(5);
		set.add(10);
		set.add(20);
		set.add(30);
		set.add(40);
		set.add(50);

		for (Integer value : set) {
			System.out.println(value);
		}
	}

}
