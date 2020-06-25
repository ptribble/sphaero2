package uk.co.petertribble.sphaero2;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * JFrame that runs a JigsawPuzzle. This is the front end for
 * JigsawPuzzle, the main class of the jigsaw application.
 *
 * <p> "Sphaero" is short for "Sphaerodactylinea". This is the name of one
 * of the subfamilies of geckos, including house geckos, tokay geckos,
 * striped leaf geckos, and several other common varieties. It reminded me
 * of Escher's depiction of a 2D surface tiled by lizards, which vaguely
 * resemble jigsaw puzzle pieces. Hence the name. ("Gecko" was already
 * taken.)
 *
 * <h4> Known Bugs </h4>
 *
 * <p> You can rotate a piece or pieces while dragging them; that's a
 * feature.  However, it doesn't rotate around the mouse cursor in that
 * case. It uses the center of mass of the piece (I think); the upshot is
 * that a piece may appear to jump out from under the cursor, yet still
 * respond to dragging.
 *
 * <p> The program may report one or more NullPointerExceptions when
 * loading the image. This seems to be in the native image-loading code,
 * possibly due to some image data being accessed before it has loaded.
 * I've never seen any missing pieces or image data as a result of this,
 * however.
 *
 * <p> The most serious bug is an OutOfMemoryError while the puzzle is
 * being solved. This occurs particularly on large images and large
 * numbers of pieces (200+), and even then only if picture is solved by
 * forming one large set of fitted pieces, and adding pieces singly to
 * that. If it's solved instead by forming medium-sized sections first,
 * and then fitting those sections together at the end, no problems arise.
 *
 * <p> This program uses a fair bit of memory. I use a max heap size of
 * 256Mb for large (1024x768 pixels) images, 200 pieces, and occasionally
 * will still get an OutOfMemoryError as above.
 */
public class JigsawFrame extends JFrame implements ActionListener {

    private JMenuBar jmb;
    private JMenu jmh;
    private JMenuItem newItem;
    private JMenuItem exitItem;
    private JMenuItem helpItem;
    private JMenuItem aboutItem;
    private JMenuItem pictureItem;
    private Image image;
    private Icon miniImage;

    // for the interactive prompt
    private static final Color HELP_COLOR = new Color(100, 100, 150);
    private JTextField imageField;
    private JButton browseButton;
    private JComboBox <JigsawCutter> cutterCBox;
    private JSpinner pieceSpinner;
    private JLabel cutterDescLabel;
    private JButton okButton;

    private int pHeight = 480;
    private int pWidth = 640;

    private static final JigsawCutter[] cutters = {
	new Classic4Cutter(),
	new ClassicCutter(),
	new SquareCutter(),
	new RectCutter(),
	new QuadCutter(),
    };

    /**
     * Creates and displays a simple JFrame containing a jigsaw puzzle in a
     * JScrollPane. The frame may be resized freely. If an image is supplied
     * on the command line, it will be used; otherwise the user will be
     * prompted.
     *
     * <h4>Command line arguments</h4>
     *
     * <pre>
     * -p &lt;<i>number</i>&gt; Cut the picture into roughly this number of
     * pieces.
     * &lt;<i>filename</i>&gt; If this denotes an image file, it will be
     * the target picture.  If it denotes a folder, it will be searched
     * for a random file, which is subject to the rules above.
     * Potentially any image file in any subfolder could be used. If an
     * image file cannot be found this way after ten tries, the program
     * halts.
     * </pre>
     *
     * <p>100 pieces are created by default. If no filename is given, the
     * current folder is used.
     *
     * <h4>Puzzle commands</h4>
     *
     * <p> Pieces can be dragged around with the mouse. The piece (or group
     * of pieces) most recently dragged or clicked on is the active piece.
     * Press R to rotate the active piece (or group) 90 degrees clockwise.
     * Press E to rotate it 90 degrees counter-clockwise. (Case doesn't
     * matter.) Press S to shuffle all the pieces around the panel randomly,
     * keeping fitted pieces together. Pieces are fitted automatically if
     * they are placed close enough, and are rotated the same way.
     */
    public JigsawFrame(Image image, JigsawCutter cutter) {
	super("Jigsaw Puzzle");
	initFrameWork();
	init(image, cutter);
    }

