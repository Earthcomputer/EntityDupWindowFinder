package net.earthcomputer.entitydupwinfinder;

import java.util.Set;

import net.earthcomputer.entitydupwinfinder.hashimpl.HashSet_1_6_and_1_7;
import net.earthcomputer.entitydupwinfinder.hashimpl.HashSet_1_8;

public class Settings {

	private int chunkX = 0;
	private int chunkZ = 0;
	private EnumCardinalDir entityMoveDir = EnumCardinalDir.NORTH;
	private int renderDistance = 12;
	private EnumJavaVersion javaVersion = EnumJavaVersion.EIGHT;

	public int getChunkX() {
		return chunkX;
	}

	public void setChunkX(int chunkX) {
		this.chunkX = chunkX;
	}

	public int getChunkZ() {
		return chunkZ;
	}

	public void setChunkZ(int chunkZ) {
		this.chunkZ = chunkZ;
	}

	public EnumCardinalDir getEntityMoveDir() {
		return entityMoveDir;
	}

	public void setEntityMoveDir(EnumCardinalDir entityMoveDir) {
		this.entityMoveDir = entityMoveDir;
	}

	public int getRenderDistance() {
		return renderDistance;
	}

	public void setRenderDistance(int renderDistance) {
		this.renderDistance = renderDistance;
	}

	public EnumJavaVersion getJavaVersion() {
		return javaVersion;
	}

	public void setJavaVersion(EnumJavaVersion javaVersion) {
		this.javaVersion = javaVersion;
	}

	public static enum EnumCardinalDir {
		NORTH("North"), SOUTH("South"), WEST("West"), EAST("East");

		private String name;

		private EnumCardinalDir(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static EnumCardinalDir byName(String name) {
			for (EnumCardinalDir dir : values()) {
				if (dir.name.equals(name)) {
					return dir;
				}
			}
			return null;
		}
	}

	public static enum EnumJavaVersion {
		SIX("Java 6", HashSet_1_6_and_1_7.class), SEVEN("Java 7", HashSet_1_6_and_1_7.class), EIGHT("Java 8",
				HashSet_1_8.class);

		private String name;
		private Class<?> hashSetClass;

		private EnumJavaVersion(String name, Class<?> hashSetClass) {
			this.name = name;
			this.hashSetClass = hashSetClass;
		}

		@SuppressWarnings("unchecked")
		public <T> Set<T> createHashSet() {
			try {
				return (Set<T>) hashSetClass.newInstance();
			} catch (InstantiationException e) {
				throw new Error(e);
			} catch (IllegalAccessException e) {
				throw new Error(e);
			}
		}

		public String getName() {
			return name;
		}

		public static EnumJavaVersion byName(String name) {
			for (EnumJavaVersion version : values()) {
				if (version.name.equals(name)) {
					return version;
				}
			}
			return null;
		}
	}

}
