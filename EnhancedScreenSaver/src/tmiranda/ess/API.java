/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmiranda.ess;
import sagex.api.MediaFileAPI;

/**
 *
 * @author Tom Miranda
 */
public class API {
    
    public static double getRandom(double min, double max) {
        return Math.min(Math.max(Math.random(),min),max);
    }
    
    public static double getPictureAR(Object MediaFile) {
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: null MediaFile.");
            return 1.0;
        }
        
        String text = MediaFileAPI.GetMediaFileMetadata(MediaFile, "Picture.Resolution");
        if (text==null || text.length()==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: null Picture.Resolution.");            
            return 1.0;            
        }
    
// FIXME        
Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: Picture.Resolution text " + text);         
        
        String[] parts = text.toLowerCase().split("x");
        if (parts==null || parts.length<2) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: Invalid Picture.Resolution " + text);       
            return 1.0;            
        }
        
        double AR = 1.0;
        
        try {
            AR = Integer.parseInt(parts[0].replaceAll("^[0-9]","")) / Integer.parseInt(parts[1].replaceAll("^[0-9]",""));
        } catch (NumberFormatException e0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: NumberFormatException " + parts[0] + ":" + parts[1]);       
        } catch (Exception e1) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: Exception " + parts[0] + ":" + parts[1]);          
        }
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getPictureAR: AR=" + AR);
        return AR;
    }
    
    
    public static double getWidthForHeight(Object MediaFile, double height) {
        return getPictureAR(MediaFile) * height;
    }
    
}
