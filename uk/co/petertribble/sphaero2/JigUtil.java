package uk.co.petertribble.sphaero2;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;

/**
 * Common utility methods.
 */
public final class JigUtil {

    private final static JPanel trackerPanel = new JPanel(false);
    private final static MediaTracker tracker = new MediaTracker(trackerPanel);

    /*
     * This class should never be instantiated.
     */
    private JigUtil() {
    }

    /**
     * Ensures that the given Image has been loaded.  The current thread
     * will pause until all of the image data is in memory.
     */
    public static void ensureLoaded(Image image) {
	int id = 0;
	tracker.addImage(image, id);
	try { tracker.waitForID(id, 0); }
	catch (InterruptedException e) {
	    System.out.println("INTERRUPTED while loading image");
	}
	tracker.removeImage(image, id);
    }

    /**
     * Rescale the image to fit on the screen, allowing for a border.
     */
    public static BufferedImage resizedImage(BufferedImage image) {
	Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	int wtarg = 3*screen.width/5;
	int htarg = 3*screen.height/5;
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
     * @throws FileNotFoundException if no image file could be found after 10
     *   attempts
     */
    public static File getRandomImageFile(File folder)
    throws FileNotFoundException
    {
	int attempts = 0;
	File file = null;
	FileFilter ff = new FileFilter() {
	    public boolean accept(File f) {
		return f.isDirectory() || isImage(f);
	    }
	};
	do {
	    file = folder;
	    while (file.isDirectory()) {
		File[] files = file.listFiles(ff);
		if (files.length == 0) {
		    file = folder;
		    continue;
		}
		int idx = (int)Math.floor(Math.random() * files.length);
		file = files[idx];
	    }
	    attempts++;
	} while (!isImage(file) && (attempts < 10));
	if (attempts >= 10) {
	    throw new FileNotFoundException("No image found after 10 attempts");
	}
	return file;
    }

    /**
     * Returns whether the given file is an image file. For now, just
     * check whether the file extension is JPG, GIF, or PNG.
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
	String ext = name.substring(idot+1);
	return
	    ext.equalsIgnoreCase("jpg") ||
	    ext.equalsIgnoreCase("gif") ||
	    ext.equalsIgnoreCase("png");
    }

    /**
     * The about message.
     */
    public static String aboutMsg() {
	return "<html>Sphaero2 Jigsaw Puzzle.<br>"
	    + "Original by Paul Brinkley, 2003.<br>"
	    + "Updated by Peter Tribble, 2010-2012.</html>";
    }

    /**
     * The help messsage.
     */
    public static String helpMsg() {
	return "<html>Drag pieces with the mouse to fit them together.  If"
	    + " they do, they'll join and move as a unit from then on."
	    + "<p> Keyboard commands: <br>"
	    + "<table>"
	    + "<tr><td>" + JigsawPuzzle.ROTATE_LEFT
	    + " <td> rotate piece left 90 degrees"
	    + "<tr><td>" + JigsawPuzzle.ROTATE_RIGHT
	    + " <td> rotate piece right 90 degrees"
	    + "<tr><td>" + JigsawPuzzle.SHUFFLE
	    + " <td> shuffle all pieces (good for finding pieces accidentally"
	    + " moved off the board)"
	    + "<tr><td>" + JigsawPuzzle.PUSH
	    + " <td> push the top piece to the back (handy if it's hiding"
	    + " other pieces)"
	    + "<tr><td>" + JigsawPuzzle.PREV_BG
	    + " <td> change background to previous color"
	    + "<tr><td>" + JigsawPuzzle.NEXT_BG
	    + " <td> change background to next color"
	    + "<tr><td>" + JigsawPuzzle.CLEAR
	    + " <td> toggle clear mode; mouse now drags over spaces to be"
	    + " cleared of pieces; cleared pieces are placed randomly"
	    + " elsewhere"
	    + "</table>";
    }
}
