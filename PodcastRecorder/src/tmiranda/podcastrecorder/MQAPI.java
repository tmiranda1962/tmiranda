/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.util.*;

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

    public static int GetOneParamters(int i) {
        return i+1;
    }

    public static boolean GetTwoParameters(boolean x, boolean y) {
        return x&&y;
    }
}
