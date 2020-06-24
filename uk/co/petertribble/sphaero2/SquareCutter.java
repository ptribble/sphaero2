package uk.co.petertribble.sphaero2;

import java.awt.Image;
import java.awt.image.PixelGrabber;

/**
 * Cuts the puzzle into uniform squares. If the image dimensions aren't an
 * integer number of squares the ends are extended to fit.
 */
public class SquareCutter extends JigsawCutter {

    @Override
    public String getName() {
	return "Squares";
    }

    @Override
    public String getDescription() {
	return "Cuts the image into uniform squares.";
    }

    @Override
    public Piece[] cut(Image image) {
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

	jp.setMaximum(rows*columns);
	jp.setValue(iprogress);

	// Create piece images
	Piece[][] matrix = new Piece[rows][columns];
	for (int i = 0; i < rows; i++) {
	    int x1 = 0;
	    int x2 = firstEastEdge;
	    for (int j = 0; j < columns; j++) {
		int pieceW = x2 - x1 + 1;
		int pieceH = y2 - y1 + 1;
		int rotation = (int)(Math.random()*4) * 90;
		matrix[i][j] = new Piece(
				getImageData(image, x1, y1, pieceW, pieceH),
				x1, y1, pieceW, pieceH,
				width, height, rotation);
		iprogress++;
		jp.setValue(iprogress);

		// Set up x1 and x2 for next slice
		x1 = x2+1;
		x2 += edge;
		if ((width - x2) < edge) {
		    x2 = width-1;
		}
	    }

	    // Set up y1 and y2 for next slice
	    y1 = y2 + 1;
	    y2 += edge;
	    if ((height - y2) < edge) {
		y2 = height-1;
	    }
	}

	return finalBuild(matrix, columns, rows);
    }

    private int[] getImageData(Image image, int x, int y,
				int width, int height) {
	int[] data = new int[height * width];
	PixelGrabber grabber =
	    new PixelGrabber(image, x, y, width, height, data, 0, width);
	try {
	    grabber.grabPixels();
	} catch (InterruptedException ex) {
	    System.out.println("interrupted while grabbing");
	}
	return data;
    }
}
