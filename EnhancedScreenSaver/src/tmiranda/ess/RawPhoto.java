package tmiranda.ess;

import sagex.api.MediaFileAPI;
import sagex.api.Configuration;

/**
 *
 * @author Tom Miranda.
 */
public class RawPhoto {
    
    double AR;
    float displayAR;
    Object MediaFile;
    
    public RawPhoto(Object MF) {
        MediaFile = MF;
        displayAR = Configuration.GetDisplayAspectRatio();
        AR = getPictureAR();
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PhotoAnalyzer: DisplayAR:PhotoAR:AR="+displayAR+" : "+rawAR+" : "+AR);
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
    
    public double getHeightForWidth(double width) {
        return width / AR;
    }

    public double getAR() {
        return AR;
    }
    
    /**
     * Returns the aspect ratio of the photo based on the Picture.Resolution MetaData.
     * @param MediaFile
     * @return 
     */
    private double getPictureAR() {
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: null MediaFile.");
            return 1.0;
        }
        
        String text = MediaFileAPI.GetMediaFileMetadata(MediaFile, "Picture.Resolution");
        Log.getInstance().write(Log.LOGLEVEL_ALL, "getPictureAR: Picture.Resolution text " + text);
        if (text==null || text.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: null Picture.Resolution. ["+text+"]");           
            return 1.0;            
        }
     
        String[] parts = text.toLowerCase().split("x");
        if (parts==null || parts.length<2) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: Invalid Picture.Resolution " + text);       
            return 1.0;            
        }
        
        double aspect = 1.0;
        
