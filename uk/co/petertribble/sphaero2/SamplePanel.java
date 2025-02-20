package uk.co.petertribble.sphaero2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Insets;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A panel displaying some sample images, allowing the user to select
 * a built in image for the jigsaw.
 */
public final class SamplePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTextField jtf;
    private int nSamples;
    private Map<JButton, String> fmap = new HashMap<>();
    private static final String SAMPLE_DIR = "/usr/share/sphaero2/samples";

    /**
     * Create a new SamplePanel, to display some sample jigsaw images.
     *
     * @param jtf a JTextField, used by callers of this class to read the
     * filename of the selected image
     */
    public SamplePanel(JTextField jtf) {
	this.jtf = jtf;
	initSamples();
    }

    private void initSamples() {
	File fd = new File(SAMPLE_DIR);
	if (!fd.exists()) {
	    return;
	}
	if (!fd.isDirectory()) {
	    return;
	}
	/*
	 * The sample directories contain an image foo.jpg and a thumbnail
	 * thumb.foo.jpg. If the thumbnail exists then create a button using
	 * it and add the primary image filename to the map.
	 *
	 * Tighten up the border, a normal JButton has a lot of wasted space
	 * on the left and right sides.
	 */
	Insets margins = new Insets(2, 2, 2, 2);
	for (String s : fd.list()) {
	    File f2 = new File(fd, "thumb." + s);
	    if (f2.exists()) {
		JButton jb = new JButton(new ImageIcon(f2.getPath()));
		jb.setMargin(margins);
		File f1 = new File(fd, s);
		fmap.put(jb, f1.getPath());
		jb.addActionListener(this);
		nSamples++;
		add(jb);
	    }
	}
    }

    /**
     * Check if there are some valid sample images.
     *
     * @return true if there some sample images.
     */
    public boolean samplesValid() {
	return (nSamples > 0);
    }

    /*
     * When a button is pressed, retrieve the name of the corresponding file
     * from the map and poke it back into the text field.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
	JButton jb = (JButton) e.getSource();
	jtf.setText(fmap.get(jb));
    }
}
