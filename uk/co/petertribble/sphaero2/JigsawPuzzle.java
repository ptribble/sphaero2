package uk.co.petertribble.sphaero2;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JPanel;
import javax.swing.Timer;

// ### I think I need a quicker way to detect which piece is clicked on.
//   Mouse-down lags when there are lots of pieces.

/**
 * Jigsaw puzzle.
 */
public final class JigsawPuzzle extends JPanel {
    // Class constants ------------------------------------------------------
    private static final long serialVersionUID = 1L;

    // ### Allow background to be changed.
    // ### Allow these to be configured.
    // Keyboard commands
    public static final char ROTATE_LEFT = 'E';
    public static final char ROTATE_RIGHT = 'R';
    public static final char SHUFFLE = 'S';
    // change to Next and Previous ?
    public static final char PREV_BG = 'V';
    public static final char NEXT_BG = 'B';
    public static final char PUSH = 'P';
    public static final char CLEAR = 'C';
    // hide for pause
    public static final char HIDE = 'H';

    // Available background colors
    private static final Color[] BGCOLORS = {
	Color.BLACK,
	new Color(48, 0, 0),
	new Color(0, 48, 0),
	new Color(0, 0, 48),
	new Color(48, 48, 48),
	new Color(96, 0, 0),
	new Color(0, 96, 0),
	new Color(0, 0, 96),
	new Color(96, 96, 96),
	new Color(144, 0, 0),
	new Color(0, 144, 0),
	new Color(0, 0, 144),
	new Color(144, 144, 144),
    };

    private static final Color CLEAR_COLOR_W = new Color(255, 255, 255, 48);
    private static final Color CLEAR_COLOR_B = new Color(0, 0, 0, 48);

    private static final Cursor
	NORMAL_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor
	CLEAR_CURSOR  = new Cursor(Cursor.CROSSHAIR_CURSOR);

    private static final Rectangle EMPTYRECT = new Rectangle(0, 0, 0, 0);

    private final BufferedImage image;
    Image finishedImage;
    private final JigsawCutter cutter;
    private Dimension prefSize;
    private boolean mouseDown;
    private boolean finished;
    private boolean clearMode;
    private boolean paused;
    // Translation from a piece's upper-left corner to the point you clicked
    // on.
    private int transX;
    private int transY;
    // Last in list = topmost piece
    private List<Piece> zOrder;
    private int bgColor = 4;
    private int clearX0;
    private int clearY0;
    private int clearX1;
    private int clearY1;
    private Color clearColor;

    // If a keyboard command can affect a piece, it'll be this one.
    // Typically, this piece should be last in zOrder, but you never know.
    private Piece focusPiece;

    // this is the timer label
    private TimeLabel tlabel;

    /**
     * Creates a new JigsawPuzzle.
     * @param image the final picture
     * @param cutter the JigsawCutter to use on the image
     */
    public JigsawPuzzle(BufferedImage image, JigsawCutter cutter) {
	super(false);
	this.image = image;
	this.cutter = cutter;

	computePreferredSize();
	setOpaque(true);
	setFocusable(true);
	setBackground(BGCOLORS[bgColor]);
	setCursor(NORMAL_CURSOR);
	setClearColor();
	addWiring();
    }

    private void addWiring() {
	addMouseListener(new MouseAdapter() {
	    @Override
	    public void mousePressed(MouseEvent e) {
		mousePressed0(e);
	    }
	    @Override
	    public void mouseReleased(MouseEvent e) {
		mouseReleased0(e);
	    }
	});
	addMouseMotionListener(new MouseMotionAdapter() {
	    @Override
	    public void mouseDragged(MouseEvent e) {
		mouseDragged0(e);
	    }
	});
	addKeyListener(new KeyAdapter() {
	    @Override
	    public void keyTyped(KeyEvent e) {
		keyTyped0(e);
	    }
	    @Override
	    public void keyPressed(KeyEvent e) {
		keyPressed0(e);
	    }
	});
    }

    // Accessors ------------------------------------------------------------

    /**
     * Tells this puzzle to cut the image into pieces and randomize their
     * positions.  This is a potentially time-consuming operation, and should
     * not be done in the AWT thread.
     */
    public void reset() {
	zOrder = null;
	Piece[] pieces = cutter.cut(image);
	shuffle(pieces);
    }

