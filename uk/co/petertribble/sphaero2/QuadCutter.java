package uk.co.petertribble.sphaero2;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Cuts pieces into random quadrilaterals.  Pieces are arranged into rough
 * rows and columns, with their edges tilting at various angles. Each piece
 * will be about 1.2 times as wide as it is tall.
 */
public class QuadCutter extends JigsawCutter {

    private static final double WSCALE = 1.2;

    @Override
    public String getName() {
	return "Quads";
    }

    @Override
    public String getDescription() {
	return "Pieces are random quadrilaterals.";
    }

    @Override
    public Piece[] cut(BufferedImage image) {
	JigUtil.ensureLoaded(image);
	int width = image.getWidth(null);
	int height = image.getHeight(null);

	/*
	 * First compute the number of rows and columns.  If N = total number
	 * of pieces, R = rows, C = columns, H = height, W = width, and K =
	 * width/height ratio, then
	 * R * C = N
	 * (W/C) / (H/R) = K
	 * and therefore
	 * C = N/R
	 * (WRR/NH) = K
	 * R = sqrt (NHK/W)
	 */
	int rows = (int) Math.round(
		Math.sqrt(WSCALE * prefPieces * height / width));
	int columns = Math.round(prefPieces / rows);

	startProgress(rows*columns);

	// Make a matrix of points representing the corners of each piece.
	// Each point is based on a grid of equal rectangles, but may drift by
	// a factor of up to 0.1 in any direction.  Edge points, of course,
	// cannot drift in certain dimensions.
	int hVary = height / (rows * 10);
	int wVary = width / (columns * 10);
	Point[][] points = new Point[columns+1][rows+1];
	// i varies horizontally; j varies vertically
	for (int j = 0; j <= rows; j++) {
	    int baseY = j*height / rows;
	    for (int i = 0; i <= columns; i++) {
		int baseX = i*width / columns;
		int x = baseX;
		int y = baseY;
		if (i > 0 && i < columns) {
		    x += Math.random()*(2*wVary+1) - wVary;
		}
		if (j > 0 && j < rows) {
		    y += Math.random()*(2*hVary+1) - hVary;
		}
		points[i][j] = new Point(x, y);
	    }
	}

	// Create the pieces.
	Piece[][] pieces = new Piece[columns][rows];
	for (int j = 0; j < rows; j++) {
	    for (int i = 0; i < columns; i++) {
		pieces[i][j] = makePiece(image,
					  points[i][j],
					  points[i][j+1],
					  points[i+1][j],
					  points[i+1][j+1],
					  width, height);
		updateProgress();
	    }
	}

	// Set each piece's neighbors, and build the final array.
	return finalBuild(pieces, rows, columns);
    }

    private Piece makePiece(BufferedImage image,
		Point nw, Point sw, Point ne, Point se,
			    int tWidth, int tHeight) {
	int minX = Math.min(nw.x, sw.x);
	int maxX = Math.max(ne.x, se.x);
	int minY = Math.min(nw.y, ne.y);
	int maxY = Math.max(sw.y, se.y);
	int width  = maxX - minX + 1;
	int height = maxY - minY + 1;
	if (minX + width > tWidth) {
	    width = tWidth - minX;
	}
	if (minY + height > tHeight) {
	    height = tHeight-minY;
	}

	int[] data = new int[width*height];
	data = image.getRGB(minX, minY, width, height, data, 0, width);

	// Mask out anything outside the lines.
	maskOutside(data, nw, ne, minX, minY, width, height);
	maskOutside(data, ne, se, minX, minY, width, height);
	maskOutside(data, se, sw, minX, minY, width, height);
	maskOutside(data, sw, nw, minX, minY, width, height);

	int rotation = ((int) (Math.random()*4)) * 90;
	return
	    new Piece(data, minX, minY, width, height, tWidth, tHeight,
			rotation);
    }

    private void maskOutside(int[] data, Point p1, Point p2,
			int minX, int minY, int width, int height) {
	p1.translate(-minX, -minY);
	p2.translate(-minX, -minY);
	// y = mx + b
	// N = numerator; D = denominator
	int mN = p2.y - p1.y;
	int mD = p2.x - p1.x;
	int bN = mD * p1.y - mN * p1.x;
	// Since bD == mD,
	// y = mN*x/mD + bN/mD
	//   = (mN*x + bN)/mD
	// y*mD = mN*x + bN
	// make transparent if y*mD > mN*x + bN

	for (int j = 0; j < height; j++) {
	    // int y = j+minY;
	    int y = j;
	    for (int i = 0; i < width; i++) {
		// int x = i+minX;
		int x = i;
		if (y*mD < mN*x + bN) {
		    data[j*width+i] = 0;
		}
	    }
	}
	p1.translate(minX, minY);
	p2.translate(minX, minY);
    }
}
