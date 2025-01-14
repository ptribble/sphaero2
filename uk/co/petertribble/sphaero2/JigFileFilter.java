package uk.co.petertribble.sphaero2;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 * A file filter that just accepts images.
 *
 * @author Peter Tribble
 */
public final class JigFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
	return f.isDirectory() || JigUtil.isImage(f);
    }

    @Override
    public String getDescription() {
	return "Image files.";
    }
}
