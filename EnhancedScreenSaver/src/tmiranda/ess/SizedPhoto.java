package tmiranda.ess;

/**
 *
 * @author Tom Miranda.
 */
public class SizedPhoto extends RawPhoto {
    
    double height;
    double width;
    
    public SizedPhoto(Object MediaFile, double Height, double Width) {
        super(MediaFile);
        height = Height;
        width = Width;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }
    
    public PlacedPhoto setPos(double x, double y) {
        return new PlacedPhoto(MediaFile, height, width, x, y);
    }
}
