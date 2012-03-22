package tmiranda.ess;

/**
 *
 * @author Tom Miranda.
 */
public class TwoPhotoAnalyzer {
    
    private double minBorder    = 0.05;     // Minimum gap to end of screen.
    private double maxBorder    = 0.15;     // Maximum gap to end of screen.
    
    private double maxOverlapX  = 0.20;     // Maximum amount pictures may overlap in X plane.
    private double maxOverlapY  = 0.20;     // Maximum amount pictures may overlap in Y plane.
    
    private double minWidth     = 0.45;     // Minimum width of first picture.
    private double minHeight    = 0.45;     // Minimum height of first picture.
    
    private double maxWidth     = 0.65;     // Maximum width of first picture.
    private double maxHeight    = 0.65;     // Maximum height of first picture.
    
    private PlacedPhoto placedPhoto0;
    private PlacedPhoto placedPhoto1;

    //private double x1 = 0.0;
    //private double y1 = 0.0;   
    
    /**
     * Constructor.
     * @param MediaFile0
     * @param MediaFile1 
     */
    public TwoPhotoAnalyzer(Object MediaFile0, Object MediaFile1) {
        
        // FIXME
        Log.getInstance().SetLogLevel(Log.LOGLEVEL_VERBOSE);
    
        RawPhoto photo0 = new RawPhoto(MediaFile0);
        RawPhoto photo1 = new RawPhoto(MediaFile1);
        
        SizedPhoto sizedPhoto0;
           
        // Size the first picture honoring the min and max dimensions.
        if (photo0.isLandscape()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "TwoPhotoAnalyzer: Landscape.");
            sizedPhoto0 = photo0.setWidth(Rand.getRandom(minWidth, maxWidth));
        } else {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "TwoPhotoAnalyzer: Portrait or square.");
            sizedPhoto0 = photo0.setHeight(Rand.getRandom(minHeight, maxHeight));
        }
        
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "TwoPhotoAnalyzer: Height0:Width0:AR = " + sizedPhoto0.getHeight()+":"+sizedPhoto0.getWidth()+":"+photo0.getAR());
        
        // Position first picture leaving minBorder around any edge.
        double x0 = Rand.getRandom()<0.5 ? Rand.getRandom(minBorder, maxBorder) : Rand.getRandom(1.0 - sizedPhoto0.getWidth() - maxBorder, 1.0 - sizedPhoto0.getWidth() -minBorder);
        double y0 = Rand.getRandom()<0.5 ? Rand.getRandom(minBorder, maxBorder) : Rand.getRandom(1.0 - sizedPhoto0.getHeight() - maxBorder, 1.0 - sizedPhoto0.getHeight() - minBorder);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "TwoPhotoAnalyzer: x0:y0 = " + x0+":"+y0);
        placedPhoto0 = sizedPhoto0.setPos(x0, y0);
        
        PlacedPhoto[] photoArray = {placedPhoto0};
        placedPhoto1 = photo1.sizeAndPlace(minBorder, minHeight, maxHeight, minWidth, maxWidth, maxOverlapX, maxOverlapY, photoArray);
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "TwoPhotoAnalyzer: Placed0 = " + placedPhoto0.toString());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "TwoPhotoAnalyzer: Placed1 = " + placedPhoto1.toString());
    }
    
    public double getHeight0() {
        return placedPhoto0.getHeight();
    }

    public double getHeight1() {
        return placedPhoto1.getHeight();
    }

    public double getWidth0() {
        return placedPhoto0.getWidth();
    }

    public double getWidth1() {
        return placedPhoto1.getWidth();
    }

    public double getX0() {
        return placedPhoto0.getxPos();
    }

    public double getX1() {
        return placedPhoto1.getxPos();
    }

    public double getY0() {
        return placedPhoto0.getyPos();
    }

    public double getY1() {
        return placedPhoto1.getyPos();
    }
      
}
