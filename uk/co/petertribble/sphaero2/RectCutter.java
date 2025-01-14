package uk.co.petertribble.sphaero2;

import java.awt.image.BufferedImage;

/**
 * Cuts the puzzle into uniform rectangles.
 */
public final class RectCutter extends JigsawCutter {

    @Override
    public String getName() {
	return "Rectangles";
    }

    @Override
    public String getDescription() {
	return "Pieces are uniform rectangles.";
    }

    @Override
    public Piece[] cut(BufferedImage image) {
	JigUtil.ensureLoaded(image);
	int height = image.getHeight(null);
	int width = image.getWidth(null);
	int rows = (int) Math.round(Math.sqrt(prefPieces));
	int columns = (int) Math.round(Math.sqrt(prefPieces));

	startProgress(rows * columns);

	// Create piece images
	Piece[][] matrix = new Piece[rows][columns];
	for (int i = 0; i < rows; i++) {
	    int y1 = i * height / rows;
	    int y2 = (i + 1) * height / rows;
	    if (y2 >= height) {
		y2 = height - 1;
	    }
	    if (i > 0) {
		y1++;
	    }
	    for (int j = 0; j < columns; j++) {
		int x1 = j * width / columns;
		int x2 = (j + 1) * width / columns;
		if (x2 >= width) {
		    x2 = width - 1;
		}
		if (j > 0) {
		    x1++;
		}
		int pieceW = x2 - x1 + 1;
		int pieceH = y2 - y1 + 1;
		int rotation = (int) (Math.random() * 4) * 90;
		matrix[i][j] = new Piece(
				getImageData(image, x1, y1, pieceW, pieceH),
				x1, y1, pieceW, pieceH,
				width, height, rotation);
		updateProgress();
	    }
	}

	return finalBuild(matrix, columns, rows);
    }

    private int[] getImageData(BufferedImage image, int x, int y,
				int width, int height) {
	int[] data = new int[height * width];
	data = image.getRGB(x, y, width, height, data, 0, width);
	return data;
    }
}
