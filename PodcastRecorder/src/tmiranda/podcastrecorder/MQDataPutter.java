/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.lang.reflect.*;
import ortus.mq.*;
import ortus.mq.EventListener;
/**
 *
 * @author Tom Miranda
 */
public class MQDataPutter extends EventListener {

    /**
     * Constructor.  Register the OrtusEvents.
     */
    MQDataPutter() {
        super();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "MQDataPutter created.");
    }

    @OrtusEvent("MQDataPutterWithoutArgs")
    public void doDataPutterWithoutArgs(String ClientID, String InstanceID, String c, String m) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataPutterWithoutArgs invoked " + c + "." + m);

        if (ClientID==null || InstanceID==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithoutArgs: null ClientID or InstanceID " + ClientID + ":" + InstanceID);
            return;
        }

        if (c==null || c.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithoutArgs: null Class.");
            fireErrorCode(ClientID, InstanceID, "null Class.");
            return;
        }

        if (m==null || m.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithoutArgs: null Method.");
            fireErrorCode(ClientID, InstanceID, "null Method.");
            return;
        }

        Class<?> ec = null;

        try {ec = Class.forName(c);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Class exception " + e.getMessage());
            fireErrorCode(ClientID, InstanceID, "Class Exception");
            return;
        }

        Method method = null;

        try {method = ec.getMethod(m);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Method exception " + e.getMessage());
            fireErrorCode(ClientID, InstanceID, "Method Exception");
            return;
        }

        if (!Modifier.toString(method.getModifiers()).contains("static")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithoutArgs: Method is not static " + method.toString());
            fireErrorCode(ClientID, InstanceID, "Method Not Static");
            return;
        }

        Object returnedObject;

        try {returnedObject = method.invoke(null);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithoutArgs: Exception invoking method " + e.getMessage());
            fireErrorCode(ClientID, InstanceID, "Invoke Exception");
            return;
        }

        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                            ortus.mq.vars.EvenType.Clients,
                            "MQDataGetter",
                            new Object[] {ClientID, InstanceID, putterCheckForNull(returnedObject)});

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataPutterWithoutArgs done.");
    }

    @OrtusEvent("MQDataPutterWithArgs")
    public void doDataPutterWithArgs(String ClientID, String InstanceID, String c, String m, Object[] args) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataPutterWithArgs invoked " + c + "." + m + ":" + args.length);

        if (ClientID==null || InstanceID==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithArgs: null ClientID or InstanceID " + ClientID + ":" + InstanceID);
            return;
        }

        if (c==null || c.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithArgs: null Class.");
            fireErrorCode(ClientID, InstanceID, "null Class.");
            return;
        }

        if (m==null || m.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithArgs: null Method.");
            fireErrorCode(ClientID, InstanceID, "null Method.");
            return;
        }

        Class<?> ec = null;

        try {ec = Class.forName(c);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Class exception " + e.getMessage());
            fireErrorCode(ClientID, InstanceID, "Class Exception");
            return;
        }

        Method method = null;

        Class[] parmTypes = new Class[args.length];

        for (int i=0; i<args.length; i++) {
            parmTypes[i] = args[i].getClass();
        }

        try {method = ec.getMethod(m, parmTypes);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Method exception " + e.getMessage());
            fireErrorCode(ClientID, InstanceID, "Method Exception");
            return;
        }

        if (!Modifier.toString(method.getModifiers()).contains("static")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithArgs: Method is not static " + method.toString());
            fireErrorCode(ClientID, InstanceID, "Method Not Static");
            return;
        }

        Object returnedObject;

        try {returnedObject = method.invoke(null, args);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataPutterWithArgs: Exception invoking method " + e.getMessage());
            fireErrorCode(ClientID, InstanceID, "Invoke Exception");
            return;
        }

        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                            ortus.mq.vars.EvenType.Clients,
                            "MQDataGetter",
                            new Object[] {ClientID, InstanceID, putterCheckForNull(returnedObject)});

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataPutterWithArgs done.");
    }

    @OrtusEvent("MQInvokeMethodWithoutArgs")
    public void doInvokeMethodWithoutArgs(String c, String m) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doInvokeMethodWithoutArgs " + c + "." + m);

        if (c==null || c.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethodWithoutArgs: null Class.");
            return;
        }

        if (m==null || m.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethodWithoutArgs: null Method.");
            return;
        }

        Class<?> ec = null;

        try {ec = Class.forName(c);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Class exception " + e.getMessage());
            return;
        }

        Method method = null;

        try {method = ec.getMethod(m);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Method exception " + e.getMessage());
            return;
        }

        if (!Modifier.toString(method.getModifiers()).contains("static")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethod: Method is not static " + method.toString());
            return;
        }

        try {method.invoke(null);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethod: Exception invoking method " + e.getMessage());
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doInvokeMethod done.");
    }

    @OrtusEvent("MQInvokeMethodWithArgs")
    public void doInvokeMethodWithArgs(String c, String m, Object[] args) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doInvokeMethodWithArgs " + c + "." + m + ":" + args.length);

        if (c==null || c.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethodWithArgs: null Class.");
            return;
        }

        if (m==null || m.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethosWithArgs: null Method.");
            return;
        }

        Class<?> ec = null;

        try {ec = Class.forName(c);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Class exception " + e.getMessage());
            return;
        }

        Method method = null;

        Class[] parmTypes = new Class[args.length];
        for (int i=0; i<args.length; i++) {
            parmTypes[i] = args[i].getClass();
        }

        try {method = ec.getMethod(m, parmTypes);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Method exception " + e.getMessage());
            return;
        }

        if (!Modifier.toString(method.getModifiers()).contains("static")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethod: Method is not static " + method.toString());
            return;
        }

        try {method.invoke(null, args);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethod: Exception invoking method " + e.getMessage());
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doInvokeMethod done.");
    }

    private void fireErrorCode(String ClientID, String InstanceID, String Reason) {
        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                            ortus.mq.vars.EvenType.Clients,
                            "MQDataGetter",
                            new Object[] {ClientID, InstanceID, MQDataGetter.ERROR_DATA + "-" + Reason});
    }

    private Object putterCheckForNull(Object O) {
        return (O==null ? MQDataGetter.NULL_DATA : O);
    }
}
