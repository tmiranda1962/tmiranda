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

        Class ec = null;

        try {ec = Class.forName(c);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Class exception " + e.getMessage());
            e.printStackTrace();
            fireErrorCode(ClientID, InstanceID, "Class Exception");
            return;
        }

        Method method = null;

        try {method = ec.getMethod(m);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Method exception " + e.getMessage());
            e.printStackTrace();
            fireErrorCode(ClientID, InstanceID, "Method Exception");
            return;
        }

        if (!Modifier.toString(method.getModifiers()).contains("static")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataGetter: Method is not static " + method.toString());
            fireErrorCode(ClientID, InstanceID, "Method Not Static");
            return;
        }

        Object returnedObject;

        try {returnedObject = method.invoke(null);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataGetter: Exception invoking method " + e.getMessage());
            e.printStackTrace();
            fireErrorCode(ClientID, InstanceID, "Invoke Exception");
            return;
        }

        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                            ortus.mq.vars.EvenType.Clients,
                            "MQDataGetter",
                            new Object[] {ClientID, InstanceID, putterCheckForNull(returnedObject)});

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataPutter done.");
    }

    @OrtusEvent("MQDataPutterWithArgs")
    public void doDataPutterWithArgs(String ClientID, String InstanceID, String c, String m, Object[] args) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataPutterWithArgs invoked " + c + "." + m + ":" + args.length);

        Class ec = null;

        try {ec = Class.forName(c);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Class exception " + e.getMessage());
            e.printStackTrace();
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
            e.printStackTrace();
            fireErrorCode(ClientID, InstanceID, "Method Exception");
            return;
        }

        if (!Modifier.toString(method.getModifiers()).contains("static")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataGetter: Method is not static " + method.toString());
            fireErrorCode(ClientID, InstanceID, "Method Not Static");
            return;
        }

        Object returnedObject;

        try {returnedObject = method.invoke(null, args);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doDataGetter: Exception invoking method " + e.getMessage());
            e.printStackTrace();
            fireErrorCode(ClientID, InstanceID, "Invoke Exception");
            return;
        }

        ortus.mq.api.fireMQMessage(ortus.mq.vars.MsgPriority.High,
                            ortus.mq.vars.EvenType.Clients,
                            "MQDataGetter",
                            new Object[] {ClientID, InstanceID, putterCheckForNull(returnedObject)});

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doDataPutter done.");
    }

    @OrtusEvent("MQInvokeMethodWithoutArgs")
    public void doInvokeMethodWithoutArgs(String c, String m) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doInvokeMethodWithoutArgs " + c + "." + m);

        Class ec = null;

        try {ec = Class.forName(c);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Class exception " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Method method = null;

        try {method = ec.getMethod(m);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Method exception " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (!Modifier.toString(method.getModifiers()).contains("static")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethod: Method is not static " + method.toString());
            return;
        }

        try {method.invoke(null);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethod: Exception invoking method " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doInvokeMethod done.");
    }

    @OrtusEvent("MQInvokeMethodWithArgs")
    public void doInvokeMethodWithArgs(String c, String m, Object[] args) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "doInvokeMethodWithArgs " + c + "." + m + ":" + args.length);

        Class ec = null;

        try {ec = Class.forName(c);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Class exception " + e.getMessage());
            e.printStackTrace();
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
            e.printStackTrace();
            return;
        }

        if (!Modifier.toString(method.getModifiers()).contains("static")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethod: Method is not static " + method.toString());
            return;
        }

        try {method.invoke(null, args);}
        catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "doInvokeMethod: Exception invoking method " + e.getMessage());
            e.printStackTrace();
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
