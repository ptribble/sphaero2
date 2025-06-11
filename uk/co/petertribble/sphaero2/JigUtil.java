package uk.co.petertribble.sphaero2;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JPanel;

/**
 * Common utility methods.
 */
public final class JigUtil {

    private static final JPanel PTRACKER = new JPanel(false);
    private static final MediaTracker TRACKER = new MediaTracker(PTRACKER);

    /*
     * This class should never be instantiated.
     */
    private JigUtil() {
    }

    /**
     * Ensures that the given Image has been loaded.  The current thread
     * will pause until all of the image data is in memory.
     *
     * @param image the Image to track loading progress of
     */
    public static void ensureLoaded(Image image) {
	int id = 0;
	TRACKER.addImage(image, id);
	try {
	    TRACKER.waitForID(id, 0);
	} catch (InterruptedException e) { }
	TRACKER.removeImage(image, id);
    }

    /**
     * Rescale the image to fit on the screen, allowing for a border.
     *
     * @param image the input Image
     *
     * @return the resized Image
     */
    public static BufferedImage resizedImage(BufferedImage image) {
	Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	int wtarg = 3 * screen.width / 5;
	int htarg = 3 * screen.height / 5;
	/*
	 * If already small enough, just recreate the Image. The reason for
	 * this is that ImageIO uses type 0, which gives a very noticeable
	 * performance hit. So we always explicitly rewrite to ARGB.
	 */
	if (wtarg > image.getWidth() && htarg > image.getHeight()) {
	    wtarg = image.getWidth();
	    htarg = image.getHeight();
	}
	// new image of the desired size
	BufferedImage nimage = new BufferedImage(wtarg, htarg,
		image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB
						: image.getType());
	Graphics2D g2 = nimage.createGraphics();
	g2.drawImage(image, 0, 0, wtarg, htarg, null);
	g2.dispose();
	return nimage;
    }

    /**
     * Returns a random image file from the given folder, including any
     * subfolders. The algorithm picks a file randomly from the folder. If
     * it is an image file, it returns it. If it is a subfolder, it repeats
     * the algorithm on the subfolder, continuing until it finds an image
     * file or a file that is neither image nor folder.
     *
     * @param folder the directory to search for images
     *
     * @return a random image File
     *
     * @throws FileNotFoundException if no image file could be found after 10
     *   attempts
     */
    public static File getRandomImageFile(File folder)
	    throws FileNotFoundException {
	int attempts = 0;
	File file;
	FileFilter ff = new FileFilter() {
	    @Override
	    public boolean accept(File f) {
		return f.isDirectory() || isImage(f);
	    }
	};
	do {
	    file = folder;
	    while (file.isDirectory()) {
		/*
		 * We have to check here to catch both going round the outer
		 * loop and the continue out of the inner loop.
		 */
		if (attempts >= 10) {
		    throw new FileNotFoundException(
					"No image found after 10 attempts");
		}
		File[] files = file.listFiles(ff);
		if (files.length == 0) {
		    file = folder;
		    attempts++;
		    continue;
		}
		int idx = ThreadLocalRandom.current().nextInt(files.length);
		file = files[idx];
	    }
	    attempts++;
	} while (!isImage(file));
	return file;
    }

    /**
     * Returns whether the given file is an image file. For now, just
     * check whether the file extension is JPG, GIF, or PNG.
     *
     * @param file the File to check
     *
     * @return true if the given File is of a recognized image type
     */
    public static boolean isImage(File file) {
	String name = file.getName();
	int idot = name.lastIndexOf('.');
	if (idot < 0) {
	    // no extension
	    return false;
	}
	if (name.lastIndexOf("thumb") != -1) {
	    // thumbnail file
	    return false;
	}
	String ext = name.substring(idot + 1);
	return
	    "jpg".equalsIgnoreCase(ext)
	    || "gif".equalsIgnoreCase(ext)
	    || "png".equalsIgnoreCase(ext);
    }

    /**
     * The about message.
     *
     * @return the about message
     */
    public static String aboutMsg() {
	return "<html>Sphaero2 Jigsaw Puzzle.<br>"
	    + "Original by Paul Brinkley, 2003.<br>"
	    + "Updated by Peter Tribble, 2010-2022.</html>";
    }

    /**
     * Create one line in a table.
     *
     * @param c1 a character
     * @param s2 the description for c1
     *
     * @return a formatted line ready to put in a table
     */
    public static String tableLine(char c1, String s2) {
	return "<tr><td>" + c1 + "</td><td> " + s2 + "</td></tr>";
    }

    /**
     * The help message.
     *
     * @return the full help text
     */
    public static String helpMsg() {
	return "<html>Drag pieces with the mouse to fit them together.  If"
	    + " they do, they'll join and move as a unit from then on."
	    + "<p> Keyboard commands: <br>"
	    + "<table>"
	    + tableLine(JigsawPuzzle.ROTATE_LEFT,
			"rotate piece left 90 degrees")
	    + tableLine(JigsawPuzzle.ROTATE_RIGHT,
			"rotate piece right 90 degrees")
	    + tableLine(JigsawPuzzle.SHUFFLE_KEY,
			"shuffle all pieces (good for finding pieces "
			+ "accidentally moved off the board)")
	    + tableLine(JigsawPuzzle.PUSH_KEY,
			"push the top piece to the back (handy if it's "
			+ "hiding other pieces)")
	    + tableLine(JigsawPuzzle.PREV_BG,
			"change background to previous color")
	    + tableLine(JigsawPuzzle.NEXT_BG,
			"change background to next color")
	    + tableLine(JigsawPuzzle.CLEAR,
			"toggle clear mode; mouse now drags over spaces to "
			+ "be cleared of pieces; cleared pieces are placed "
			+ "randomly elsewhere")
	    + tableLine(JigsawPuzzle.HIDE,
			"toggle hidden mode to pause or unpause the puzzle")
	    + "</table></html>";
    }
}
