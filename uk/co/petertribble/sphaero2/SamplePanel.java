package uk.co.petertribble.sphaero2;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

public class SamplePanel extends JPanel implements ActionListener {

    private JTextField jtf;
    private boolean hasSamples;
    private Map <JButton, String> fmap = new HashMap <JButton, String> ();
    private static final String SAMPLE_DIR = "/usr/share/sphaero2/samples";

    public SamplePanel(JTextField jtf) {
	this.jtf = jtf;
	initSamples();
    }

    private void initSamples() {
	hasSamples = false;
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
	 */
	for (String s : fd.list()) {
	    File f2 = new File(fd, "thumb." + s);
	    if (f2.exists()) {
		JButton jb = new JButton(new ImageIcon(f2.getPath()));
		File f1 = new File(fd, s);
		fmap.put(jb, f1.getPath());
		jb.addActionListener(this);
		hasSamples = true;
		add(jb);
	    }
	}
    }

    public boolean samplesValid() {
	return hasSamples;
    }

    /*
     * When a button is pressed, retrieve the name of the corresponding file
     * from the map and poke it back into the text field.
     */
    public void actionPerformed(ActionEvent e) {
	JButton jb = (JButton) e.getSource();
	jtf.setText(fmap.get(jb));
    }
}
