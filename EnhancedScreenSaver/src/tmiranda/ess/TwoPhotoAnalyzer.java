/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmiranda.ess;

/**
 *
 * @author Tom Miranda.
 */
public class TwoPhotoAnalyzer {
    
    private PhotoAnalyzer photo0;
    private PhotoAnalyzer photo1;
    
    private double x0 = 0.0;
    private double y0 = 0.0;
    
    private double height0 = 1.0;    
    private double width0 = 1.0;

    private double x1 = 0.0;
    private double y1 = 0.0;
    
    private double height1 = 1.0;    
    private double width1 = 1.0;    
    
    public TwoPhotoAnalyzer(Object MediaFile0, Object MediaFile1) {
    
        photo0 = new PhotoAnalyzer(MediaFile0);
        photo1 = new PhotoAnalyzer(MediaFile1);
        
        // Place them depending on their orientation.
        if (photo0.isLandscape() && photo1.isLandscape()) {
            
            // Both landscape.  Place them on different parallel planes.
            
        } else if (photo0.isPortrait() && photo1.isPortrait()) {
            
            // Both portrait. place them side by side.
            
        } else if (photo0.isLandscape() && photo1.isPortrait()) {
            
        } else {
            
            // photo0 is landscape and photo1 is portrait or one (or both) is square.
        }
    }
    
}