    public JigsawFrame() {
	super("Jigsaw Puzzle");
	initFrameWork();
	initPrompt();
    }

    private void initFrameWork() {
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	JMenu jmf = new JMenu("File");
	jmf.setMnemonic(KeyEvent.VK_F);

	newItem = new JMenuItem("New Image", KeyEvent.VK_N);
	newItem.addActionListener(this);
	jmf.add(newItem);

	jmf.addSeparator();

	exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
	exitItem.addActionListener(this);
	jmf.add(exitItem);

	jmb = new JMenuBar();
	jmb.add(jmf);
	setJMenuBar(jmb);

	setIconImage(new ImageIcon(this.getClass().getClassLoader()
			.getResource("pixmaps/sphaero2.png")).getImage());

	/*
	 * Create the help menu for the puzzle here, even though it's only
	 * visible in puzzle mode.
	 */
	jmh = new JMenu("Help");
	jmh.setMnemonic(KeyEvent.VK_H);
	helpItem = new JMenuItem("Instructions", KeyEvent.VK_I);
	helpItem.addActionListener(this);
	jmh.add(helpItem);
	aboutItem = new JMenuItem("About", KeyEvent.VK_A);
	aboutItem.addActionListener(this);
	jmh.add(aboutItem);
	pictureItem = new JMenuItem("Show Picture", KeyEvent.VK_P);
	pictureItem.addActionListener(this);
	jmh.add(pictureItem);
    }

    private void init(Image image, JigsawCutter cutter) {
	this.image = image;

	JigsawPuzzle puzzle = new JigsawPuzzle(image, cutter);
	JPanel ppanel = new JPanel(new BorderLayout());
	ppanel.add(new JScrollPane(puzzle));
	TimeLabel tlabel = new TimeLabel();
	ppanel.add(tlabel, BorderLayout.SOUTH);
	setContentPane(ppanel);
	pack();

	setSize(1024, 740);
	setVisible(true);

	// This doesn't quite work; I would prefer a modal dialog, but that
	// completely blocks the app

	JProgressBar jp = new JProgressBar();
	jp.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
	JDialog dialog = new JDialog(this, "Processing image.");
	dialog.setContentPane(jp);
	cutter.setJProgressBar(jp);
	dialog.pack();
	dialog.setLocationRelativeTo(this);
	dialog.setVisible(true);
	puzzle.reset();
	dialog.setVisible(false);
	jmb.add(jmh);
	repaint();
	tlabel.start();
	puzzle.setTimeLabel(tlabel);
    }

