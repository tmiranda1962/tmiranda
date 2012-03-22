/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmiranda.ess;

import java.util.Random;

/**
 *
 * @author Tom Miranda
 */
class Rand {
    
    private static Random random = new Random();
    
    /**
     * Returns a random double between min and max.
     * @param min
     * @param max
     * @return 
     */
    public static double getRandom(double min, double max) {
        
        min = Math.max(0.0, min);           // Make sure it's not negative.
        max = Math.min(1.0, max);           // Make sure it's not > 1.0.
        if (min>max)                        // Check range.
            max = min;
        double rand = random.nextDouble();
        return min + (rand*(max-min));
    }
    
    public static double getRandom() {
        return random.nextDouble();
    }
    
    /**
     * Returns a random variance (positive or negative between minVariance and MaxVariance.
     * @param minVariance
     * @param maxVariance
     * @return 
     */
    public static double getRandomOffset(double minVariance, double maxVariance) {
        
        minVariance = Math.abs(minVariance);
        maxVariance = Math.abs(maxVariance);
        
        double offset = getRandom(minVariance, maxVariance);
        double direction = (random.nextDouble()<=0.5 ? -1.0 : 1.0);
        return offset * direction;
    }
    
    public static double getRandomOffset(double minVariance, double maxVariance, double direction) {
        return getRandom(minVariance, maxVariance) * direction;
    }
    
}
