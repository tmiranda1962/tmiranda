/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmiranda.ess;

/**
 *
 * @author Tom Miranda.
 */
public class PlacedPhoto extends SizedPhoto {
    
    private double xPos;
    private double yPos;
    
    public PlacedPhoto(Object MediaFile, double height, double width, double x, double y) {
        super(MediaFile, height, width);
        xPos = x;
        yPos = y;
    }

    public double getxPos() {
        return xPos;
    }

    public double getyPos() {
        return yPos;
    }
    
    public double getInsideX() {
        double endX = xPos + width;
        return xPos > 1.0 - endX ? xPos : endX;
    }
    
    public double getInsideY() {
        double endY = yPos + height;
        return yPos > 1.0 - endY ? yPos : endY;
    }    
    
    public double getTopGap() {
        return yPos;
    }
    
    public double getBottomGap() {
        return 1.0 - yPos + height;
    }
    
    public double getLeftGap() {
        return xPos;
    }
    
    public double getRightGap() {
        return 1.0 - xPos + width;
    }
    
    public double getLargestGap() {
        return Math.max(getTopGap(), Math.max(getRightGap(), Math.max(getBottomGap(), getLeftGap())));
    }
    
    // Returns true if the photo occupies any portion of the space defined.
    public boolean occupies(double spaceStartX, double spaceStartY, double h, double w) {
    
        double photoStartX = xPos;
        double photoEndX = xPos+width;
        double spaceEndX = spaceStartX + w;
        
        double photoStartY = yPos;
        double photoEndY = yPos + height;
        double spaceEndY = spaceStartY + h;

        if (photoEndX<spaceStartX || photoEndY<spaceStartY || photoStartX>spaceEndX || photoStartY>spaceEndY)
            return false;
        else    
            return true;
    }
    
    @Override
    public String toString() {
        return "Height=" + height + " Width=" + width + " X=" + xPos + " Y=" + yPos;
    }
    
}
