package uk.co.petertribble.sphaero2;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;
import java.util.HashSet;
import java.util.Set;

// ### Should Pieces be implemented as BufferedImages backed by Rasters,
// rather than images backed by MemoryImageSources?

// ### Classic piece edges are problematic. First of all, the corners are
// weird.  Secondly, the edges are probably too close together. This is
// causing the bevels to overlap very slightly, so they don't look quite
// right.

/**
 * A single piece of a jigsaw puzzle. Each piece knows the portion of the
 * image it contains, how its edges are to be drawn, and what its
 * neighboring pieces are.
 *
 * When two or more Pieces are put together, the result is another
 * Piece object, a MultiPiece.
 *
 * @see MultiPiece
 */
public class Piece {
    // Class constants ------------------------------------------------------

    /*
     * A Piece must be within this many pixels of "perfect" to be considered
     * close.
     */
    private static final int NEARBY = 7;

    // Constructor and fields -----------------------------------------------

    /**
     * Creates a new Piece.  No initial rotation is done.  (This is needed
     * by MultiPiece, which needs to set its subpieces before rotating.)
     * @param data image data
     * @param imageX X position of image relative to entire puzzle
     * @param imageY Y position of image relative to entire puzzle
     * @param imageWidth width of original image
     * @param imageHeight height of original image
     * @param totalWidth the width of the entire picture
     * @param totalHeight the height of the entire picture
     */
    protected Piece(int[] data,
		    int imageX, int imageY,
		    int imageWidth, int imageHeight,
		    int totalWidth, int totalHeight) {
	neighbors = new HashSet<>();
	origData = data; //NOPMD
	this.imageX = imageX;
	this.imageY = imageY;
	this.curWidth = imageWidth;
	this.curHeight = imageHeight;
	this.origWidth = imageWidth;
	this.origHeight = imageHeight;
	this.totalWidth = totalWidth;
	this.totalHeight = totalHeight;
    }

    /**
     * Creates a new Piece.
     * @param data image data
     * @param imageX X position of image relative to entire puzzle
     * @param imageY Y position of image relative to entire puzzle
     * @param imageWidth width of original image
     * @param imageHeight height of original image
     * @param totalWidth the width of the entire picture
     * @param totalHeight the height of the entire picture
     * @param rotation initial rotation
     */
    public Piece(int[] data,
		 int imageX, int imageY,
		 int imageWidth, int imageHeight,
		 int totalWidth, int totalHeight,
		 int rotation) {
	this(data, imageX, imageY, imageWidth, imageHeight,
		totalWidth, totalHeight);
	forceSetRotation(rotation);
    }

    // Location in the image.
    private int imageX;
    private int imageY;

    // Size of the entire image.
    private int totalWidth;
    private int totalHeight;

    // Location in the image adjusted by current rotation.
    private int rotatedX;
    private int rotatedY;

    /**
     * Pieces considered to be neighbors to this one.  They are the only
     * ones that can be fitted to it.
     */
    protected Set<Piece> neighbors;

    /**
     * Original image width.
     */
    protected int origWidth;
    /**
     * Original image height.
     */
    protected int origHeight;
    /*
     * Original image data.
     */
    private int[] origData;

    /**
     * Current width, taking rotation into account.
     */
    protected int curWidth;
    /**
     * Current height, taking rotation into account.
     */
    protected int curHeight;
    /**
     * Current data, taking rotation into account.
     */
    protected int[] curData;

    // Location in the puzzle panel.
    private int puzzleX;
    private int puzzleY;

    // Image for this Piece. null for a MultiPiece
    private Image image;

    // This is measured in integer degrees. 0 is unrotated.  90 is 90
    // degrees clockwise, etc. Must be 0, 90, 180, or 270.
    private int rotation;

    // Accessors ------------------------------------------------------------

    /**
     * Returns this Piece's current rotation.  The rotation is given in
     * integer degrees clockwise, and will always be 0, 90, 180, or 270.
     *
     * @return the Piece's current rotation.
     *
     * @see #setRotation
     */
    public int getRotation() {
	return rotation;
    }

    /**
     * Sets this Piece's current rotation.  The rotation is given in integer
     * degrees clockwise, and should always be 0, 90, 180, or 270.
     * If the new rotation is different, this Piece's image data will be
     * recomputed.
     *
     * @param rot The new rotation
     *
     * @see #getRotation
     */
    public void setRotation(int rot) {
	if (rot != rotation) {
	    forceSetRotation(rot);
	}
    }

