package net.earthcomputer.entitydupwinfinder;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Main {

	private static Settings settings;
	private static Thread chunkDisplayCalcThread;
	private static ChunkDisplayPanel chunkDisplayPanel;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		settings = new Settings();

		JFrame frame = new JFrame("Entity Duplication Window Finder");

		JPanel optionsPanel = new JPanel();
		{
			optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
			{
				JPanel row = new JPanel();
				optionsPanel.add(row);
				JLabel coordTitle = new JLabel("Chunk the entity is moving into");
				coordTitle.setFont(coordTitle.getFont().deriveFont(Font.BOLD));
				row.add(coordTitle);
			}
			{
				JPanel row = new JPanel();
				optionsPanel.add(row);
				row.add(new JLabel("X: "));
				row.add(createTextField(new IntConsumer() {
					@Override
					public void consume(int value) {
						settings.setChunkX(value);
					}
				}));
			}
			{
				JPanel row = new JPanel();
				optionsPanel.add(row);
				row.add(new JLabel("Z: "));
				row.add(createTextField(new IntConsumer() {
					@Override
					public void consume(int value) {
						settings.setChunkZ(value);
					}
				}));
			}
			{
				JPanel row = new JPanel();
				optionsPanel.add(row);
				row.add(new JLabel("Entity direction of movement: "));
				String[] dirStrings = new String[Settings.EnumCardinalDir.values().length];
				for (int i = 0; i < dirStrings.length; i++) {
					dirStrings[i] = Settings.EnumCardinalDir.values()[i].getName();
				}
				final JComboBox<String> comboBox = new JComboBox<String>(dirStrings);
				row.add(comboBox);
				comboBox.setSelectedItem(Settings.EnumCardinalDir.NORTH.getName());
				comboBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						settings.setEntityMoveDir(Settings.EnumCardinalDir.byName((String) comboBox.getSelectedItem()));
						recalcChunks();
					}
				});
			}
			optionsPanel.add(Box.createRigidArea(new Dimension(10, 10)));
			{
				JPanel row = new JPanel();
				optionsPanel.add(row);
				row.add(new JLabel("Render distance: "));
				JTextField field = createTextField(new IntConsumer() {
					@Override
					public void consume(int value) {
						if (value >= 2 && value <= 32) {
							settings.setRenderDistance(value);
						}
					}
				});
				row.add(field);
				field.setColumns("32".length());
			}
			{
				JPanel row = new JPanel();
				optionsPanel.add(row);
				row.add(new JLabel("Server Java version: "));
				String[] javaVersionStrings = new String[Settings.EnumJavaVersion.values().length];
				for (int i = 0; i < javaVersionStrings.length; i++) {
					javaVersionStrings[i] = Settings.EnumJavaVersion.values()[i].getName();
				}
				final JComboBox<String> comboBox = new JComboBox<String>(javaVersionStrings);
				row.add(comboBox);
				comboBox.setSelectedItem(Settings.EnumJavaVersion.EIGHT.getName());
				comboBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						settings.setJavaVersion(Settings.EnumJavaVersion.byName((String) comboBox.getSelectedItem()));
						recalcChunks();
					}
				});
			}
			{
				Filler glue = (Filler) Box.createVerticalGlue();
				glue.changeShape(glue.getMinimumSize(), new Dimension(0, Short.MAX_VALUE), glue.getMaximumSize());
				optionsPanel.add(glue);
			}
		}

		chunkDisplayPanel = new ChunkDisplayPanel();
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridLayout(1, 1));
		rightPanel.add(chunkDisplayPanel);

		recalcChunks();

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, optionsPanel, rightPanel);
		frame.getContentPane().add(splitPane);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
	}

	private static JTextField createTextField(final IntConsumer updateSettings) {
		final JTextField field = new JFormattedTextField(NumberFormat.getIntegerInstance());
		field.setColumns("-1875000".length());
		field.setText("0");
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				// No change here, this is only changes in formatting
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				onChange();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				onChange();
			}

			private void onChange() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int intValue;
						try {
							intValue = Integer.parseInt(field.getText());
						} catch (NumberFormatException e) {
							return;
						}
						updateSettings.consume(intValue);
						recalcChunks();
					}
				});
			}
		});
		return field;
	}

	private static void recalcChunks() {
		if (chunkDisplayCalcThread != null) {
			chunkDisplayCalcThread.interrupt();
		}
		chunkDisplayPanel.setWorking(true);
		chunkDisplayCalcThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Chunk entityMovingFrom;
				Chunk entityMovingInto = new Chunk(settings.getChunkX(), settings.getChunkZ());
				Chunk leastChunk;
				Settings.EnumCardinalDir entityMoveDir = settings.getEntityMoveDir();
				int renderDistance = settings.getRenderDistance();
				switch (entityMoveDir) {
				case EAST: {
					entityMovingFrom = new Chunk(entityMovingInto.getX() - 1, entityMovingInto.getZ());
					leastChunk = new Chunk(entityMovingInto.getX() - renderDistance,
							entityMovingInto.getZ() - renderDistance);
					break;
				}
				case NORTH: {
					entityMovingFrom = new Chunk(entityMovingInto.getX(), entityMovingInto.getZ() + 1);
					leastChunk = new Chunk(entityMovingFrom.getX() - renderDistance,
							entityMovingFrom.getZ() - renderDistance);
					break;
				}
				case SOUTH: {
					entityMovingFrom = new Chunk(entityMovingInto.getX(), entityMovingInto.getZ() - 1);
					leastChunk = new Chunk(entityMovingInto.getX() - renderDistance,
							entityMovingInto.getZ() - renderDistance);
					break;
				}
				case WEST: {
					entityMovingFrom = new Chunk(entityMovingInto.getX() + 1, entityMovingInto.getZ());
					leastChunk = new Chunk(entityMovingFrom.getX() - renderDistance,
							entityMovingFrom.getZ() - renderDistance);
					break;
				}
				default:
					throw new AssertionError();
				}
				int cols = renderDistance * 2;
				if (entityMoveDir == Settings.EnumCardinalDir.NORTH
						|| entityMoveDir == Settings.EnumCardinalDir.SOUTH) {
					cols++;
				}
				int rows = renderDistance * 2;
				if (entityMoveDir == Settings.EnumCardinalDir.WEST || entityMoveDir == Settings.EnumCardinalDir.EAST) {
					rows++;
				}
				final ChunkMap map = new ChunkMap(leastChunk.getX(), leastChunk.getZ(), cols, rows, entityMoveDir);

				if (Thread.currentThread().isInterrupted()) {
					return;
				}

				if (renderDistance >= 5) {
					for (int x = 0; x < cols; x++) {
						for (int z = 0; z < rows; z++) {
							Set<Long> unloadingChunksSet = settings.getJavaVersion().createHashSet();
							for (int chunkX = x - renderDistance; chunkX <= x + renderDistance; chunkX++) {
								for (int chunkZ = z - renderDistance; chunkZ <= z + renderDistance; chunkZ++) {
									unloadingChunksSet.add(new Chunk(chunkX, chunkZ).toLong());
								}
							}
							Set<Chunk> firstPassChunksSet = new HashSet<Chunk>();
							Set<Chunk> secondPassChunksSet = new HashSet<Chunk>();
							Iterator<Long> unloadingChunksItr = unloadingChunksSet.iterator();
							for (int i = 0; i < 100; i++) {
								firstPassChunksSet.add(Chunk.fromLong(unloadingChunksItr.next()));
							}
							for (int i = 0; i < 100 && unloadingChunksItr.hasNext(); i++) {
								secondPassChunksSet.add(Chunk.fromLong(unloadingChunksItr.next()));
							}
							if (firstPassChunksSet.contains(entityMovingFrom)
									&& secondPassChunksSet.contains(entityMovingInto)) {
								map.getDuplicationChunks().add(new Chunk(x, z));
							} else if (secondPassChunksSet.contains(entityMovingFrom)
									&& firstPassChunksSet.contains(entityMovingInto)) {
								map.getDeletionChunks().add(new Chunk(x, z));
							}
							if (Thread.currentThread().isInterrupted()) {
								return;
							}
						}
					}
				}

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						chunkDisplayPanel.setChunkMap(map);
						chunkDisplayPanel.setWorking(false);
						chunkDisplayCalcThread = null;
					}
				});
			}
		});
		chunkDisplayCalcThread.setDaemon(true);
		chunkDisplayCalcThread.start();
	}

}
