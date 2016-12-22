package net.earthcomputer.entitydupwinfinder;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JComponent;

public class ChunkDisplayPanel extends JComponent {

	private static final long serialVersionUID = -8202791280908327943L;

	private ChunkMap map;
	private boolean working = true;

	public void setChunkMap(ChunkMap map) {
		this.map = map;
		this.repaint();
	}

	public void setWorking(boolean working) {
		this.working = working;
		this.repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		int drawStartX = getX();
		int drawStartY = getY();
		if (map != null) {
			int chunkSize = Math.min(getWidth() / map.getWidth(), getHeight() / map.getHeight());
			int drawWidth = chunkSize * map.getWidth();
			int drawHeight = chunkSize * map.getHeight();
			drawStartX = getX() + getWidth() / 2 - drawWidth / 2;
			drawStartY = getY() + getHeight() / 2 - drawHeight / 2;

			// Background
			g.setColor(Color.WHITE);
			g.fillRect(drawStartX, drawStartY, drawWidth, drawHeight);

			// Grid lines
			g.setColor(Color.BLACK);
			for (int x = 0; x <= map.getWidth(); x++) {
				g.drawLine(drawStartX + chunkSize * x, drawStartY, drawStartX + chunkSize * x,
						drawStartY + drawHeight - 1);
			}
			for (int y = 0; y <= map.getHeight(); y++) {
				g.drawLine(drawStartX, drawStartY + chunkSize * y, drawStartX + drawWidth - 1,
						drawStartY + chunkSize * y);
			}

			// Duplicating chunks
			g.setColor(new Color(127, 127, 255));
			for (Chunk chunk : map.getDuplicationChunks()) {
				g.fillOval(drawStartX + chunkSize * (chunk.getX() - map.getMinX()) + 1,
						drawStartY + chunkSize * (chunk.getZ() - map.getMinZ()), chunkSize - 1, chunkSize - 1);
			}

			// Deleting chunks
			g.setColor(new Color(255, 127, 127));
			for (Chunk chunk : map.getDeletionChunks()) {
				g.fillOval(drawStartX + chunkSize * (chunk.getX() - map.getMinX()),
						drawStartY + chunkSize * (chunk.getZ() - map.getMinZ()), chunkSize - 1, chunkSize - 1);
			}

			// Entity movement
			g.setColor(new Color(0, 127, 0));
			switch (map.getEntityDirection()) {
			case EAST: {
				g.drawLine(drawStartX + drawWidth / 2 - chunkSize / 2, drawStartY + drawHeight / 2,
						drawStartX + drawWidth / 2 + chunkSize / 2, drawStartY + drawHeight / 2);
				Point arrowHead = new Point(drawStartX + drawWidth / 2 + chunkSize / 2, drawStartY + drawHeight / 2);
				g.drawLine(arrowHead.x, arrowHead.y, arrowHead.x - 5, arrowHead.y - 5);
				g.drawLine(arrowHead.x, arrowHead.y, arrowHead.x - 5, arrowHead.y + 5);
				break;
			}
			case NORTH: {
				g.drawLine(drawStartX + drawWidth / 2, drawStartY + drawHeight / 2 - chunkSize / 2,
						drawStartX + drawWidth / 2, drawStartY + drawHeight / 2 + chunkSize / 2);
				Point arrowHead = new Point(drawStartX + drawWidth / 2, drawStartY + drawHeight / 2 - chunkSize / 2);
				g.drawLine(arrowHead.x, arrowHead.y, arrowHead.x - 5, arrowHead.y + 5);
				g.drawLine(arrowHead.x, arrowHead.y, arrowHead.x + 5, arrowHead.y + 5);
				break;
			}
			case SOUTH: {
				g.drawLine(drawStartX + drawWidth / 2, drawStartY + drawHeight / 2 - chunkSize / 2,
						drawStartX + drawWidth / 2, drawStartY + drawHeight / 2 + chunkSize / 2);
				Point arrowHead = new Point(drawStartX + drawWidth / 2, drawStartY + drawHeight / 2 + chunkSize / 2);
				g.drawLine(arrowHead.x, arrowHead.y, arrowHead.x - 5, arrowHead.y - 5);
				g.drawLine(arrowHead.x, arrowHead.y, arrowHead.x + 5, arrowHead.y - 5);
				break;
			}
			case WEST: {
				g.drawLine(drawStartX + drawWidth / 2 - chunkSize / 2, drawStartY + drawHeight / 2,
						drawStartX + drawWidth / 2 + chunkSize / 2, drawStartY + drawHeight / 2);
				Point arrowHead = new Point(drawStartX + drawWidth / 2 - chunkSize / 2, drawStartY + drawHeight / 2);
				g.drawLine(arrowHead.x, arrowHead.y, arrowHead.x + 5, arrowHead.y - 5);
				g.drawLine(arrowHead.x, arrowHead.y, arrowHead.x + 5, arrowHead.y + 5);
				break;
			}
			}
		}

		// Working
		if (working) {
			g.setColor(Color.GREEN.darker());
			g.setFont(g.getFont().deriveFont(Font.BOLD, 40));
			g.drawString("Working...", drawStartX + 2, drawStartY + 2 + g.getFontMetrics().getAscent());
		}
	}

}
