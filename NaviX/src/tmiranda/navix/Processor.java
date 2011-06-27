
package tmiranda.navix;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author Tom Miranda.
 *
 */
public class Processor {

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
    /**
    private static boolean parseV2(String[] lines,int start,String url) throws Exception {
        return parseV2(lines,start,url,null);
    }


    private static boolean parseV2(String[] lines,int start,String url,ChannelAuth a) throws Exception {
        Pattern ifparse=Pattern.compile("^([^<>=!]+)\\s*([!<>=]+)\\s*(.*)");
        boolean if_skip=false;
        boolean if_true=false;
        int maxV=0;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Processor.parseV2.");
        vars.put("s_url", url);
        for(int i=start;i<lines.length;i++) {
            String line=lines[i];
            if(ChannelUtil.ignoreLine(line))
                continue;
line=line.trim();
Channels.debug("navix proc line "+line);
if(if_true)
if(line.startsWith("else")||line.startsWith("elseif")) {
if_skip=true;
continue;
}
// this if block was not active skip it
if(if_skip&&!line.startsWith("else")&&!line.startsWith("elseif")&&!line.startsWith("endif"))
continue;
if_skip=false;

if(line.equalsIgnoreCase("scrape")) { // scrape, fetch page...
String sUrl=vars.get((String)"s_url");
URLConnection u=null;
String action=vars.get("s_action");
Proxy p=ChannelUtil.proxy(a);
if(action!=null&&action.equalsIgnoreCase("geturl")) {
// YUCK!! this sucks, we need to get the location out of the redirect...
Channels.debug("geturl called "+sUrl);
HttpURLConnection h=(HttpURLConnection)new URL(sUrl).openConnection(p);
h.setInstanceFollowRedirects(false);
h.connect();
Channels.debug("connect return");
String hName="";
vars.put("geturl", h.getURL().toString());
Channels.debug("put "+h.getURL().toString());
for (int j=1; (hName = h.getHeaderFieldKey(j))!=null; j++) {
Channels.debug("hdr "+hName+" val "+h.getHeaderField(j));
if(hName.equalsIgnoreCase("location")) {
vars.put("v1", h.getHeaderField(j));
maxV=1;
break;
}
}
h.disconnect();
continue;
}
u=new URL(sUrl).openConnection(p);
String method=vars.get("s_method");
String sPage;
HashMap<String,String> hdr=new HashMap<String,String>();
for(String key : vars.keySet()) {
if(!key.startsWith("s_headers."))
continue;
hdr.put(key.substring(10), vars.get(key));
}
if(!ChannelUtil.empty(vars.get("s_referer"))) {
hdr.put("Referer", vars.get("s_referer"));
}
if(method!=null&&method.equalsIgnoreCase("post")) {
String q=vars.get("s_postdata");
sPage=ChannelUtil.postPage(u,(q==null?"":q),vars.get("s_cookie"),hdr);
}
else {
sPage=ChannelUtil.fetchPage(u,null,vars.get("s_cookie"),hdr);
}
if(ChannelUtil.empty(sPage)) {
Channels.debug("bad page from proc");
throw new Exception("empty scrape page");
}
vars.put("geturl", u.getURL().toString());
Channels.debug("scrape page "+sPage);
vars.put("htmRaw", sPage);
// get headers and cookies
String hName="";
for (int j=1; (hName = u.getHeaderFieldKey(j))!=null; j++) {
if (hName.equals("Set-Cookie")) {
String[] fields = u.getHeaderField(j).split(";\\s*");
String cookie=fields[0];
String[] cf=cookie.split("=",2);
String cookieName = cf[0];
String cookieValue=null;
if(cf.length>1)
cookieValue= cf[1];
vars.put("cookies."+cookieName, cookieValue);
}
else {
String data=u.getHeaderField(j);
vars.put("headers."+hName, data);
}
}
// apply regexp
Pattern re=Pattern.compile(escapeChars(vars.get("regex")));
Matcher m=re.matcher(sPage);
clearVs(maxV);
maxV=0;
if(m.find()) {
for(int j=1;j<=m.groupCount();j++) {
vars.put("v"+String.valueOf(j), m.group(j));
rvars.put("v"+String.valueOf(j), m.group(j));
}
maxV=m.groupCount();
}
continue;
}
if(line.startsWith("endif")) {
if_true=false;
continue;
}

if(line.startsWith("if ")) { // if block
String cond=line.substring(3);
Channels.debug("if "+cond+" pattern "+ifparse.pattern());
Matcher im=ifparse.matcher(cond);
String var;
String op=null;
String comp=null;
if(!im.find()) {
var=getVar(cond);
}
else {
var=getVar(im.group(1));
Channels.debug("gc "+im.groupCount()+" "+var);
if(im.groupCount()>1)
op=im.group(2);
if(im.groupCount()>2) {
String s=im.group(2);
comp=fixVar(s.trim(),getVar(s.trim()));
}
}
Channels.debug("if var "+var+" op "+op+" comp "+comp);
if(op==null) { // no operator
if(var!=null) {
if_true=true;
continue;
}
else { // skip some lines
if_skip=true;
continue;
}
}
if_true=boolOp(var,op,comp);
if(!if_true)
if_skip=true;
continue;
}

if(line.startsWith("elseif ")) {
String cond=line.substring(7);
Matcher im=ifparse.matcher(cond);
String var;
String op=null;
String comp=null;
if(!im.find()) {
var=getVar(cond);
}
else {
var=getVar(im.group(1));
Channels.debug("gc "+im.groupCount()+" "+var);
if(im.groupCount()>1)
op=im.group(2);
if(im.groupCount()>2) {
String s=im.group(2);
comp=fixVar(s.trim(),getVar(s.trim()));
}
}
if(op==null) { // no operator
if(var!=null) {
if_true=true;
continue;
}
else { // skip some lines
if_skip=true;
continue;
}
}
if_true=boolOp(var,op,comp);
if(!if_true)
if_skip=true;
continue;
}
if(line.startsWith("else ")) {
if_true=true;
continue;
}

if(line.startsWith("error ")) {
Channels.debug("Error "+line.substring(6));
throw new Exception("NIPL error");
}


if(line.startsWith("concat ")) {
String[] ops=line.substring(7).split(" ",2);
String res=ChannelUtil.append(getVar(ops[0].trim()),"",
fixVar(ops[1],getVar(ops[1])));
putVar(ops[0].trim(), res);
Channels.debug("concat "+ops[0]+" res "+res);
continue;
}

if(line.startsWith("match ")) {
String var=line.substring(6).trim();
Pattern re=Pattern.compile(escapeChars(vars.get("regex")));
Matcher m=re.matcher(getVar(var));
if(!m.find()) {
Channels.debug("no match "+re.pattern());
vars.put("nomatch","1");
}
else {
Channels.debug("match "+m.groupCount());
for(int j=1;j<=m.groupCount();j++)
vars.put("v"+String.valueOf(j), m.group(j));
maxV=m.groupCount();
}
continue;
}

if(line.startsWith("replace ")) {
String[] ops=line.substring(8).split(" ",2);
Pattern re=Pattern.compile(vars.get("regex"));
Matcher m=re.matcher(getVar(ops[0]));
String res=m.replaceAll(fixVar(ops[1],getVar(ops[1])));
putVar(ops[0], res);
continue;
}

if(line.startsWith("unescape ")) {
String var=line.substring(9).trim();
String res=ChannelUtil.unescape(getVar(var));
putVar(var, res);
continue;
}

if(line.startsWith("escape ")) {
String var=line.substring(7).trim();
String res=ChannelUtil.escape(getVar(var));
putVar(var, res);
continue;
}

String[] vLine=line.split("=",2);
if(vLine.length==2) { // variable
String key=vLine[0].trim();
String val=vLine[1].trim();
if(key.startsWith("report_val ")) {
key=key.substring(11);
rvars.put(key, fixVar(val,vars.get(val)));
Channels.debug("rvar ass "+key+"="+fixVar(val,vars.get(val)));
}
else {
String realVal=fixVar(val,vars.get(val));
if(key.startsWith("nookies.")) {
nookies.put(key.substring(8), realVal);
ChannelNaviXNookie.store(key.substring(8),realVal,vars.get("nookie_expires"));
}
else if(key.startsWith("pms_stash.")) {
// special PMS stash
String[] kSplit=key.split(".",3);
if(kSplit.length>1) {
String stash="default";
String sKey=kSplit[1];
if(kSplit.length>2) {
stash=kSplit[1];
sKey=kSplit[2];
}
Channels.putStash(stash, sKey, realVal);
}
}
vars.put(key, realVal);
Channels.debug("var ass "+key+"="+realVal);
}
continue;
}

//////////////////////////////////////
// These ones are channel specific
//////////////////////////////////////

if(line.startsWith("prepend ")) {
String[] ops=line.substring(8).split(" ",2);
String res=ChannelUtil.append(fixVar(ops[1],getVar(ops[1])),"",
getVar(ops[0].trim()));
putVar(ops[0].trim(), res);
Channels.debug("prepend "+ops[0]+" res "+res);
continue;
}

if(line.startsWith("call ")) {
String nScript=line.substring(5).trim();
ArrayList<String> s=Channels.getScript(nScript);
if(s==null) {
Channels.debug("Calling unknown script "+nScript);
continue;
}
String arg=vars.get("url");
if(ChannelUtil.empty(arg))
arg=url;
String[] arr=s.toArray(new String[s.size()]);
Channels.debug("call script "+nScript+" arg "+arg);
parseV2(arr,0,arg);
continue;
}

//////////////////////
// Exit form here
//////////////////////

if(line.startsWith("report")) {
Channels.debug("report found take another spin");
return true;
}

if(line.trim().equals("play"))
return false;

}
// This is weird no play statement?? throw error
Channels.debug("no play found");
throw new Exception("NIPL error no play");
}
     * */
