/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class SageFileName {
    String FileName = null;

    public SageFileName(String N) {
        FileName = (N==null ? "ERROR" : N);
    }

    public String getExtension() {
        String[] NameExt = FileName.split("\\.");

        String MediaFileExt = null;

        if (NameExt.length == 2) {
            // Most common case: "filename.ext"
            MediaFileExt = NameExt[1];
        } else {
            MediaFileExt = FileName.substring(FileName.lastIndexOf(".")+1);
        }

        return MediaFileExt;
    }
}
