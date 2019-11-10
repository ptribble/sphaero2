package uk.co.petertribble.sphaero2;

import java.awt.Image;
import javax.swing.JProgressBar;

/**
 * A JigsawCutter determines how pieces are to be cut from the image.
 *
 * <p> Suitable piece rotations depend on how the pieces are cut.
 * Therefore, a JigsawCutter must also provide hints for how a piece may be
 * oriented.  (<i>This feature is not yet implemented.</i>)
 */
public abstract class JigsawCutter {

    public static final int DEFAULT_PIECES = 100;
    public static final int MIN_PIECES = 4;
    public static final int MAX_PIECES = 1000;
    public int prefPieces;
    public JProgressBar jp;
    public int iprogress;

    public String toString() {
	return getName();
    }

    /**
     * Returns a name for this cutting algorithm, suitable for display in a
     * user interface.  The name is expected to be one line, and at most
     * roughly 30 characters long.
     */
    public abstract String getName();

    /**
     * Returns a description of how this cutter will work, suitable for
     * display in a user interface.  The description may contain several
     * sentences, and is expected to be about a paragraph long.
     */
    public abstract String getDescription();

    /**
     * Cuts the given Image into Pieces, and returns them.  This is a
     * potentially time-consuming operation, and should not be run in the AWT
     * thread.
     */
    public abstract Piece[] cut(Image image);

    /**
     * Sets the preferred number of pieces to create.  The actual number of
     * pieces may differ slightly, depending on the specific cutting
     * algorithm. If out of range, clamp to the allowed range.
     *
     * @param prefPieces the preferred number of pieces; the cutter will try
     * to produce close to this many
     */
    public void setPreferredPieceCount(int prefPieces) {
	this.prefPieces = prefPieces;
	if (prefPieces < MIN_PIECES) {
	    prefPieces = MIN_PIECES;
	}
	if (prefPieces > MAX_PIECES) {
	    prefPieces = MAX_PIECES;
	}
    }

    /**
     * Associate a JProgressBar that can be used to display progress of
     * generating the pieces.
     */
    public void setJProgressBar(JProgressBar jp) {
	this.jp = jp;
    }

    /**
     * Allocates neighbours and builds the final array.
     */
    public Piece[] finalBuild(Piece[][] pieces, int rows, int columns) {
	Piece[] ret = new Piece[rows*columns];
	for (int j = 0; j < rows; j++) {
	    for (int i = 0; i < columns; i++) {
		if (i > 0) {
		    pieces[i][j].addNeighbor(pieces[i-1][j]);
		}
		if (j > 0) {
		    pieces[i][j].addNeighbor(pieces[i][j-1]);
		}
		if (i < columns-1) {
		    pieces[i][j].addNeighbor(pieces[i+1][j]);
		}
		if (j < rows-1) {
		    pieces[i][j].addNeighbor(pieces[i][j+1]);
		}
		ret[j*columns+i] = pieces[i][j];
	    }
	}
	return ret;
    }
}
