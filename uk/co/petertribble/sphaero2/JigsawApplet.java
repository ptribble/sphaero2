package uk.co.petertribble.sphaero2;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * This is an applet implementation of sphaero.
 */
public class JigsawApplet extends JApplet implements ActionListener {

    private JMenuItem helpItem;
    private JMenuItem aboutItem;
    private JMenuItem pictureItem;
    private Image image;
    private Icon miniImage;

    public void init() {
	image = getImage(getCodeBase(), getParameter("image"));
	int npieces = 64;
	try {
	    npieces = Integer.parseInt(getParameter("pieces"));
	} catch (NumberFormatException nfe) {}
	JigsawCutter cutter = new Classic4Cutter(npieces);
	JigsawPuzzle puzzle = new JigsawPuzzle(image, cutter);
	JPanel ppanel = new JPanel(new BorderLayout());
	ppanel.add(new JScrollPane(puzzle));
	TimeLabel tlabel = new TimeLabel();
	ppanel.add(tlabel, BorderLayout.SOUTH);
	add(ppanel);
	JProgressBar jp = new JProgressBar();
	jp.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
	JDialog dialog = new JDialog();
	dialog.setContentPane(jp);
	cutter.setJProgressBar(jp);
	dialog.pack();
	dialog.setLocationRelativeTo(this);
	dialog.setVisible(true);
	puzzle.reset();
	dialog.setVisible(false);
	doMenu();
	repaint();
	tlabel.start();
	puzzle.setTimeLabel(tlabel);
    }

    private void doMenu() {
	JMenuBar jmb = new JMenuBar();
	JMenu jmh = new JMenu("Help");
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
	jmb.add(jmh);
	setJMenuBar(jmb);
    }

    private void showPicture() {
	if (miniImage == null) {
	    miniImage = new ImageIcon(image.getScaledInstance(200, -1,
							Image.SCALE_FAST));
	}
	JOptionPane.showMessageDialog(this, miniImage);
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == helpItem) {
	    JOptionPane.showMessageDialog(this, JigUtil.helpMsg());
	} else if (e.getSource() == aboutItem) {
	    JOptionPane.showMessageDialog(this, JigUtil.aboutMsg());
	} else if (e.getSource() == pictureItem) {
	    showPicture();
	}
    }
}
