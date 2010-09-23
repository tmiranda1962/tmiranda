/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.util.concurrent.*;
import java.util.UUID;
import sagex.api.*;
import ortus.mq.*;
import ortus.mq.EventListener;



/**
 *
 * @author Default
 */
public class MQDataGetter extends EventListener {
    
    private String ClientID;
    private BlockingQueue<Object> Queue;

    public static final String NULL_DATA = "MQNULLMQ";
    public static final String ERROR_DATA = "MQERRORMQ";

    MQDataGetter() {
        super();
        ClientID = UUID.randomUUID().toString();
        Queue = new LinkedBlockingQueue<Object>();
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MQDataGetter created for ClientID " + ClientID);

    }

    public Object getDataFromServer(String c, String m, Object[] args, long timeout) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "getDataFromServerWithArgs invoked " + c + "." + m + ":" + args.length);

        if (anyNullArgs(args)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Fatal error. Found null arg in " + c + "." + m);
        }

        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                                ortus.mq.vars.EvenType.Server,
                                "MQDataPutterWithArgs",
                                new Object[] {ClientID, getInstanceID(), c, m, args});

        Object data;
        try {data = Queue.poll(timeout,TimeUnit.MILLISECONDS);}
        catch (InterruptedException e) {data=null;}

        if (data==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getDataFromServer returned null. Operation may have timed out");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "getDataFromServer done.");
        return getterCheckForNull(data);
    }

    public Object getDataFromServer(String c, String m, long timeout) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "getDataFromServerWithoutArgs invoked " + c + "." + m);

        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                                ortus.mq.vars.EvenType.Server,
                                "MQDataPutterWithoutArgs",
                                new Object[] {ClientID, getInstanceID(), c, m});

        Object data = null;
        try {data = Queue.poll(timeout, TimeUnit.MILLISECONDS);}
        catch (InterruptedException e) {data=null;}

        if (data==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getDataFromServer returned null. Operation may have timed out");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "getDataFromServer done. data = " + data.toString());
        return getterCheckForNull(data);
    }

    private static String getInstanceID() {
        return UUID.randomUUID().toString();
    }

    @OrtusEvent("MQDataGetter")
    public void doDataGetter(String Client, String Instance, Object data) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataGetter invoked.");

        // ID can't be null.
        if (Client==null || Instance==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataGetter: null Client or Instance");
            return;
        }

        // Make sure the message is for this client.
        //if (!ClientID.equalsIgnoreCase(Client) || !Instance.equalsIgnoreCase(Instance)) {
        if (!ClientID.equalsIgnoreCase(Client)) {
            return;
        }

        // Add the data to the Queue, unblocking getDataFromServer.
        if (!Queue.add(data)) Log.getInstance().printStackTrace();
        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataGetter done.");
    }

    public void invokeMethodOnServer(String c, String m, Object[] args) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "invokeMethodOnServer " + c + "." + m + ":" + args.length);

        if (anyNullArgs(args)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Fatal error. Found null arg in " + c + "." + m);
        }

        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                                ortus.mq.vars.EvenType.Server,
                                "MQInvokeMethodWithArgs",
                                new Object[] {c, m, args});

        return;
    }

    public void invokeMethodOnServer(String c, String m) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "invokeMethodOnServer " + c + "." + m);

        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                                ortus.mq.vars.EvenType.Server,
                                "MQInvokeMethodWithoutArgs",
                                new Object[] {c, m});

        return;
    }

    private boolean anyNullArgs(Object[] args) {
        for (int i=0; i<args.length; i++) {
            if (args[i]==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Arg is null " + i);
                return true;
            }
        }

        return false;
    }

    private Object getterCheckForNull(Object O) {
        if (O.toString().startsWith(ERROR_DATA)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MQDataGetter: Error received from server " + O.toString());
            return null;
        }

        return (O.toString().equals(NULL_DATA) ? null : O);
    }
}




