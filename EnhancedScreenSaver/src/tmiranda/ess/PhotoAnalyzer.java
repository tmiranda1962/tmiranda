/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmiranda.ess;

/**
 *
 * @author Tom Miranda.
 */
public class PhotoAnalyzer {
    
    private double AR = 1.0;
    
    public PhotoAnalyzer(Object MediaFile) {
        AR = API.getPictureAR(MediaFile);
    }
    
    public boolean isLandscape() {
        return AR > 1.0;
    }
    
    public boolean isPortrait() {
        return AR < 1.0;
    }
    
    public boolean isSquare() {
        return AR == 1.0;
    }
    
    public double getWidthForHeight(double height) {
        return AR * height;
    }
    
}
