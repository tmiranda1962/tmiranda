/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmiranda.ess;

/**
 *
 * @author Tom
 */
public class OnePhotoAnalyzer {
    
    private double minBorder    = 0.05;     // Minimum gap to end of screen.
    private double maxBorder    = 0.15;     // Maximum gap to end of screen.
    
    private double minWidth     = 0.45;     // Minimum width of first picture.
    private double minHeight    = 0.45;     // Minimum height of first picture.
    
    private double maxWidth     = 0.65;     // Maximum width of first picture.
    private double maxHeight    = 0.65;     // Maximum height of first picture.
    
    private PlacedPhoto placedPhoto;
    
    public OnePhotoAnalyzer(Object MediaFile) {
        
        // FIXME
        Log.getInstance().SetLogLevel(Log.LOGLEVEL_VERBOSE);
    
        RawPhoto photo = new RawPhoto(MediaFile);       
        placedPhoto = photo.sizeAndPlace(minBorder, minHeight, maxHeight, minWidth, maxWidth);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "TwoPhotoAnalyzer: Placed = " + placedPhoto.toString());
    }    
    
    public double getHeight() {
        return placedPhoto.getHeight();
    }

    public double getWidth() {
        return placedPhoto.getWidth();
    }

    public double getY() {
        return placedPhoto.getyPos();
    } 
    
    public double getX() {
        return placedPhoto.getxPos();
    }     
}