    /**
     * Move current pieces around randomly, randomize z-order, but don't
     * randomize rotation.
     */
    public void shuffle() {
	Piece[] pieces = new Piece[zOrder.size()];
	zOrder.toArray(pieces);
	shuffle(pieces);
	repaint();
    }

    /**
     * Push the top piece (at the front) to the bottom (the back).
     */
    public void push() {
	Piece p = zOrder.remove(zOrder.size() - 1);
	zOrder.add(0, p);
	repaint();
    }

    // Component methods ----------------------------------------------------

    @Override
    public Dimension getMaximumSize() {
	return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
	return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
	return prefSize;
    }

    /**
     * Returns whether the pieces have been prepared for this puzzle.
     *
     * @return true if the Pieces have been cut for this puzzle
     */
    public boolean isCut() {
	return zOrder != null;
    }

    @Override
    protected void paintComponent(Graphics g) {
	super.paintComponent(g);

	if (zOrder == null) {
	    return;
	}

	if (paused) {
	    return;
	}

	for (Piece piece : zOrder) {
	    piece.draw(g);
	}

	if (clearMode && mouseDown) {
	    int cx = Math.min(clearX0, clearX1);
	    int cy = Math.min(clearY0, clearY1);
	    int cw = Math.abs(clearX0 - clearX1);
	    int ch = Math.abs(clearY0 - clearY1);
	    g.setColor(clearColor);
	    g.fillRect(cx, cy, cw, ch);
	}

	if (finished && finishedImage != null) {
	    Piece lastPiece = zOrder.get(0);
	    int x = lastPiece.getPuzzleX();
	    int y = lastPiece.getPuzzleY();
	    g.drawImage(finishedImage, x, y, null);
	}
    }

    private void setClearMode(boolean flag) {
	clearMode = flag;
	setCursor(clearMode ? CLEAR_CURSOR : NORMAL_CURSOR);
    }

    private void toggleHidden() {
	paused = !paused;
	repaint();
	if (tlabel != null) {
	    if (paused) {
		tlabel.pause();
	    } else {
		tlabel.unpause();
	    }
	}
    }

    /**
     * Connect a timer label to this puzzle. If there's a valid label
     * it will be updated with the running solution time.
     *
     * @param tlabel a TimeLabel to be updated
     */
    public void setTimeLabel(TimeLabel tlabel) {
	this.tlabel = tlabel;
    }

    // Private methods ------------------------------------------------------

    /**
     * Ideally, the preferred area is roughly 1.5 times the area of the
     * image, and the preferred width is 5/3 of the preferred height.
     * However, if the result would be smaller than the image in either
     * dimension, it is enlarged to allow the image to fit.
     */
    private void computePreferredSize() {
	JigUtil.ensureLoaded(image);
	int iWidth  = image.getWidth(null);
	int iHeight = image.getHeight(null);
	int area = iWidth * iHeight * 3 / 2;
	int width = (int) Math.sqrt(area * 5 / 3);
	int height = width * 3 / 5;
	width  = Math.max(width, iWidth);
	height = Math.max(height, iHeight);
	prefSize = new Dimension(width, height);
    }

    // ### Should this be public?
    private void finish() {
	if (zOrder.size() != 1) {
	    return;
	}
	finished = true;
	Piece lastPiece = zOrder.get(0);

	// Auto-rotate the puzzle to its correct position.
	lastPiece.setRotation(0);

	// stop the time label to show the solution time
	if (tlabel != null) {
	    tlabel.finished();
	}

	// Center the last piece in the middle of the panel.
	int prevX = lastPiece.getPuzzleX();
	int prevY = lastPiece.getPuzzleY();
	final int width = lastPiece.getImageWidth();
	final int height = lastPiece.getImageHeight();
	int curW = getWidth();
	int curH = getHeight();
	final int centerX = (curW - width) / 2;
	final int centerY = (curH - height) / 2;
	lastPiece.moveTo(centerX, centerY);
	repaint(0, prevX, prevY, width, height);
	repaint(0, centerX, centerY, width, height);

	// Draw the original image on top of the last piece in increasing
	// opaqueness.  This should make the pieces appear to fade into the
	// original image.
	final int[] data = new int[width * height];
	try {
	    new PixelGrabber(image, 0, 0, width, height, data, 0, width)
		.grabPixels();
	} catch (InterruptedException ex) { }
	for (int i = 0; i < data.length; i++) {
	    data[i] = data[i] & 0x00ffffff;
	}

	ActionListener fader = new ActionListener() {
	    int trans;
	    @Override
	    public void actionPerformed(ActionEvent evt) {
		for (int i = 0; i < data.length; i++) {
		    data[i] = (data[i] & 0x00ffffff) | (trans << 24);
		}
		if (finishedImage != null) {
		    finishedImage.flush();
		}
		finishedImage = Toolkit.getDefaultToolkit().createImage(
			new MemoryImageSource(width, height, data, 0, width));
		repaint(0, centerX, centerY, width, height);
		if (trans < 0xff) {
		    trans += 0x11;
		    if (trans >= 0xff) {
			trans = 0xff;
		    }
		    Timer timer = new Timer(200, this);
		    timer.setRepeats(false);
		    timer.start();
		}
	    }
	};

	Timer timer = new Timer(200, fader);
	timer.setRepeats(false);
	timer.start();
    }