    /**
     * Sets this Piece's current rotation.  This
     * method forces a recompute of the image.
     *
     * @param rot The new rotation
     */
    protected void forceSetRotation(int rot) {
	rotation = rot;
	recomputeImageData();
	if (image != null) {
	    image.flush();
	}
	image = Toolkit.getDefaultToolkit().createImage(
		new MemoryImageSource(
				curWidth, curHeight, curData, 0, curWidth));
    }

    /**
     * Sets this Piece's upper-left position relative to the upper-left
     * position of the JigsawPuzzle.
     *
     * @param x The Piece's new x position
     * @param y The Piece's new y position
     */
    public void setPuzzlePosition(int x, int y) {
	this.puzzleX = x;
	this.puzzleY = y;
    }

    /**
     * Returns this Piece's current height in pixels.  This is the height of
     * the smallest rectangle containing all of this Piece's image data at
     * its current rotation.
     *
     * @return the Piece's current height in pixels
     */
    public int getCurrentHeight() {
	return curHeight;
    }

    /**
     * Returns this Piece's current width in pixels.  This is the width of
     * the smallest rectangle containing all of this Piece's image data at
     * its current rotation.
     *
     * @return the Piece's current width in pixels
     */
    public int getCurrentWidth() {
	return curWidth;
    }

    /**
     * Returns the width of the entire picture.
     *
     * @return the entire puzzle's current width in pixels
     */
    public int getTotalWidth() {
	return totalWidth;
    }

    /**
     * Returns the height of the entire picture.
     *
     * @return the entire puzzle's current height in pixels
     */
    public int getTotalHeight() {
	return totalHeight;
    }

    /**
     * Returns this Piece's image height in pixels.  This is the height of
     * the smallest rectangle containing all of this Piece's image data in
     * its original orientation.
     *
     * @return the Piece's current image height in pixels
     */
    public int getImageHeight() {
	return origHeight;
    }

    /**
     * Returns this Piece's image width in pixels.  This is the width of the
     * smallest rectangle containing all of this Piece's image data in its
     * original orientation.
     *
     * @return the Piece's current image width in pixels
     */
    public int getImageWidth() {
	return origWidth;
    }

    /**
     * Returns this Piece's X position in the original image.
     *
     * @return the Piece's X position in the original image
     */
    public int getImageX() {
	return imageX;
    }

    /**
     * Returns this Piece's Y position in the original image.
     *
     * @return the Piece's Y position in the original image
     */
    public int getImageY() {
	return imageY;
    }

    /**
     * Returns this Piece's X position in the original image, modified by
     * its current rotation.  The origin is the center of rotation.
     *
     * @return the Piece's rotated X position in the original image
     */
    public int getRotatedX() {
	return rotatedX;
    }

    /**
     * Returns this Piece's Y position in the original image, modified by
     * its current rotation.  The origin is the center of rotation.
     *
     * @return the Piece's rotated Y position in the original image
     */
    public int getRotatedY() {
	return rotatedY;
    }

    /**
     * Returns this Piece's X position in the puzzle.
     *
     * @return this Piece's X position
     */
    public int getPuzzleX() {
	return puzzleX;
    }

    /**
     * Returns this Piece's Y position in the puzzle.
     *
     * @return this Piece's Y position
     */
    public int getPuzzleY() {
	return puzzleY;
    }

    /**
     * Returns this Piece's current image.  This will be the Piece's portion
     * of the original image, rotated by this Piece's current rotation.
     *
     * @return this Piece's portion of the overall image
     */
    public Image getImage() {
	return image;
    }

    /**
     * Adds a Piece to this Piece's set of neighbors.
     *
     * @param neighbor the Piece to add to this Piece's set of neighbors
     */
    public void addNeighbor(Piece neighbor) {
	neighbors.add(neighbor);
    }

    /**
     * Removes the given Piece from this Piece's set of neighbors.
     *
     * @param neighbor the Piece to remove from this Piece's set of neighbors
     */
    public void removeNeighbor(Piece neighbor) {
	neighbors.remove(neighbor);
    }

    /**
     * Moves this Piece to the given location, relative to the puzzle
     * panel's upper-left corner.
     *
     * @param x The Piece's new x position
     * @param y The Piece's new y position
     */
    public void moveTo(int x, int y) {
	setPuzzlePosition(x, y);
    }

    /**
     * Return a String, solely for debugging purposes.
     *
     * @return a String describing the properties of this Piece
     */
    @Override
    public String toString() {
	return "Piece[iPos=(" + imageX + "," + imageY + "),"
	    + "iSiz=" + origWidth + "x" + origHeight + ","
	    + "rot=" + rotation + ","
	    + "rPos=(" + rotatedX + "," + rotatedY + "),"
	    + "pPos=(" + puzzleX + "," + puzzleY + ")]";
    }

