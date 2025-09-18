package uk.co.petertribble.sphaero2;

import java.awt.image.BufferedImage;
import javax.swing.JProgressBar;

/**
 * A JigsawCutter determines how pieces are to be cut from the image.
 *
 * <p> Suitable piece rotations depend on how the pieces are cut.
 * Therefore, a JigsawCutter must also provide hints for how a piece may be
 * oriented.  (<i>This feature is not yet implemented.</i>)
 */
public abstract class JigsawCutter {

    /**
     * The default number of pieces, 100.
     */
    public static final int DEFAULT_PIECES = 100;

    /**
     * The minimum number of pieces, 4, that the gui will allow the user
     * to choose.
     */
    public static final int MIN_PIECES = 4;

    /**
     * The maximum number of pieces, 1000, that the gui will allow the user
     * to choose.
     */
    public static final int MAX_PIECES = 1000;

    /**
     * The number of pieces the user requests the picture be cut into.
     * The game will attempt to create close to that many pieces.
     */
    public int prefPieces = DEFAULT_PIECES;
    /**
     * A bar to show progress to the user.
     */
    public JProgressBar jp;
    /**
     * How far the progress bar has got.
     */
    public int iprogress;
    /**
     * The maximum value progress can reach.
     */
    public int progressmax;

    @Override
    public final String toString() {
	return getName();
    }

    /**
     * Returns a name for this cutting algorithm, suitable for display in a
     * user interface.  The name is expected to be one line, and at most
     * roughly 30 characters long.
     *
     * @return a short name for this cutter
     */
    public abstract String getName();

    /**
     * Returns a description of how this cutter will work, suitable for
     * display in a user interface.  The description may contain several
     * sentences, and is expected to be about a paragraph long.
     *
     * @return a longer description of this cutter
     */
    public abstract String getDescription();

    /**
     * Cuts the given Image into Pieces, and returns them.  This is a
     * potentially time-consuming operation, and should not be run in the AWT
     * thread.
     *
     * @param image the image to be cut
     *
     * @return the array of cut Pieces
     */
    public abstract Piece[] cut(BufferedImage image);

    /**
     * Sets the preferred number of pieces to create.  The actual number of
     * pieces may differ slightly, depending on the specific cutting
     * algorithm. If out of range, clamp to the allowed range.
     *
     * @param nPieces the preferred number of pieces; the cutter will try
     * to produce close to this many
     */
    public void setPreferredPieceCount(final int nPieces) {
	prefPieces = nPieces;
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
     *
     * @param njp the progress bar to update while cutting is in progress
     */
    public void setJProgressBar(final JProgressBar njp) {
	jp = njp;
	if (progressmax > 0) {
	    jp.setMaximum(progressmax);
	    jp.setValue(iprogress);
	}
    }

    /**
     * Start generating. If there's a progress bar, it will be set to
     * zero.
     *
     * @param nprogressmax the anticipated number of steps
     */
    public void startProgress(final int nprogressmax) {
	progressmax = nprogressmax;
	iprogress = 0;
	if (jp != null) {
	    jp.setMaximum(progressmax);
	    jp.setValue(iprogress);
	}
    }

    /**
     * Update progress of this cutting operation.
     */
    public void updateProgress() {
	iprogress++;
	if (jp != null) {
	    jp.setValue(iprogress);
	}
    }

    /**
     * Allocates neighbours and builds the final array.
     *
     * @param pieces the Pieces array
     * @param rows the number of rows
     * @param columns the number of columns
     *
     * @return the final array of Pieces
     */
    public Piece[] finalBuild(final Piece[][] pieces,
			      final int rows, final int columns) {
	Piece[] ret = new Piece[rows * columns];
	for (int j = 0; j < rows; j++) {
	    for (int i = 0; i < columns; i++) {
		if (i > 0) {
		    pieces[i][j].addNeighbor(pieces[i - 1][j]);
		}
		if (j > 0) {
		    pieces[i][j].addNeighbor(pieces[i][j - 1]);
		}
		if (i < columns - 1) {
		    pieces[i][j].addNeighbor(pieces[i + 1][j]);
		}
		if (j < rows - 1) {
		    pieces[i][j].addNeighbor(pieces[i][j + 1]);
		}
		ret[j * columns + i] = pieces[i][j];
	    }
	}
	return ret;
    }
}
