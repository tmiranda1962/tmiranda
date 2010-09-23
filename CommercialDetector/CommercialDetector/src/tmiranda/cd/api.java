/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.io.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class api {

    public static boolean AddJob(Object MediaFile) {
        if (MediaFile==null) {
            Log.getInstance().Write(Log.LOGLEVEL_WARN, "AddJob: null MediaFile.");
            return false;
        }

        if (Global.IsClient()) {
            CSC.getInstance().addStatus("queue", MediaFileAPI.GetMediaFileID(MediaFile));
            return true;
        } else {
            ComskipManager.getInstance().cleanup(MediaFile);
            return ComskipManager.getInstance().queue(MediaFile);
        }
    }

    public static boolean DeleteComskipFiles(Object MediaFile) {

        if (MediaFile==null) {
            Log.getInstance().Write(Log.LOGLEVEL_WARN, "DeleteComskipFiles: null MediaFile.");
            return false;
        }

        if (Global.IsClient()) {
            Log.getInstance().Write(Log.LOGLEVEL_VERBOSE, "DeleteComskipFiles: Ignored, Running as SageClient.");
            return false;
        }

        File MediaFileFile = MediaFileAPI.GetFileForSegment(MediaFile, 0);
        if (MediaFileFile==null) {
            Log.getInstance().Write(Log.LOGLEVEL_ERROR, "DeleteComskipFiles: null File.");
            return false;
        }

        String MediaFileName = MediaFileFile.getAbsolutePath();
        if (MediaFileName==null) {
            Log.getInstance().Write(Log.LOGLEVEL_ERROR, "DeleteComskipFiles: null MediaFileName.");
            return false;
        }

        String BaseName = MediaFileName.substring(0, MediaFileName.lastIndexOf("."));

        String[] Extensions = {".edl",".txt"};

        for (String Ext : Extensions) {
            Log.getInstance().Write(Log.LOGLEVEL_TRACE, "DeleteComskipFiles: Looking for existing file " + BaseName+Ext);
            File F = new File(BaseName+Ext);

            if (F==null) {
                Log.getInstance().Write(Log.LOGLEVEL_WARN, "DeleteComskipFiles: null File " + BaseName+Ext);
            } else {
                if (F.exists()) {
                    Log.getInstance().Write(Log.LOGLEVEL_TRACE, "DeleteComskipFiles: File exists " + BaseName+Ext);
                    if (!F.delete()) {
                        Log.getInstance().Write(Log.LOGLEVEL_WARN, "DeleteComskipFiles: Delete failed " + BaseName+Ext);
                    }
                }
            }
        }

        return true;
    }

    public static boolean IsComskipActive() {

        if (Global.IsClient()) {

            String IDString = CSC.getInstance().getStatus("processing");

            if (IDString==null || IDString.isEmpty()) {
                return false;
            }
 
            String[] items = IDString.split(",");

            if (items==null || items.length==0) {
                return false;
            }

            for (String S : items) {
                int ID = 0;

                try {ID = Integer.parseInt(S); }
                catch (NumberFormatException e) {ID=0;}

                if (ID>0) {
                    return true;
                }
            }

            return false;
        }

        int NumberRunning = ComskipManager.getInstance().getNumberRunning();
        Log.getInstance().Write(Log.LOGLEVEL_VERBOSE, "NumberRunning = " + NumberRunning);
        return NumberRunning > 0;
    }

    public static boolean IsThisMediaFileComskipping(Object MediaFile) {
        if (MediaFile==null) {
            Log.getInstance().Write(Log.LOGLEVEL_WARN, "IsThisMediaFileComskipping: null MediaFile");
            return false;
        }

        if (Global.IsClient()) {
            return CSC.getInstance().statusContains("processing", MediaFileAPI.GetMediaFileID(MediaFile));
        }

        boolean IsRunning = ComskipManager.getInstance().isMediaFileRunning(MediaFile);
        Log.getInstance().Write(Log.LOGLEVEL_VERBOSE, "IsRunning = " + IsRunning);
        return IsRunning;
    }

    public static boolean HasEdlOrTxt(Object MediaFile) {
        if (MediaFile==null) {
            Log.getInstance().Write(Log.LOGLEVEL_WARN, "HasEdlOrTxt: null MediaFile");
            return false;
        }

        // If this is running on a client, ask the ComskipManager for info.
        //if (Global.IsClient()) {
            //return ComskipManager.getInstance().hasEdlOrTxt(MediaFile);
        //}

        File F = MediaFileAPI.GetFileForSegment(MediaFile, 0);
        if (F==null) {
            Log.getInstance().Write(Log.LOGLEVEL_ERROR, "HasEdlOrTxt: null File");
            return false;
        }

        String FileName = F.getAbsolutePath();
        if (FileName==null) {
            Log.getInstance().Write(Log.LOGLEVEL_ERROR, "HasEdlOrTxt: null FileName");
            return false;
        }

        boolean HasEdlOrTxt = ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName);
        Log.getInstance().Write(Log.LOGLEVEL_VERBOSE, "HasEdlOrTxt = " + HasEdlOrTxt);
        return HasEdlOrTxt;
    }

}
