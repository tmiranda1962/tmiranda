/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.navix;

import java.io.*;
import java.net.*;

/**
 *
 * @author Default
 */
public class Test {
    public Test(String HomeURL) {

        if (HomeURL==null || HomeURL.isEmpty()) {
            System.out.println("Empty HomeURL");
            return;
        }

        BufferedReader br = read(HomeURL);

        String line = null;

        try {
            while ((line=br.readLine()) != null) {
                System.out.println("line = " + line);
            }
        } catch (IOException e) {
            System.out.println("IO Exception");
        }

    }

    public static BufferedReader read(String urlString) {

        URL url = null;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL " + urlString);
            return null;
        }

        InputStream is = null;

        try {
            is = url.openStream();
        } catch (IOException e) {
            System.out.println("IO Exception");
            return null;
        }

        InputStreamReader isr = new InputStreamReader(is);

        BufferedReader br = new BufferedReader(isr);

        return br;
    }

}
