package net.earthcomputer.entitydupwinfinder;

import java.util.HashSet;
import java.util.Set;

public class ChunkMap {

	private int width;
	private int height;
	private Chunk[] chunks;
	private Set<Chunk> duplicationChunks;
	private Set<Chunk> deletionChunks;
	private Settings.EnumCardinalDir entityDirection;

	public ChunkMap(int minX, int minZ, int width, int height, Settings.EnumCardinalDir entityDirection) {
		this.width = width;
		this.height = height;
		this.chunks = new Chunk[width * height];
		int index = 0;
		for (int z = 0; z < height; z++) {
			for (int x = 0; x < width; x++) {
				chunks[index++] = new Chunk(x + minX, z + minZ);
			}
		}
		this.duplicationChunks = new HashSet<Chunk>();
		this.deletionChunks = new HashSet<Chunk>();
		this.entityDirection = entityDirection;
	}

	public int getMinX() {
		return chunks[0].getX();
	}

	public int getMinZ() {
		return chunks[0].getZ();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Chunk getChunkAt(int x, int z) {
		return chunks[x + z * width];
	}
	
	public Set<Chunk> getDuplicationChunks() {
		return duplicationChunks;
	}
	
	public Set<Chunk> getDeletionChunks() {
		return deletionChunks;
	}
	
	public Settings.EnumCardinalDir getEntityDirection() {
		return entityDirection;
	}

}
