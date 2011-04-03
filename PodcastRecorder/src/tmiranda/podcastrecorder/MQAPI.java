
package tmiranda.podcastrecorder;


/**
 *
 * @author Default
 */
public class MQAPI {

    public String TestNonStatic() {
        return "DATA";
    }

    public static String GetNoParameters() {
        System.out.println("MQAPI WAS INVOKED");
        return "!!!DATA FROM MQAPI!!!";
    }

    public static int GetOneParameters(int i) {
        return i+1;
    }

    public static boolean GetTwoParameters(boolean x, boolean y) {
        return x&&y;
    }
}
