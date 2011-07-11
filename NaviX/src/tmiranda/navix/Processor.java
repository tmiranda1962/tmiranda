
package tmiranda.navix;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The methods in this class are used to invoke the Processor that translates the url into
 * an actual file (video, audio, text, etc) that can be played.
 *
 * Many websites either imbed the video into their webpages or provide a url that is
 * dependent upon the IP address of the requester and time of the request.  NaviX gets around
 * this by using "Processors".  A detailed explanation can be found on the Navi-Xtreme website
 * but in a nutshell a processor is really just a set of commands written in a simple
 * language (called NIPL) that tells a local client how to get a video link from a website.
 *
 * @author Tom Miranda.
 */
public class Processor implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    private String UrlString;
    private URL url;

    private static HashMap<String,String>   vars=new HashMap<String,String>();
    private static HashMap<String,String>   rvars=new HashMap<String,String>();
    public static HashMap<String,String>    nookies=new HashMap<String,String>();
    private static long                     lastExpire=0;

    public Processor (String URLString) {

        if (URLString==null || URLString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Processor: null URL.");
        }

        UrlString = URLString;

        try {
            url = new URL(UrlString);
        } catch (MalformedURLException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor: Malformed URL " + UrlString);
        }
    }

    /**
     * Send the Command to the URL specified in the constructor and return the response
     * in a List of Strings.
     * 
     * @param Command
     * @return
     */
    public List<String> send(String Command) {

        List<String> answer = new ArrayList<String>();

        URLConnection connection = null;

        try {
            connection = url.openConnection();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.send: Error opening connection " + e.getMessage());
            return null;
        }

        connection.setDoOutput(true);

        OutputStreamWriter out = null;

        try {
            out = new OutputStreamWriter(connection.getOutputStream());
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.send: Error creating OutputStreamWriter " + e.getMessage());
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Processor.send: Sending " + Command);

        try {
            out.write(Command);
            out.close();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.send: Error sending " + e.getMessage());
            return null;
        }

        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.send: Error creating BufferedReader " + e.getMessage());
            return null;
        }

        String inputLine = null;

        try {
            while ((inputLine = in.readLine()) != null) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Processor.send: Read " + inputLine);
                answer.add(inputLine);
            }
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.send: Error reading " + e.getMessage());
        }

        try {
            in.close();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.send: Error closing BufferedReader " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.send: Returning " + answer);
        return answer;
    }

    private static String escapeChars(String str) {
        StringBuilder sb=new StringBuilder();
        // str=str.replaceAll("\\(", "\\\\\\(");
        for(int i=0;i<str.length();i++) {
            char ch;
            switch((ch=str.charAt(i))) {
            case '\"':
                sb.append("\\\"");
                    break;
            case '\'':
                sb.append("\\\'");
                break;
            /*case '\\':
                sb.append("\\\\");
                break;*/
            default:
                sb.append(ch);
            break;
            }
        }
        return sb.toString();
    }

    private static String fixVar(String val,String storedVal) {
        if(val.charAt(0)=='\'') { // string literal
            int stop=val.length();
            /* if(val.charAt(stop-1)=='\'')
            return val.substring(1, stop-1);
        else*/
            return val.substring(1);
        }
        else { // variable
            return storedVal;
        }
    }

    private static boolean eqOp(String op) {
        if(op.equals("<=")) // lte
            return true;
        if(op.equals("=")||op.equals("=="))
            return true;
        if(op.equals(">="))
            return true;
        return false;
    }

    private static boolean boolOp(String var,String op,String comp) {
        if(var==null)
            var="";
        if(comp==null)
            comp="";
        if(op.equals("<")) // less then
            return (var.compareTo(comp)<0);
        if(op.equals("<=")) // lte
            return (var.compareTo(comp)<=0);
        if(op.equals("=")||op.equals("=="))
            return (var.compareTo(comp)==0);
        if(op.equals("!=")||op.equals("<>"))
            return (var.compareTo(comp)!=0);
        if(op.equals(">"))
            return (var.compareTo(comp)>0);
        if(op.equals(">="))
            return (var.compareTo(comp)>=0);
        return false;
    }

    private static String getVar(String key) {
        if(key.startsWith("pms_stash.")) {
            // special PMS stash
            String[] kSplit=key.split(".",3);
            if(kSplit.length>1) {
                String stash="default";
                String sKey=kSplit[1];
                if(kSplit.length>2) {
                    stash=kSplit[1];
                    sKey=kSplit[2];
                }
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.getVar: Tried to read pms_stash.");
                return null;
                // FIXME - SHARKHUNTER
                //return Channels.getStashData(stash, sKey);
            }
        }
        return vars.get(key);
    }

    private static void putVar(String key,String val) {
        if(key.startsWith("pms_stash.")) {
            // special PMS stash
            String[] kSplit=key.split(".",3);
            if(kSplit.length>1) {
                String stash="default";
                String sKey=kSplit[1];
                if(kSplit.length>2) {
                    stash=kSplit[1];
                    sKey=kSplit[2];
                }
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Processor.putVar: Tried to put pms_stash.");
                // FIXME SHARKHUNTER
                //Channels.putStash(stash, sKey,val);
            }
        }
        vars.put(key, val);
    }

    private static void clearVs(int maxV) {
        for(int j=1;j<=maxV;j++) {
            vars.remove("v"+String.valueOf(j));
            rvars.remove("v"+String.valueOf(j));
        }
    }

}
