package uk.co.petertribble.sphaero2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * A label showing how long it has taken to solve the puzzle.
 *
 * @author Peter Tribble
 */
public final class TimeLabel extends JLabel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private Timer timer;
    private long startmillis;
    private long pausemillis;

    private static final String ELAPSED_LABEL = "Elapsed time: ";
    private static final String PAUSED_LABEL = "(Paused) time: ";
    private String currentLabel;

    /**
     * Create a new TimeLabel, that will show the current running and
     * final elapsed time taken to solve the puzzle.
     */
    public TimeLabel() {
	super("|", SwingConstants.RIGHT);
	currentLabel = ELAPSED_LABEL;
    }

    /**
     * Start solving the puzzle.
     */
    public void start() {
	startmillis = System.currentTimeMillis();
	if (timer == null) {
	    timer = new Timer(1000, this);
	}
	timer.start();
    }

    /**
     * Pause solving the puzzle.
     */
    public void pause() {
	timer.stop();
	currentLabel = PAUSED_LABEL;
	pausemillis = System.currentTimeMillis();
	updateTime();
    }

    /**
     * Restart solving the puzzle.
     */
    public void unpause() {
	timer.start();
	currentLabel = ELAPSED_LABEL;
	startmillis += System.currentTimeMillis() - pausemillis;
	updateTime();
    }

    /**
     * Finish solving the puzzle. The text will change from the current
     * running time to the final solution time, and will no longer be
     * updated.
     */
    public void finished() {
	if (timer != null) {
	    timer.stop();
	}
	long elapsed = System.currentTimeMillis() - startmillis;
	setText("Solution time: " + elapsed/1000 + "s");
    }

    private void updateTime() {
	long elapsed = System.currentTimeMillis() - startmillis;
	setText(currentLabel + elapsed/1000 + "s");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
	updateTime();
    }
}