    /**
     * Draws this Piece in the given Graphics object.  The current image
     * will be drawn, at this Piece's current puzzle position.
     *
     * @param g the Graphics object to draw to
     */
    protected void draw(Graphics g) {
	Image img = getImage();
	if (img != null) {
	    g.drawImage(img, getPuzzleX(), getPuzzleY(), null);
	}
    }

    /**
     * Returns whether this Piece currently contains the given point,
     * relative to the puzzle panel's upper-left corner.
     *
     * @param x The x coordinate to be checked
     * @param y The y coordinate to be checked
     *
     * @return true if the given coordinates are inside the current Piece
     */
    public boolean contains(int x, int y) {
	int puzX = getPuzzleX();
	int puzY = getPuzzleY();
	int w = getCurrentWidth();
	int h = getCurrentHeight();
	return
	    (puzX <= x) && (x <= (puzX + w - 1))
	    && (puzY <= y) && (y <= (puzY + h - 1))
	    && (getAlpha(x - puzX, y - puzY) != 0);
    }

    /**
     * Returns the alpha (transparency) value at the given coordinates in
     * the current image data.
     *
     * @param x The x coordinate to be checked
     * @param y The y coordinate to be checked
     *
     * @return the alpha transparency at the given coordinates
     */
    protected int getAlpha(int x, int y) {
	int pixel = curData[y * curWidth + x];
	return (pixel >> 24) & 0xff;
    }

    /**
     * Returns whether this piece is located and oriented close enough to
     * the given Piece to be fitted.
     *
     * @param piece The other Piece to check for closeness
     *
     * @return true if this Piece and the given Piece are located and oriented
     * close enough together to be fitted
     */
    protected boolean isCloseTo(Piece piece) {
	// Don't even bother if they're not aligned.
	if (piece.getRotation() != rotation) {
	    return false;
	}
	int puzXD = getPuzzleX() - piece.getPuzzleX();
	int puzYD = getPuzzleY() - piece.getPuzzleY();
	int rotXD = getRotatedX() - piece.getRotatedX();
	int rotYD = getRotatedY() - piece.getRotatedY();
	return
	    (Math.abs(puzXD - rotXD) <= NEARBY)
	    && (Math.abs(puzYD - rotYD) <= NEARBY);
    }

    // Joining pieces -------------------------------------------------------

    /**
     * Checks whether any of this Piece's neighbors are located and oriented
     * close enough to be joined to this one.
     *
     * @return an array of Pieces, or null if no neighbors were close enough;
     *   if the array is non-null, the first Piece will be the new one;
     *   subsequent Pieces will be the ones it was built from
     */
    public Piece[] join() {
	Set<Piece> close = new HashSet<>();
	for (Piece piece : neighbors) {
	    if (piece.isCloseTo(this)) {
		close.add(piece);
	    }
	}
	if (close.isEmpty()) {
	    return null;
	}

	// We can't just return the new MultiPiece, because the JigsawPuzzle
	// needs to know what pieces the new one was formed from that are
	// currently in its list.  These might include other MultiPieces, which
	// wouldn't be in the new Piece's subpiece list.
	Piece newPiece = MultiPiece.join(this, close);
	Piece[] ret = new Piece[close.size() + 2];
	ret[0] = newPiece;
	ret[1] = this;
	this.image.flush();
	int i = 2;
	for (Piece piece : close) {
	    ret[i] = piece;
	    piece.image.flush();
	    i++;
	}
	System.gc();
	return ret;
    }


    // Bevel drawing --------------------------------------------------------

    /**
     * Draws bevels on data.  Check every opaque pixel's NW and SE
     * neighbors.  If NW is transparent and SE is opaque, brighten the
     * central pixel.  If it's the other way around, darken it.  If both or
     * neither are transparent, leave it alone.
     *
     * @param data the image data
     * @param width the width of the image data
     * @param height the height of the image data
     */
    private static void bevel(int[] data, int width, int height) {
	// Scan diagonal NW-SE lines.  The first and last lines can be skipped.
	// moved these out of the loop
	boolean nw; // true iff that pixel is opaque
	boolean c; // true iff that pixel is opaque
	boolean se; // true iff that pixel is opaque
	for (int i = 0; i < width + height - 3; i++) {
	    nw = false;
	    int x = Math.max(0, i - height + 2);
	    int y = Math.max(0, height - i - 2);
	    c = (((data[y * width + x] >> 24) & 0xff) > 0);
	    while ((x < width) && (y < height)) {
		if ((x + 1 < width) && (y + 1 < height)) {
		    se = ((data[(y + 1) * width + x + 1] >> 24) & 0xff) > 0;
		} else {
		    se = false;
		}
		if (c) {
		    int datum = data[y * width + x];
		    if (nw && !se) {
			data[y * width + x] = darker(datum);
		    } else if (!nw && se) {
			data[y * width + x] = brighter(datum);
		    }
		}
		nw = c;
		c = se;
		x++; y++;
	    }
	}
    }

