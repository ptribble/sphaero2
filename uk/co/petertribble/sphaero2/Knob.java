package uk.co.petertribble.sphaero2;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

// ### Trouble with this scheme: roundoff error, apparently.  Transformed
//   paths aren't guaranteed to end up exactly at the endpoints.
//   ClassicCutter closes the paths, but the result often has ugly bits
//   sticking out at the corners.
//   The rightest solution would probably be to join the Knobs into
//   continuous paths going all the way across the puzzle, and then use
//   their intersections, etc. to compute piece corners.  I'll revisit this
//   later.

// ### Need a way to vary the slope at each point.  XDB and XDF need to be
//   redone as plain distances, rather than X-axis variations.

/**
 * A knob on one side of an interlocking jigsaw piece.  Knobs are also
 * known as tabs.
 */
public class Knob {

    // Control data.  There are five points describing a knob.
    //  #1 is the start corner.
    //  #2 is inside the knob neck.
    //  #3 is at the top of the knob.
    //  #4 is on the knob neck opposite #2.
    //  #5 is the end corner.

    // For each point, there are five control values:
    //  the X,Y coordinates of the point
    //  the slope of the control line passing through that point
    //    (positive if NW-SE, negative if SW-NE)
    //  the X-delta of the control point behind that point
    //    (higher delta = deeper curve)
    //  the X-delta of the control point ahead of that point
    //    (higher delta = deeper curve)

    // The sign of the X-deltas can be thought of as describing an arrow
    // going through the main points, on top of the slope line.  If the
    // sign is positive, the arrow goes left-to-right; if negative, it goes
    // right-to-left.  As the curve goes forward through each point, it
    // must go in the direction of each arrow.

    // Control is described this way, rather than with four control points
    // for each Bezier curve (counting both endpoints), because it makes
    // the entire curve smooth.

    // X and the two X-deltas are each scaled by the width of the piece.
    // Y is scaled by the height of the piece.

    // The knob described protrudes north.  The values are transformed as
    // needed to point the knob in any direction.

    private static final float[][] CTL = new float[][] {
	// X Y SLOPE XDB XDF
	{0f, 0f, 1/8f, 0f, 3/16f, },
	    {1/3f, -4/32f,  5/4f, -1/ 8f, -1/10f, },
		{1/2f, -5/16f, 0f,  1/16f,  1/16f, },
		    {2/3f, -4/32f, -5/4f, -1/10f, -1/ 8f, },
			{1f, 0f, -1/8f, 3/16f, 0f, },
			    };
    private static final int X = 0;
    private static final int Y = 1;
    private static final int SLOPE = 2;
    private static final int XDB = 3;
    private static final int XDF = 4;

    // x,y each varies by + or - itsVar
    private static final float XVARY = 1/20f;
    private static final float YVARY = 1/20f;
    // b,f each varies by + or - it*itsVar
    private static final float XDBVARY = 1/4f;
    private static final float XDFVARY = 1/4f;

    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;
    private GeneralPath cPath;
    private GeneralPath cPathReverse;

    /**
     * Creates a new Knob, anchored on the given coordinates.
     *
     * @param x1 x coordinate of the start endpoint
     * @param y1 y coordinate of the start endpoint
     * @param x2 x coordinate of the finish endpoint
     * @param y2 y coordinate of the finish endpoint
     */
    public Knob(int x1, int y1, int x2, int y2) {
	this.x1 = x1;
	this.y1 = y1;
	this.x2 = x2;
	this.y2 = y2;
	initPath();
    }

    private void initPath() {
	float[][] data = new float[CTL.length][];
	for (int i = 0; i < data.length; i++) {
	    data[i] = CTL[i].clone();
	}

	jitter(data, XVARY, YVARY, XDBVARY, XDFVARY);

	cPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, data.length*3 - 2);
	cPathReverse
          = new GeneralPath(GeneralPath.WIND_NON_ZERO, data.length*3 - 2);
	cPath.moveTo(data[0][X], data[0][Y]);
	cPathReverse.moveTo(data[data.length-1][X], data[data.length-1][Y]);
	for (int i = 0; i < data.length-1; i++) {
	    curveTo(cPath, data, i, true);
	    curveTo(cPathReverse, data, data.length-1-i, false);
	}

	// Transform to coincide with line segment (x1,y1)-(x2,y2)
	AffineTransform affine =
	    new AffineTransform(x2-x1, y2-y1, y1-y2, x2-x1, x1, y1);
	cPath.transform(affine);
	cPathReverse.transform(affine);
    }

    private void curveTo(GeneralPath path, float[][] data,
			  int idx, boolean forward) {
	int delta = forward ? 1 : -1;
	float cx1 = data[idx][X];
	float cy1 = data[idx][Y];
	float m1 = data[idx][SLOPE];
	float d1f = data[idx][forward?XDF:XDB];
	float cx2 = data[idx+delta][X];
	float cy2 = data[idx+delta][Y];
	float m2 = data[idx+delta][SLOPE];
	float d2b = data[idx+delta][forward?XDB:XDF];
	if (!forward) {
	    d1f *= -1.0f;
	    d2b *= -1.0f;
	}
	float x1 = cx1 + d1f;
	float y1 = cy1 + d1f*m1;
	float x2 = cx2 - d2b;
	float y2 = cy2 - d2b*m2;
	path.curveTo(x1, y1, x2, y2, cx2, cy2);
    }

    /**
     * Returns a copy of the path used to create this Knob, starting with
     * the given endpoint.
     *
     * @param x the x coordinate of the endpoint
     * @param y the y coordinate of the endpoint
     *
     * @return a copy of the Path bounding this Knob
     *
     * @throws IllegalArgumentException if (x,y) is not an endpoint of this
     *   Knob
     */
    public GeneralPath getCurvePath(int x, int y) {
	if ((x == x1) && (y == y1)) {
	    return (GeneralPath) cPath.clone();
	} else if ((x == x2) && (y == y2)) {
	    return (GeneralPath) cPathReverse.clone();
	} else {
	    throw new IllegalArgumentException(
					"Not an endpoint: ("+x+","+y+")");
	}
    }

    /**
     * Returns a rectangle bounding this Knob.
     *
     * @return the Rectangle bounding this Knob
     */
    public Rectangle getBounds() {
	return cPath.getBounds();
    }

    private void jitter(float[][] pts, float xVar, float yVar,
			 float bVar, float fVar) {
	for (int i = 0; i < pts.length; i++) {
	    float b = pts[i][XDB];
	    float f = pts[i][XDF];
	    // x,y each varies by + or - itsVar
	    // first and last x do not vary
	    if ((i > 0) && (i < pts.length-1)) {
		pts[i][X] += Math.random()*xVar*2 - xVar;
	    }
	    pts[i][Y] += Math.random()*yVar*2 - yVar;
	    // b,f each varies by + or - it*itsVar
	    pts[i][XDB] += Math.random()*b*bVar*2 - b*bVar;
	    pts[i][XDF] += Math.random()*f*fVar*2 - f*fVar;
	}
    }

    @Override
    public String toString() {
	return "Knob[p1=("+x1+","+y1+"),p2=("+x2+","+y2+")]";
    }
}
