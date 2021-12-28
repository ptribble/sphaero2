package uk.co.petertribble.sphaero2;

import java.util.Set;
import java.util.HashSet;

/**
 * A set of joined pieces of a jigsaw puzzle.  It knows the same things a
 * Piece knows, generally by way of keeping a set of the simple Pieces it
 * contains.
 *
 * <p> When two or more Pieces are put together, the result is another
 * Piece object.  Pieces that weren't created this way are referred to here
 * as <i>atomic pieces</i>.
 */
public class MultiPiece extends Piece {

    // Subpieces.  Records of subs are necessary because of rotation.
    // Without it, subpiece images can be thrown away after they're combined
    // to form the joined piece.  With rotation, however, the bevels will
    // need to be repainted, and so the original image data is still needed.
    // Subs are also necessary if someday I decide to support splitting
    // joined pieces back into their parts.
    private Set <Piece> subs;

    // Constructor and fields -----------------------------------------------

    /**
     * Creates a new MultiPiece.
     * @param subs A set of Pieces used directly by MultiPiece,
     *   should not be modified afterward
     * @param imageX X position of image relative to entire puzzle
     * @param imageY Y position of image relative to entire puzzle
     * @param imageWidth width of original image
     * @param imageHeight height of original image
     * @param totalWidth width of the entire puzzle in pixels
     * @param totalHeight height of the entire puzzle in pixels
     * @param rotation initial rotation
     */
    public MultiPiece(Set <Piece> subs,
		int imageX, int imageY,
		int imageWidth, int imageHeight,
		int totalWidth, int totalHeight,
		int rotation) {
	super(null, imageX, imageY, imageWidth, imageHeight,
		totalWidth, totalHeight);
	this.subs = subs;
	forceSetRotation(rotation);
    }

    // For a MultiPiece, original image size and data work differently.  Size
    // is equivalent to the smallest rectangle bounding all subpieces.  Image
    // data is always null; it's always rebuilt from the subpieces.

    // Accessors ------------------------------------------------------------

    @Override
    public void setRotation(int rot) {
	for (Piece piece : subs) {
	    piece.setRotation(rot);
	}
	// Call this last, so it will rebuild this image from the subs
	super.setRotation(rot);
    }

    @Override
    public String toString() {
	return "Multi"+super.toString()+"[pieces="+subs.size()+"]";
    }

    // Joining pieces -------------------------------------------------------

    /**
     * Joins the given main Piece with the list of Pieces, and returns a new
     * Piece.  The new Piece's location and orientation are based on those of
     * the main Piece.
     *
     * @param main main Piece
     * @param others other Pieces
     *
     * @return the combined MultiPiece
     */
    protected static MultiPiece join(Piece main, Set <Piece> others) {
	Set <Piece> neighbors = new HashSet <Piece> ();
	neighbors.addAll(main.neighbors);
	int mainPX = main.getPuzzleX();
	int mainPY = main.getPuzzleY();

	// Compute a bounding rectangle for all pieces.
	// Build the neighbors set at the same time.
	int minX = main.getImageX();
	int minY = main.getImageY();
	int maxX = minX + main.getImageWidth() - 1;
	int maxY = minY + main.getImageHeight() - 1;
	for (Piece piece : others) {
	    int minXT = piece.getImageX();
	    int minYT = piece.getImageY();
	    int maxXT = minXT + piece.getImageWidth() - 1;
	    int maxYT = minYT + piece.getImageHeight() - 1;
	    minX = Math.min(minX, minXT);
	    minY = Math.min(minY, minYT);
	    maxX = Math.max(maxX, maxXT);
	    maxY = Math.max(maxY, maxYT);
	    neighbors.addAll(piece.neighbors);
	}
	int width = maxX-minX+1;
	int height = maxY-minY+1;

	// new piece neighbors = main/others' neighbors, minus themselves
	neighbors.remove(main);
	neighbors.removeAll(others);

	// Build the set of subpieces.
	Set <Piece> subs = new HashSet <Piece> ();
	addSubs(subs, main);
	for (Piece piece : others) {
	    addSubs(subs, piece);
	}

	// Make the new Piece, and set its data, size, and positions.
	MultiPiece newPiece = new MultiPiece(subs,
					 minX, minY,    // image position
					 width, height, // image size
					 main.getTotalWidth(),
					 main.getTotalHeight(),
					 main.getRotation());

	// Set the new piece position so that the main piece doesn't appear to
	// move.
	int dx = newPiece.getRotatedX() - main.getRotatedX();
	int dy = newPiece.getRotatedY() - main.getRotatedY();
	newPiece.setPuzzlePosition(mainPX+dx, mainPY+dy);

	// Add each piece as a neighbor, and change each piece's neighbors
	//  (remove main and others, and add newPiece)
	for (Piece piece : neighbors) {
	    newPiece.addNeighbor(piece);
	    piece.removeNeighbor(main);
	    for (Piece other : others) {
		piece.removeNeighbor(other);
	    }
	    piece.addNeighbor(newPiece);
	}

	return newPiece;
    }

    private static void addSubs(Set <Piece> subset, Piece piece) {
	if (piece instanceof MultiPiece) {
	    subset.addAll(((MultiPiece) piece).subs);
	} else {
	    subset.add(piece);
	}
    }

    // This is effectly image blending with support for 0 or max
    // transparency, with int arrays as data.  I couldn't find a tool for
    // doing this in JDK.
    /**
     * Overlays the current image in the given Piece onto the data array.
     * The data array's image location is given by (dataX,dataY).  Its image
     * size is given by (width,height).  The Piece's image is assumed to fit
     * entirely within the data image rectangle.
     *
     * @param data the array to overlay the Piece into
     * @param dataX the x coordinate of the data array
     * @param dataY the y cordinate of the data array
     * @param width the width of the data array
     * @param height the height of the data array
     * @param piece the Piece to overlay into the data array
     */
    protected static void overlay(int[] data, int dataX, int dataY,
				   int width, int height, Piece piece) {
	int pieceX = piece.getRotatedX();
	int pieceY = piece.getRotatedY();
	int pieceW = piece.getCurrentWidth();
	int pieceH = piece.getCurrentHeight();

	int[] newData = piece.curData;

	// Fold it into the data.  ReSPECT mah transparensah!
	int offset = (pieceY-dataY) * width + (pieceX-dataX);

	for (int i = 0; i < pieceH; i++) {
	    int iNDOffset = i*pieceW;
	    int iDOffset  = i*width;
	    for (int j = 0; j < pieceW; j++) {
		// int ndOff = iNDOffset+j;
		int newDatum = newData[iNDOffset+j];
		if (newDatum != 0) {
		    data[ offset + iDOffset + j ] = newDatum;
		}
	    }
	}
    }

    // 4-way rotation -------------------------------------------------------

    protected void recomputeImageData() {
	setRotatedPosition();
	// System.out.println ("recomputing: "+this);
	int[] data = new int[curWidth*curHeight];
	int rotX = getRotatedX();
	int rotY = getRotatedY();
	for (Piece sub : subs) {
	    overlay(data, rotX, rotY, curWidth, curHeight, sub);
	}
	curData = data;
    }
}