    private void initPrompt() {
	JPanel mainPane = new JPanel();
	mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));

	imageField = new JTextField();
	imageField.setText(getCurrentPath());
	imageField.selectAll();

	browseButton = new JButton("Browse...");
	browseButton.setMnemonic(KeyEvent.VK_B);
	browseButton.addActionListener(this);

	JLabel imageLabel = createHelpLabel("<html>"
		+"If this is an image file, it is used to create the puzzle. "
		+"If it is a folder, an image file is selected from it "
		+"(including any subfolders) at random.");

	JPanel imageTopPane = new JPanel(new BorderLayout());
	imageTopPane.add(imageField, BorderLayout.CENTER);
	imageTopPane.add(browseButton, BorderLayout.EAST);

	JPanel imagePane = new JPanel(new BorderLayout());
	imagePane.setBorder(createTitledBorder("Find an image"));
	imagePane.add(imageTopPane, BorderLayout.NORTH);
	imagePane.add(imageLabel, BorderLayout.CENTER);

	cutterCBox = new JComboBox <JigsawCutter> (cutters);
	cutterCBox.addActionListener(this);

	cutterDescLabel = createHelpLabel(null);
	JPanel cutterPane = new JPanel(new BorderLayout());
	cutterPane.add(cutterCBox, BorderLayout.NORTH);
	cutterPane.add(cutterDescLabel, BorderLayout.CENTER);
	cutterPane.setBorder(createTitledBorder("Cutting Style"));
	fireCutterChanged();

	pieceSpinner = new JSpinner(new SpinnerNumberModel(
			 JigsawCutter.DEFAULT_PIECES, JigsawCutter.MIN_PIECES,
			 JigsawCutter.MAX_PIECES, 1));
	JLabel pieceLabel = createHelpLabel("<html>"
		+" The puzzle will have roughly this many pieces.");
	JPanel piecePane = new JPanel(new BorderLayout());
	piecePane.add(pieceSpinner, BorderLayout.NORTH);
	piecePane.add(pieceLabel, BorderLayout.CENTER);
	piecePane.setBorder(createTitledBorder("Piece Count"));

	JPanel okPanel = new JPanel(new GridLayout(1, 2, 10, 10));
	okButton  = new JButton("OK");
	okButton.setMnemonic(KeyEvent.VK_K);
	okPanel.add(okButton);
	okButton.addActionListener(this);

	SamplePanel sPanel = new SamplePanel(imageField);
	if (sPanel.samplesValid()) {
	    JPanel samplePane = new JPanel(new BorderLayout());
	    samplePane.setBorder(createTitledBorder("Select an image"));
	    samplePane.add(new JScrollPane(sPanel));
	    mainPane.add(samplePane);
	    pHeight = 640;
	}

	mainPane.add(imagePane);
	mainPane.add(piecePane);
	mainPane.add(cutterPane);
	mainPane.add(okPanel);

	setContentPane(mainPane);
	setSize(pWidth, pHeight);
	setVisible(true);
    }

    public static void main(String[] args) {
	if (args.length == 0) {
	    new JigsawFrame();
	} else {
	    final File defBase = new File(".");
	    File base = defBase;
	    String argcutter = "classic";
	    int prefPieces = JigsawCutter.DEFAULT_PIECES;
	    int arg = 0;
	    while (arg < args.length) {
		if (args[arg].equals("-p")) {
		    arg++;
		    if (arg < args.length) {
			try {
			    prefPieces = Integer.parseInt(args[arg]);
			} catch (NumberFormatException ex) {
			    System.err.println("Invalid number of pieces!");
			    System.exit(1);
			}
			if (prefPieces < JigsawCutter.MIN_PIECES) {
			    System.err.println("Too few pieces!");
			    System.exit(1);
			}
			if (prefPieces > JigsawCutter.MAX_PIECES) {
			    System.err.println("Too many pieces!");
			    System.exit(1);
			}
		    } else {
			System.err.println("Expecting an argument to -p!");
			System.exit(1);
		    }
		} else if (args[arg].equals("-c")) {
		    arg++;
		    if (arg < args.length) {
			argcutter = args[arg];
		    } else {
			System.err.println("Expecting an argument to -c!");
			System.exit(1);
		    }
		} else {
		    File argFile = new File(args[arg]);
		    if (argFile.exists()) {
			base = argFile;
		    } else {
			System.err.println("Invalid file!");
			System.exit(1);
		    }
		}
		arg++;
	    }
	    if (base.isFile() && !JigUtil.isImage(base)) {
		base = defBase;
	    }

	    File file = null;
	    try {
		file = JigUtil.getRandomImageFile(base);
	    } catch (FileNotFoundException ex) {
		System.err.println("Couldn't find an image file!");
		System.err.println("Name one on the command line, or run");
		System.err.println("this program in a folder containing them.");
		System.exit(1);
	    }

	    BufferedImage image = null;
	    try {
		image = JigUtil.resizedImage(ImageIO.read(file));
	    } catch (IOException e) {
		System.err.println("Error reading image file!");
		System.exit(1);
	    }

	    /*
	     * Try and use the cutter specified on the command line, or if
	     * not specified use a Classic Cutter. Error out if we're given
	     * an invalid cutter, but give the user the full list.
	     */
	    boolean cmatch = false;
	    for (JigsawCutter cutter : cutters) {
		if (argcutter.equalsIgnoreCase(cutter.getName())) {
		    cmatch = true;
		    cutter.setPreferredPieceCount(prefPieces);
		    new JigsawFrame(image, cutter);
		}
	    }
	    if (!cmatch) {
		System.err.println("Invalid cutter!");
		System.err.println("Valid cutters are:");
		for (JigsawCutter cutter : cutters) {
		    System.err.println(cutter.getName());
		}
		System.exit(1);
	    }
	}
    }

    private JLabel createHelpLabel(String text) {
	JLabel label = new JLabel(text);
	label.setBorder(BorderFactory.createEmptyBorder(5, 1, 1, 1));
	label.setForeground(HELP_COLOR);
	return label;
    }

    private Border createTitledBorder(String title) {
	Border outer = BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), title);
	Border inner = BorderFactory.createEmptyBorder(2, 5, 5, 5);
	return BorderFactory.createCompoundBorder(outer, inner);
    }

    private void showPrompt() {
	getContentPane().removeAll();
	jmb.remove(jmh);
	jmb.revalidate();
	miniImage = null;
	System.gc();
	initPrompt();
    }

    private void showPicture() {
	if (miniImage == null) {
	    miniImage = new ImageIcon(image.getScaledInstance(200, -1,
							Image.SCALE_FAST));
	}
	JOptionPane.showMessageDialog(this, miniImage,
			"Quick view", JOptionPane.PLAIN_MESSAGE);
    }

    private void fireBrowseAction() {
	JFileChooser chooser = new JFileChooser(getCurrentFolder());
	chooser.setFileFilter(new JigFileFilter());
	chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
	chooser.setAccessory(new ImagePreview(chooser));
	if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
	    imageField.setText(chooser.getSelectedFile().getAbsolutePath());
	}
    }

    private void fireCutterChanged() {
	JigsawCutter cutter = (JigsawCutter) cutterCBox.getSelectedItem();
	cutterDescLabel.setText("<html>"+cutter.getDescription());
    }

    private void fireOKAction() {
	setUpPuzzle();
    }

    private void setUpPuzzle() {
	// Get the image.
	File file = new File(imageField.getText());

	if (!file.exists()) {
	    JOptionPane.showMessageDialog(this, "File does not exist.",
			"Nonexistent file", JOptionPane.ERROR_MESSAGE);
	    return;
	}
	if (file.isDirectory()) {
	    try {
		file = JigUtil.getRandomImageFile(file);
	    } catch (FileNotFoundException ex) {
		JOptionPane.showMessageDialog(this,
				"This folder contains no images.",
				"Empty folder", JOptionPane.ERROR_MESSAGE);
		return;
	    }
	} else if (!JigUtil.isImage(file)) {
	    JOptionPane.showMessageDialog(this, "This is not an image file.",
				"Invalid Image", JOptionPane.ERROR_MESSAGE);
	    return;
	}

	// Get the cutter and set its piece count
	JigsawCutter cutter = (JigsawCutter) cutterCBox.getSelectedItem();
	int count = ((Number) pieceSpinner.getValue()).intValue();
	cutter.setPreferredPieceCount(count);

	try {
	    BufferedImage image = ImageIO.read(file);
	    // FIXME this doesn't actually show the window properly until
	    // after the pieces have been cut???
	    // So the progress bar doesn't work either
	    init(JigUtil.resizedImage(image), cutter);
	} catch (IOException e) {
	    JOptionPane.showMessageDialog(this, "Image file cannot be read.",
				"Invalid Image", JOptionPane.ERROR_MESSAGE);
	}
    }

    /*
     * Lame way of getting the current path in a way that guarantees an
     * answer returned, and no exception thrown.  Could still throw a
     * SecurityException, but I don't care.
     */
    private String getCurrentPath() {
	File folder = getCurrentFolder();
	try {
	    return folder.getCanonicalPath();
	} catch (IOException ex) {
	    return folder.getAbsolutePath();
	}
    }

    /*
     * Returns the folder corresponding to whatever's currently displayed in
     * imageField.
     */
    private File getCurrentFolder() {
	String text = imageField.getText().trim();
	return new File((text.length() == 0) ? "." : text);
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == exitItem) {
	    System.exit(0);
	} else if (e.getSource() == newItem) {
	    showPrompt();
	} else if (e.getSource() == helpItem) {
	    JOptionPane.showMessageDialog(this, JigUtil.helpMsg(),
			"Sphaero2 help", JOptionPane.PLAIN_MESSAGE);
	} else if (e.getSource() == aboutItem) {
	    JOptionPane.showMessageDialog(this, JigUtil.aboutMsg(),
			"About Sphaero2", JOptionPane.PLAIN_MESSAGE);
	} else if (e.getSource() == pictureItem) {
	    showPicture();
	} else if (e.getSource() == browseButton) {
	    fireBrowseAction();
	} else if (e.getSource() == cutterCBox) {
	    fireCutterChanged();
	} else if (e.getSource() == okButton) {
	    fireOKAction();
	}
    }
}