    // Mouse event handling -------------------------------------------------

    void mousePressed0(MouseEvent e) {
	if (finished) {
	    return;
	}
	mouseDown = true;
	if (clearMode) {
	    startClearRect(e);
	} else {
	    grabPiece(e);
	}
    }

    void mouseDragged0(MouseEvent e) {
	if (finished) {
	    return;
	}
	if (clearMode) {
	    dragClearRect(e);
	} else {
	    dragPiece(e);
	}
    }

    void mouseReleased0(MouseEvent e) {
	if (finished) {
	    return;
	}
	mouseDown = false;
	if (clearMode) {
	    finishClearRect(e);
	} else {
	    releasePiece();
	}
    }

    private void grabPiece(MouseEvent e) {
	int x = e.getX();
	int y = e.getY();

	focusPiece = null;
	ListIterator<Piece> iter = zOrder.listIterator(zOrder.size());
	while (focusPiece == null && iter.hasPrevious()) {
	    Piece piece = iter.previous();
	    if (piece.contains(x, y)) {
		focusPiece = piece;
		iter.remove();
	    }
	}
	if (focusPiece != null) {
	    zOrder.add(focusPiece);
	    transX = x - focusPiece.getPuzzleX();
	    transY = y - focusPiece.getPuzzleY();
	    // The focusPiece might have moved up in Z-order. At worst, we have
	    // to repaint its bounding rectangle.
	    repaint(0, focusPiece.getPuzzleX(), focusPiece.getPuzzleY(),
		focusPiece.getCurrentWidth(), focusPiece.getCurrentHeight());
	}
    }

    private void dragPiece(MouseEvent e) {
	if (focusPiece == null) {
	    return;
	}
	int prevX = focusPiece.getPuzzleX();
	int prevY = focusPiece.getPuzzleY();
	int prevW = focusPiece.getCurrentWidth();
	int prevH = focusPiece.getCurrentHeight();
	focusPiece.moveTo(e.getX() - transX, e.getY() - transY);
	// Repaint the focusPiece' previous and current bounding rects.
	repaint(0, prevX, prevY, prevW, prevH);
	repaint(0, focusPiece.getPuzzleX(), focusPiece.getPuzzleY(),
		focusPiece.getCurrentWidth(), focusPiece.getCurrentHeight());
    }

    private void releasePiece() {
	if (focusPiece == null) {
	    return;
	}
	Piece[] result = focusPiece.join();
	if (result != null) {
	    Piece newPiece = result[0];
	    for (int i = 1; i < result.length; i++) {
		zOrder.remove(result[i]);
	    }
	    zOrder.add(newPiece);
	    focusPiece = newPiece;
	    // Joined pieces may be of any size and number. Mouse release isn't
	    // a terribly frequent event, so just repaint the whole thing.  If
	    // it's really necessary later, the thing to do would be to repaint
	    // the bounding rect for every piece in the result array above.
	    repaint();
	    if (zOrder.size() == 1) {
		finish();
	    }
	}
    }

    private void startClearRect(MouseEvent e) {
	clearX0 = e.getX();
	clearY0 = e.getY();
    }

    private void dragClearRect(MouseEvent e) {
	int prevX1 = clearX1;
	int prevY1 = clearY1;
	clearX1 = e.getX();
	clearY1 = e.getY();
	int x = Math.min(clearX0, prevX1);
	int y = Math.min(clearY0, prevY1);
	int w = Math.abs(clearX0 - prevX1);
	int h = Math.abs(clearY0 - prevY1);
	repaint(0, x, y, w, h);
	x = Math.min(clearX0, clearX1);
	y = Math.min(clearY0, clearY1);
	w = Math.abs(clearX0 - clearX1);
	h = Math.abs(clearY0 - clearY1);
	repaint(0, x, y, w, h);
    }

