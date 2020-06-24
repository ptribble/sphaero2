package uk.co.petertribble.sphaero2;

import java.awt.Image;
import java.awt.image.PixelGrabber;

/**
 * Cuts the puzzle into uniform rectangles.
 */
public class RectCutter extends JigsawCutter {

    @Override
    public String getName() {
	return "Rectangles";
    }

    @Override
    public String getDescription() {
	return "Cuts the image into uniform rectangles.";
    }

    @Override
    public Piece[] cut(Image image) {
	JigUtil.ensureLoaded(image);
	int height = image.getHeight(null);
	int width = image.getWidth(null);
	int rows = (int) Math.round(Math.sqrt(prefPieces));
	int columns = (int) Math.round(Math.sqrt(prefPieces));

	jp.setMaximum(rows*columns);
	jp.setValue(iprogress);

	// Create piece images
	Piece[][] matrix = new Piece[rows][columns];
	for (int i = 0; i < rows; i++) {
	    int y1 = i * height / rows;
	    int y2 = (i+1) * height / rows;
	    if (i > 0) {
		y1++;
	    }
	    for (int j = 0; j < columns; j++) {
		int x1 = j * width / columns;
		int x2 = (j+1) * width / columns;
		if (j > 0) {
		    x1++;
		}
		int pieceW = x2 - x1 + 1;
		int pieceH = y2 - y1 + 1;
		int rotation = (int)(Math.random()*4) * 90;
		matrix[i][j] = new Piece(
				getImageData(image, x1, y1, pieceW, pieceH),
				x1, y1, pieceW, pieceH,
				width, height, rotation);
		iprogress++;
		jp.setValue(iprogress);
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
