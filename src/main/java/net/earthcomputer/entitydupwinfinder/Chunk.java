package net.earthcomputer.entitydupwinfinder;

public class Chunk implements Comparable<Chunk> {

	private int x;
	private int z;

	public Chunk(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public int getZ() {
		return z;
	}

	public long toLong() {
		return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32;
	}

	public static Chunk fromLong(long value) {
		return new Chunk((int) (value & 0xFFFFFFFFL), (int) ((value >>> 32) & 0xFFFFFFFFL));
	}

	@Override
	public int hashCode() {
		return new Long(toLong()).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Chunk)) {
			return false;
		}
		return equals((Chunk) other);
	}

	public boolean equals(Chunk other) {
		return x == other.x && z == other.z;
	}

	@Override
	public int compareTo(Chunk other) {
		if (x == other.x) {
			return Integer.compare(z, other.z);
		} else {
			return Integer.compare(x, other.x);
		}
	}

	@Override
	public String toString() {
		return "Chunk{" + x + "," + z + "}";
	}

}