    private void finishClearRect(MouseEvent e) {
	clearX1 = e.getX();
	clearY1 = e.getY();
	int cx0 = Math.max(0, Math.min(clearX0, clearX1));
	int cy0 = Math.max(0, Math.min(clearY0, clearY1));
	int cx1 = Math.min(getWidth(), Math.max(clearX0, clearX1));
	int cy1 = Math.min(getHeight(), Math.max(clearY0, clearY1));
	for (Piece piece : zOrder) {
	    if (intersects(piece, cx0, cy0, cx1, cy1)) {
		shuffle(piece, cx0, cy0, cx1, cy1);
	    }
	}
	repaint();
    }

    // (x1,y1) guaranteed to be SE of (x0,y0)
    /**
     * Return whether the piece intersects with the rectangle defined by the
     * given points.  ### Not perfect; returns true for some pieces that it
     * shouldn't.  Ideally, it should grab the part of the Piece in the
     * rectangle, and search it for non-transparent pixels.  Costly, so be
     * careful.
     *
     * @param piece the Piece to be checked
     * @param x0 location of the left of the rectangle
     * @param y0 location of the bottom of the rectangle
     * @param x1 location of the right of the rectangle
     * @param y1 location of the top of the rectangle
     *
     * @return true if the Piece intersects with the given rectangle
     */
    private boolean intersects(Piece piece, int x0, int y0, int x1, int y1) {
	int px = piece.getPuzzleX();
	int py = piece.getPuzzleY();
	int pw = piece.getCurrentWidth();
	int ph = piece.getCurrentHeight();
	int w = x1 - x0;
	int h = y1 - y0;
	Rectangle r  = new Rectangle(x0, y0, w, h);
	Rectangle rp = new Rectangle(px, py, pw, ph);
	return r.intersects(rp);
    }

    // (x1,y1) guaranteed to be SE of (x0,y0)
    /**
     * Shuffle piece randomly, but keeping it out of the rectangle defined
     * by the given points.
     *
     * @param piece the Piece to be shuufled
     * @param x0 location of the left of the rectangle
     * @param y0 location of the bottom of the rectangle
     * @param x1 location of the right of the rectangle
     * @param y1 location of the top of the rectangle
     */
    private void shuffle(Piece piece, int x0, int y0, int x1, int y1) {
	// Make the rectangle denoting where the Piece could be placed in the
	// whole panel.  Top point will be (0,0).
	int w = getWidth() - piece.getCurrentWidth();
	int h = getHeight() - piece.getCurrentHeight();
	// If w or h is negative, the piece is too big to be shuffled, so quit.
	if (w < 0 || h < 0) {
	    return;
	}

	// Define the endpoints of the rectangle the Piece must avoid.
	int ax = Math.max(0, x0 - piece.getCurrentWidth());
	int ay = Math.max(0, y0 - piece.getCurrentHeight());
	// int aw = x1 - ax;
	int ah = y1 - ay;

	// Now define four rectangles forming the shape where the NW piece
	// corner could go.  I'll use BorderLayout rectangles as a guide.

	// FIXME we could calculate the areas directly and only create
	// the one rectangle we need for the shuffle call, or even not
	// create any rectangles as we only need the height and width
	Rectangle north = (ay == 0) ? EMPTYRECT
	    : new Rectangle(0, 0, w, ay);
	Rectangle south = (y1 >= h) ? EMPTYRECT
	    : new Rectangle(0, y1 + 1, w, h - y1);
	Rectangle west  = (ax == 0 || ah == 0) ? EMPTYRECT
	    : new Rectangle(0, ay, ax, ah);
	Rectangle east  = (x1 >= w || ah == 0) ? EMPTYRECT
	    : new Rectangle(x1, ay, w - x1, ah);

	int nArea = north.width * north.height;
	int sArea = south.width * south.height;
	int wArea = west.width * west.height;
	int eArea = east.width * east.height;
	int totalArea = nArea + sArea + wArea + eArea;

	int rand = ThreadLocalRandom.current().nextInt(totalArea);

	rand -= nArea;
	if (rand < 0) {
	    shuffle(piece, north);
	    return;
	}
	rand -= sArea;
	if (rand < 0) {
	    shuffle(piece, south);
	    return;
	}
	rand -= wArea;
	if (rand < 0) {
	    shuffle(piece, west);
	    return;
	}
	shuffle(piece, east);
    }

