package uk.co.petertribble.sphaero2;

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cuts the image into uniform squares. If the image dimensions aren't an
 * integer number of squares the ends are extended to fit.
 */
public final class SquareCutter extends JigsawCutter {

    @Override
    public String getName() {
	return "Squares";
    }

    @Override
    public String getDescription() {
	return "Pieces are uniform squares.";
    }

    @Override
    public Piece[] cut(final BufferedImage image) {
	JigUtil.ensureLoaded(image);
	int height = image.getHeight(null);
	int width = image.getWidth(null);
	int edge = (int) Math.round(Math.sqrt(height * width / prefPieces));
	int hRemain = height % edge;
	int wRemain = width  % edge;
	int rows = height / edge;
	int columns = width  / edge;
	int firstSouthEdge = edge + (hRemain / 2) - 1;
	int firstEastEdge  = edge + (wRemain / 2) - 1;

	int y1 = 0;
	int y2 = firstSouthEdge;

	startProgress(rows * columns);

	// Create piece images
	Piece[][] matrix = new Piece[rows][columns];
	for (int i = 0; i < rows; i++) {
	    int x1 = 0;
	    int x2 = firstEastEdge;
	    for (int j = 0; j < columns; j++) {
		int pieceW = x2 - x1 + 1;
		int pieceH = y2 - y1 + 1;
		int rotation = ThreadLocalRandom.current().nextInt(4) * 90;
		matrix[i][j] = new Piece(
				getImageData(image, x1, y1, pieceW, pieceH),
				x1, y1, pieceW, pieceH,
				width, height, rotation);
		updateProgress();

		// Set up x1 and x2 for next slice
		x1 = x2 + 1;
		x2 += edge;
		if ((width - x2) < edge) {
		    x2 = width - 1;
		}
	    }

	    // Set up y1 and y2 for next slice
	    y1 = y2 + 1;
	    y2 += edge;
	    if ((height - y2) < edge) {
		y2 = height - 1;
	    }
	}

	return finalBuild(matrix, columns, rows);
    }

    private int[] getImageData(final BufferedImage image,
			       final int x, final int y,
			       final int width, final int height) {
	int[] data = new int[height * width];
	data = image.getRGB(x, y, width, height, data, 0, width);
	return data;
    }
}
