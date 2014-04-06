package uk.co.petertribble.sphaero2;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A label showing how long it has taken to solve the puzzle.
 *
 * @author Peter Tribble
 */
public class TimeLabel extends JLabel implements ActionListener {

    private Timer timer;
    private long startmillis;

    public TimeLabel() {
	super("|", SwingConstants.RIGHT);
    }

    // FIXME pause and resume

    public void start() {
	startmillis = System.currentTimeMillis();
	if (timer == null) {
	    timer = new Timer(1000, this);
	}
	timer.start();
    }

    public void finished() {
	if (timer != null) {
	    timer.stop();
	}
	long elapsed = System.currentTimeMillis() - startmillis;
	setText("Solution time: " + elapsed/1000 + "s");
    }

    public void updateTime() {
	long elapsed = System.currentTimeMillis() - startmillis;
	setText("Elapsed time: " + elapsed/1000 + "s");
    }

    public void actionPerformed(ActionEvent e) {
	updateTime();
    }
}