    // This mimics Color.brighter() and Color.darker(). They multiply or
    // divide R/G/B by 0.7, and trim them to 0 or 255 if needed. I'm going
    // to use 7/10 (so it's int arithmetic), and not use Math. I don't quite
    // trust inlining yet. And I certainly don't want to make scads of Color
    // objects for each pixel. It's bad enough these are methods, and not
    // inlined in bevel().
    private static final int FN = 10;
    private static final int FD = 7;
    private static final int MAXB = 255 * FD / FN;
    private static int brighter(int val) {
	int r = (val >> 16) & 0xff;
	int g = (val >> 8) & 0xff;
	int b = val & 0xff;

	// Black goes to #030303 gray
	if (r == 0 && g == 0 && b == 0) {
	    return 0xff030303;
	}
	r = r < 3 ? 3 : r;
	g = g < 3 ? 3 : g;
	b = b < 3 ? 3 : b;

	r = r >= MAXB ? 255 : r * FN / FD;
	g = g >= MAXB ? 255 : g * FN / FD;
	b = b >= MAXB ? 255 : b * FN / FD;
	return ((((0xff00 | r) << 8) | g) << 8) | b;
    }
    private static int darker(int val) {
	int r = (val >> 16) & 0xff;
	int g = (val >> 8) & 0xff;
	int b = val & 0xff;
	r = r * FD / FN;
	g = g * FD / FN;
	b = b * FD / FN;
	return ((((0xff00 | r) << 8) | g) << 8) | b;
    }

    // 4-way rotation -------------------------------------------------------

    /**
     * Sets this Piece's rotated position and size, based on its current
     * rotation.  The entire puzzle is rotated about the origin, and then
     * translated so that its new upper left corner is at the origin.  Each
     * piece's rotated position is then defined by its new upper left corner
     * in the rotated puzzle.
     */
    protected void setRotatedPosition() {
	if (rotation == 0) {
	    rotatedX = imageX; rotatedY =  imageY;
	    curWidth = origWidth;
	    curHeight = origHeight;
	} else if (rotation == 90) {
	    rotatedX = totalHeight - imageY - origHeight;
	    rotatedY = imageX;
	    curWidth = origHeight;
	    curHeight = origWidth;
	} else if (rotation == 180) {
	    rotatedX = totalWidth - imageX - origWidth;
	    rotatedY = totalHeight - imageY - origHeight;
	    curWidth = origWidth;
	    curHeight = origHeight;
	} else if (rotation == 270) {
	    rotatedX = imageY;
	    rotatedY = totalWidth - imageX - origWidth;
	    curWidth = origHeight;
	    curHeight = origWidth;
	}
    }

    /**
     * Recomputes this Piece's current image data and size from its original
     * image data and rotation.
     */
    protected void recomputeImageData() {
	setRotatedPosition();
	if (rotation == 0) {
	    curData = origData.clone();
	} else if (rotation == 90) {
	    curData = new int[origData.length];
	    for (int i = 0; i < curWidth; i++) {
		for (int j = 0; j < curHeight; j++) {
		    curData[j * curWidth + i] =
			origData[(origHeight - i - 1) * origWidth + j];
		}
	    }
	} else if (rotation == 180) {
	    curData = new int[origData.length];
	    /*
	     * for (int i = 0; i < curWidth; i++)
	     * for (int j = 0; j < curHeight; j++)
	     * curData[j*curWidth+i] =
	     * origData[(origHeight-j-1)*origWidth + (origWidth-i-1)];
	     */
	    // it's just a reverse
	    for (int i = 0; i < origData.length; i++) {
		curData[i] = origData[origData.length - i - 1];
	    }
	} else if (rotation == 270) {
	    curData = new int[origData.length];
	    for (int i = 0; i < curWidth; i++) {
		for (int j = 0; j < curHeight; j++) {
		    curData[j * curWidth + i] =
			origData[i * origWidth + origWidth - j - 1];
		}
	    }
	}
	bevel(curData, curWidth, curHeight);
	image = Toolkit.getDefaultToolkit().createImage(
	    new MemoryImageSource(curWidth, curHeight, curData, 0, curWidth));
    }
}
