/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 * <p>
 * CSC = Client to Server Communication.  A simple interface that uses ServerProperties to pass data between
 * a SageClient and the Sage server.
 */
public class CSC {

    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_QUEUE = "queue";
    public static final String PROPERTY_CSC_STATUS = "CSC/status_";

    private static CSC instance = new CSC();

    private CSC() {};

    public static CSC getInstance() {
        return instance;
    }

    public void test() {
        String item = "test";
        String S = null;

        System.out.println("BEGIN CSC TEST");

        setStatus(item, "TEST");
        S = getStatus(item);
        System.out.println("After setting to TEST " + S);

        setStatus(item, null);
        S = getStatus(item);
        System.out.println("After setting to null " + S);

        addStatus(item, 0);
        S = getStatus(item);
        System.out.println("After adding 0 " + S);

        removeStatus(item, 0);
        S = getStatus(item);
        System.out.println("After removing 0 " + S);

        addStatus(item, 0);
        addStatus(item, 1);
        addStatus(item, 2);
        S = getStatus(item);
        System.out.println("After adding 0,1,2 " + S);

        removeStatus(item, 0);
        S = getStatus(item);
        System.out.println("After removing 0 " + S);
        removeStatus(item, 1);
        S = getStatus(item);
        System.out.println("After removing 1 " + S);
        removeStatus(item, 2);
        S = getStatus(item);
        System.out.println("After removing 2 " + S);

        addStatus(item, 0);
        addStatus(item, 1);
        addStatus(item, 2);
        S = getStatus(item);
        System.out.println("After adding 0,1,2 " + S);

        removeStatus(item, 2);
        S = getStatus(item);
        System.out.println("After removing 2 " + S);
        removeStatus(item, 1);
        S = getStatus(item);
        System.out.println("After removing 1 " + S);
        removeStatus(item, 0);
        S = getStatus(item);
        System.out.println("After removing 0 " + S);

        addStatus(item, 0);
        addStatus(item, 1);
        addStatus(item, 2);
        S = getStatus(item);
        System.out.println("After adding 0,1,2 " + S);

        removeStatus(item, 1);
        S = getStatus(item);
        System.out.println("After removing 1 " + S);
        removeStatus(item, 2);
        S = getStatus(item);
        System.out.println("After removing 2 " + S);
        removeStatus(item, 0);
        S = getStatus(item);
        System.out.println("After removing 0 " + S);

        addStatus(item, 0);
        addStatus(item, 1);
        addStatus(item, 2);
        S = getStatus(item);
        System.out.println("After adding 0,1,2 " + S);

        removeStatus(item, 1);
        S = getStatus(item);
        System.out.println("After removing 1 " + S);
        removeStatus(item, 0);
        S = getStatus(item);
        System.out.println("After removing 0 " + S);
        removeStatus(item, 2);
        S = getStatus(item);
        System.out.println("After removing 2 " + S);

        addStatus(item, 0);
        addStatus(item, 1);
        addStatus(item, 2);
        addStatus(item, 3);
        addStatus(item, 4);
        addStatus(item, 5);
        S = getStatus(item);
        System.out.println("After adding 0,1,2,3,4,5 " + S);

        removeStatus(item, 2);
        S = getStatus(item);
        System.out.println("After removing 2 " + S);
        removeStatus(item, 4);
        S = getStatus(item);
        System.out.println("After removing 4 " + S);
        removeStatus(item, 3);
        S = getStatus(item);
        System.out.println("After removing 3 " + S);
        addStatus(item, 2);
        addStatus(item, 4);
        addStatus(item, 3);
        S = getStatus(item);
        System.out.println("After adding back 2,4,3 " + S);

        setStatus(item, null);
        S = getStatus(item);
        System.out.println("After setting to null " + S);
        
        addStatus(item, 0);
        addStatus(item, 1);
        addStatus(item, 2);
        addStatus(item, 3);
        addStatus(item, 4);
        addStatus(item, 5);
        S = getStatus(item);
        System.out.println("After adding 0,1,2,3,4,5 " + S);

        String F = null;

        for (int i=0; i<6; i++) {
            F = getFirstStatus(item);
            S = getStatus(item);
            System.out.println("After getFirst " + F + " " + S);
        }
    }

    public void destroy() {
        instance = null;
    }

