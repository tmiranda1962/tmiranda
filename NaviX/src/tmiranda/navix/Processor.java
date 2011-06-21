
package tmiranda.navix;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author Tom Miranda.
 *
 */
public class Processor {

    private String UrlString;
    private URL url;

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
}