        try {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "getPictureAR: Parts[0] " + parts[0].replaceAll("[^0-9]",""));  
            Log.getInstance().write(Log.LOGLEVEL_ALL, "getPictureAR: Parts[1] " + parts[1].replaceAll("[^0-9]",""));
            //int numerator = Integer.parseInt(parts[0].replaceAll("[^0-9]",""));
            //int denominator = Integer.parseInt(parts[1].replaceAll("[^0-9]",""));
            //float displayAR = Configuration.GetDisplayAspectRatio();
            //aspect = (double)(numerator*displayAR) / (double)denominator;
            aspect = (double)Integer.parseInt(parts[0].replaceAll("[^0-9]","")) / (double)Integer.parseInt(parts[1].replaceAll("[^0-9]",""));
        } catch (NumberFormatException e0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: NumberFormatException " + parts[0] + ":" + parts[1]);       
        } catch (Exception e1) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPictureAR: Exception " + parts[0] + ":" + parts[1]);          
        }
        
        Log.getInstance().write(Log.LOGLEVEL_ALL, "getPictureAR: AR=" + aspect);
        return aspect;
    }    
     
    public SizedPhoto setWidth(double width) {
        return new SizedPhoto(MediaFile, getHeightForWidth(width), width);
    }
    
    public SizedPhoto setHeight(double height) {
        return new SizedPhoto(MediaFile, height, getWidthForHeight(height));
    } 
    
    public PlacedPhoto sizeAndPlace(double minBorder, double minHeight, double maxHeight, double minWidth, double maxWidth) {
        return sizeAndPlace(minBorder, minHeight, maxHeight, minWidth, maxWidth, 0.0, 0.0, null);
    }
    
    public PlacedPhoto sizeAndPlace(double minBorder, double minHeight, double maxHeight, double minWidth, double maxWidth, double maxOverlapX, double maxOverlapY, PlacedPhoto[] placedPhotos) {
    
        if (placedPhotos==null || placedPhotos.length==0) {
            
            double minX;
            double maxX;
            double minY;
            double maxY;
            
            double h;
            double w;

            if (this.isLandscape()) {                 
                w = Rand.getRandom(minWidth, maxWidth);
                h = this.getHeightForWidth(w);
            } else {
                h = Rand.getRandom(minHeight, maxHeight);
                w = this.getWidthForHeight(h);
            }
            
            minX = minBorder;
            maxX = 1.0 - minBorder - w;

            minY = minBorder;
            maxY = 1.0 - minBorder - h;            
            
            double x = Rand.getRandom(minX, maxX);
            double y = Rand.getRandom(minY, maxY);
            
            return new PlacedPhoto(MediaFile, h, w, x, y);
        }
        
        PlacedPhoto p = placedPhotos[0];
        double biggest = 0.0;
        
        // Loop through all of the PlacedPhotos and find the biggest available 'hole' to put the new photo.
        // FIXME - Only works for one photo right now.
        for (PlacedPhoto placed : placedPhotos) {            
            biggest = placed.getLargestGap();
            p = placed;
        }
        
        // Now put the photo into the biggest gap.
        
        double minH;
        double maxH;
        double minW;
        double maxW;
        double minX;
        double maxX;
        double minY;
        double maxY;
        
        double newHeight;
        double newWidth;
        double newX;
        double newY;
        
        if (biggest==p.getTopGap()) {
            
            if (p.getLeftGap() > p.getRightGap()) {
                minX = minBorder;
                maxX = p.getxPos() + (maxOverlapX*p.getWidth()) - minWidth;
            } else {
                minX = p.getxPos() - (maxOverlapX*p.getWidth()) + p.getWidth();
                maxX = 1.0 - minBorder - minWidth;                
            }
            
            newX = Rand.getRandom(minX, maxX);
            
            minY = minBorder;
            maxY = p.getTopGap() + (maxOverlapY*p.getHeight()) - minHeight;
            newY = Rand.getRandom(minY, maxY);
                    
            minH = minHeight;
            maxH = p.getyPos() + (maxOverlapY*p.getHeight()) - newY;
            newHeight = Rand.getRandom(minH, maxH);
            
            newWidth = this.getWidthForHeight(newHeight);
            
        } else if (biggest==p.getRightGap()) {
            
            minX = p.getxPos() - (maxOverlapX*p.getWidth());
            maxX = 1.0 - minBorder - minWidth;
            newX = Rand.getRandom(minX, maxX);
            
            if (p.getTopGap() > p.getBottomGap()) {
                minY = minBorder;
                maxY = p.getyPos() + (maxOverlapY*p.getHeight()) - minHeight;           
            } else {
                minY = p.getyPos() - (maxOverlapY*p.getHeight());
                maxY = 1.0 - minBorder - minHeight;              
            }
            
            newY = Rand.getRandom(minY, maxY);
                    
            minW = minWidth;
            maxW = 1.0 - newX - minBorder;
            newWidth = Rand.getRandom(minW, maxW);
            
            newHeight = this.getHeightForWidth(newWidth);
            
            
        } else if (biggest==p.getBottomGap()) {
            
            if (p.getLeftGap() > p.getRightGap()) {
                minX = minBorder;
                maxX = p.getxPos() + (maxOverlapX*p.getWidth()) - minWidth;
            } else {
                minX = p.getxPos() - (maxOverlapX*p.getWidth()) + p.getWidth();
                maxX = 1.0 - minBorder - minWidth;                
            }

            newX = Rand.getRandom(minX, maxX);
            
            minY = p.getyPos() + p.getHeight() - (maxOverlapY*p.getHeight());
            maxY = 1.0 - minBorder - minHeight;
            newY = Rand.getRandom(minY, maxY);
            
            minH = minHeight;
            maxH = 1.0 - minBorder - newY;
            newHeight = Rand.getRandom(minH, maxH);
            
            newWidth = this.getWidthForHeight(newHeight);
            
        } else { // Left
            
            minX = minBorder;
            maxX = p.getxPos() + (maxOverlapX+p.getWidth()) - minWidth;
            newX = Rand.getRandom(minX, maxX);
            
            if (p.getTopGap() > p.getBottomGap()) {
                minY = minBorder;
                maxY = p.getyPos() + (maxOverlapY*p.getHeight()) - minHeight;           
            } else {
                minY = p.getyPos() - (maxOverlapY*p.getHeight());
                maxY = 1.0 - minBorder - minHeight;              
            }
            
            newY = Rand.getRandom(minY, maxY);
            
            minW = minWidth;
            maxW = p.getxPos() + (maxOverlapX*p.getWidth()) - newX;
            newWidth = Rand.getRandom(minW, maxW);
            
            newHeight = this.getHeightForWidth(newWidth);
        }

        
        return new PlacedPhoto(MediaFile, newHeight, newWidth, newX, newY);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RawPhoto other = (RawPhoto) obj;
        if (Double.doubleToLongBits(this.AR) != Double.doubleToLongBits(other.AR)) {
            return false;
        }
        if (this.MediaFile != other.MediaFile && (this.MediaFile == null || !this.MediaFile.equals(other.MediaFile))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.AR) ^ (Double.doubleToLongBits(this.AR) >>> 32));
        hash = 47 * hash + (this.MediaFile != null ? this.MediaFile.hashCode() : 0);
        return hash;
    }    
}
