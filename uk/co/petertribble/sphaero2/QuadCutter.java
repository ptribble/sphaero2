package uk.co.petertribble.sphaero2;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.PixelGrabber;

/**
 * Cuts pieces into random quadrilaterals.  Pieces are arranged into rough
 * rows and columns, with their edges tilting at various angles.
 */
public class QuadCutter extends JigsawCutter {

    private static final double defWHRatio = 1.2;
    private double widthToHeightRatio;

    @Override
    public String getName() {
	return "Quads";
    }

    @Override
    public String getDescription() {
	return "Cuts the image into random quadrilaterals.";
    }

    /**
     * Creates a QuadCutter.  It will produce the default number of pieces,
     * each about 1.2 times as wide as they are tall.
     */
    public QuadCutter() {
	this(DEFAULT_PIECES, defWHRatio);
    }

    /**
     * Creates a QuadCutter.  Each piece will be about 1.2 times as wide as
     * it is tall.
     *
     * @param prefPieces the preferred number of pieces; the cutter will try
     * to produce close to this many
     */
    public QuadCutter(int prefPieces) {
	this(prefPieces, defWHRatio);
    }

    /**
     * Creates a QuadCutter.
     *
     * @param prefPieces the preferred number of pieces; the cutter will try
     * to produce close to this many
     *
     * @param whRatio the preferred width/height ratio
     *
     * @throws IllegalArgumentException if whRatio is less than or equal to 0.0
     */
    public QuadCutter(int prefPieces, double whRatio) {
	setPreferredPieceCount(prefPieces);
	if (whRatio <= 0.0) {
	    throw new IllegalArgumentException
				("Invalid width/height ratio: "+whRatio);
	}
	this.widthToHeightRatio = whRatio;
    }

    @Override
    public Piece[] cut(Image image) {
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
		Math.sqrt(widthToHeightRatio * prefPieces * height / width));
	int columns = (int) Math.round(prefPieces / rows);

	jp.setMaximum(rows*columns);
	jp.setValue(iprogress);

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
		if ((i > 0) && (i < columns)) {
		    x += Math.random()*(2*wVary+1) - wVary;
		}
		if ((j > 0) && (j < rows)) {
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
		iprogress++;
		jp.setValue(iprogress);
	    }
	}

	// Set each piece's neighbors, and build the final array.
	return finalBuild(pieces, rows, columns);
    }

    private Piece makePiece(Image image,
		Point nw, Point sw, Point ne, Point se,
			    int tWidth, int tHeight) {
	int minX = Math.min(nw.x, sw.x);
	int maxX = Math.max(ne.x, se.x);
	int minY = Math.min(nw.y, ne.y);
	int maxY = Math.max(sw.y, se.y);
	int width  = maxX - minX + 1;
	int height = maxY - minY + 1;

	int[] data = new int[width*height];
	PixelGrabber grabber =
	    new PixelGrabber(image, minX, minY, width, height, data, 0, width);
	try { grabber.grabPixels(); }
	catch (InterruptedException ex) {
	    System.out.println("interrupted while grabbing");
	}

	// Mask out anything outside the lines.
	maskOutside(data, nw, ne, minX, minY, width, height);
	maskOutside(data, ne, se, minX, minY, width, height);
	maskOutside(data, se, sw, minX, minY, width, height);
	maskOutside(data, sw, nw, minX, minY, width, height);

	int rotation = ((int)(Math.random()*4)) * 90;
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