    public synchronized void setStatus(String item, String status) {
        if (item==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC setStatus: null item");
            return;
        }

        Configuration.SetServerProperty(PROPERTY_CSC_STATUS+item, status);
    }

    /**
     * Clears the status of item and sets it to status.
     *
     * @param item
     * @param status
     */
    public synchronized void setStatus(String item, int status) {
        setStatus(item, Integer.toString(status));
    }

    public synchronized void setStatus(String item, boolean status) {
        setStatus(item, Boolean.toString(status));
    }

    /**
     * Adds var to status item
     *
     * @param item
     * @param var
     */
    public synchronized void addStatus(String item, int var) {
        if (item==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC addStatus: null item");
            return;
        }
        
        String Status = getStatus(item);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC addStatus: item before add " + Status);
        
        if (Status==null || Status.isEmpty()) {
            setStatus(item, Integer.toString(var));
        } else {
            setStatus(item, Status + "," + Integer.toString(var));
        }
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CSC addStatus: item after add " + getStatus(item));    
    }

    public synchronized void removeStatus(String item, int var) {
        if (item==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC removeStatus: null item.");
            return;
        }

        String Status = getStatus(item);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC removeStatus: item before remove " + Status);

        if (Status==null || Status.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "CSC removeStatus: null Status.");
            return;
        }

        String[] items = Status.split(",");

        if (items==null || items.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "CSC removeStatus: null items.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC removeStatus: length of items " + items.length);

        String NewStatus = null;

        for (String S : items) {
            if (S==null || S.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC removeStatus: null entry detected.");
            } else {

                // Convert to int, checking for errors.
                int i = 0;
                try {
                    i = Integer.parseInt(S);
                } catch (NumberFormatException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC removeStatus: invalid entry detected " + S);
                    i = 0;
                }

                // If this is not the one we want to remove, add it to the String we will return.
                if (i!=var) {

                    // If it's the first item just make it the new String, if it's not the first,
                    // add it to the existing String and terminate with a comma.
                    if (NewStatus==null) {
                        NewStatus = S;
                    } else {
                        NewStatus = NewStatus + "," + S;
                    }

                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC removeStatus: after adding non-match " + NewStatus);
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC removeStatus: found item to remove " + NewStatus);
                }
            }
        }

        // Chop off the last comma.
        if (NewStatus !=null && NewStatus.endsWith(",")) {
            int EndPos = NewStatus.lastIndexOf(",");
            NewStatus = NewStatus.substring(0, EndPos);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC removeStatus: item after removing last comma " + NewStatus);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CSC removeStatus: item after remove " + NewStatus);
        setStatus(item, NewStatus);
    }

    public synchronized String getFirstStatus(String item) {
        if (item==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC getFirstStatus: null item.");
            return null;
        }

        String Status = getStatus(item);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC getFirstStatus: item before remove " + Status);

        if (Status==null || Status.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC getFirstStatus: null Status.");
            return null;
        }

        String[] items = Status.split(",");

        if (items==null || items.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "CSC getFirstStatus: null items.");
            return null;
        }

        // If there is only 1 item in the list, return it and set the list to null.
        if (items.length==1) {
            setStatus(item, null);
            return items[0];
        }

        // If there are two or more, remove the first item and return it.
        String FirstItem = items[0];

        int i = 0;
        try {
            i = Integer.parseInt(FirstItem);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC getFirstStatus: invalid FirstItem " + FirstItem);
            i = 0;
        }

        removeStatus(item, i);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC getFirstStatus: item after remove " + getStatus(item));
        return FirstItem;
    }
        
    /**
     * Returns the item completely raw.
     * 
     * @param item
     * @return
     */
    public synchronized String getStatus(String item) {
        if (item==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC getStatus: null item");
            return null;
        }

        return Configuration.GetServerProperty(PROPERTY_CSC_STATUS + item, null);
    }

    public synchronized boolean statusContains(String item, int var) {
        if (item==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC statusContains: null item.");
            return false;
        }

        String s = getStatus(item);

        if (s==null || s.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "CSC statusContains: No items.");
            return false;
        }

        String[] items = s.split(",");

        if (items==null || items.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC statusContains: null items. " + s);
            return false;
        }

        for (String S : items) {
            try {
                if (Integer.parseInt(S)==var) {
                    return true;
                }
            } catch (NumberFormatException e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "CSC statusContains: bad S " + S);
            }
        }

        return false;
    }

}