    private void shuffle(Piece piece, Rectangle rect) {
	int dx = ThreadLocalRandom.current().nextInt(rect.width);
	int dy = ThreadLocalRandom.current().nextInt(rect.height);
	piece.moveTo(rect.x + dx, rect.y + dy);
    }

    // Keyboard event handling ----------------------------------------------

    void keyTyped0(KeyEvent e) {
	char ch = Character.toUpperCase(e.getKeyChar());
	if (ch == PREV_BG) {
	    prevBackground();
	} else if (ch == NEXT_BG) {
	    nextBackground();
	}
	if (finished) {
	    return;
	}
	if (ch == ROTATE_LEFT) {
	    rotatePiece(270);
	} else if (ch == ROTATE_RIGHT) {
	    rotatePiece(90);
	} else if (ch == SHUFFLE) {
	    shuffle();
	} else if (ch == PUSH) {
	    push();
	} else if (ch == CLEAR) {
	    toggleClearMode();
	} else if (ch == HIDE) {
	    toggleHidden();
	}
    }

    void keyPressed0(KeyEvent e) {
	if (!finished) {
	    if (e.getKeyCode() == KeyEvent.VK_LEFT
		    || e.getKeyCode() == KeyEvent.VK_KP_LEFT) {
		rotatePiece(270);
	    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT
		    || e.getKeyCode() == KeyEvent.VK_KP_RIGHT) {
		rotatePiece(90);
	    } else if (e.getKeyCode() == KeyEvent.VK_DOWN
		    || e.getKeyCode() == KeyEvent.VK_KP_DOWN) {
		push();
	    }
	}
    }

    private void rotatePiece(int amount) {
	if (focusPiece == null) {
	    return;
	}
	int newRotation = focusPiece.getRotation() + amount;
	newRotation %= 360;
	int prevW = focusPiece.getCurrentWidth();
	int prevH = focusPiece.getCurrentHeight();
	int prevX = focusPiece.getPuzzleX();
	int prevY = focusPiece.getPuzzleY();
	focusPiece.setRotation(newRotation);
	// Make the piece appear to rotate about its center.
	// ### Feature: When the mouse is down, rotate about the cursor instead
	//   of the center.
	int currW = focusPiece.getCurrentWidth();
	int currH = focusPiece.getCurrentHeight();
	int currX = prevX + (prevW - currW) / 2;
	int currY = prevY + (prevH - currH) / 2;
	focusPiece.moveTo(currX, currY);
	repaint(0, prevX, prevY, prevW, prevH);
	repaint(0, currX, currY, currW, currH);
    }

    // Copy pieces into zOrder, and randomize their positions.
    private void shuffle(Piece[] pieces) {
	// Arrays.asList() doesn't work, so be explicit
	zOrder = new ArrayList<>();
	int height = getHeight();
	int width = getWidth();

	ThreadLocalRandom trandom = ThreadLocalRandom.current();
	for (Piece piece : pieces) {
	    piece.setPuzzlePosition(
		trandom.nextInt((width  - piece.getCurrentWidth())),
		trandom.nextInt((height - piece.getCurrentHeight())));
	    zOrder.add(piece);
	}
	Collections.shuffle(zOrder);

	finished = false;
	if (finishedImage != null) {
	    finishedImage.flush();
	    finishedImage = null;
	}
    }

    private void prevBackground() {
	bgColor--;
	if (bgColor < 0) {
	    bgColor = BGCOLORS.length - 1;
	}
	setBackground(BGCOLORS[bgColor]);
	setClearColor();
	repaint();
    }

    private void nextBackground() {
	bgColor++;
	if (bgColor >= BGCOLORS.length) {
	    bgColor = 0;
	}
	setBackground(BGCOLORS[bgColor]);
	setClearColor();
	repaint();
    }

    private void toggleClearMode() {
	// can't toggle clear mode while dragging
	if (!mouseDown) {
	    setClearMode(!clearMode);
	}
    }

    private void setClearColor() {
	clearColor = isBright(BGCOLORS[bgColor]) ? CLEAR_COLOR_B
	    : CLEAR_COLOR_W;
    }

    private boolean isBright(Color c) {
	float[] hsb =
	    Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
	return hsb[2] > 0.5;
    }
}
