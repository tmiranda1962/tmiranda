/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.io.*;
import java.util.*;

import sagex.api.*;

/**
 *
 * @author Tom Miranda
 * <p>
 * This represents a job that is queued.  Each MediaFile may contain multiple segments.
 */
public class QueuedJob implements Serializable {

    private List<String> FileNames = null;
    private boolean JobHasFailed = false;
    private int FailingSegment = -1;
    private int MediaFileID = -1;

    public QueuedJob(List<String> nFileNames, int ID) {
        FileNames = nFileNames;
        JobHasFailed = false;
        FailingSegment = -1;
        MediaFileID = ID;
    }

    public List<String> getFileName() {
        return FileNames;
    }

    public boolean getJobHasFailed() {
        return JobHasFailed;
    }

    public int getFailingSegment() {
        return FailingSegment;
    }

    public int getMediaFileID() {
        return MediaFileID;
    }

    public void setJobHasFailed(boolean status) {
        JobHasFailed = status;
    }

    public void setFailingSegment(int segment) {
        FailingSegment = segment;
    }

    public Object getMediaFile() {
        return MediaFileAPI.GetMediaFileForID(MediaFileID);
    }

    public String getChannelName() {
        Object MediaFile = this.getMediaFile();
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob: null MediaFile.");
            return null;
        }

        return AiringAPI.GetAiringChannelName(MediaFile);
    }
    
    public String getShowName() {
        Object MediaFile = this.getMediaFile();
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob: null MediaFile.");
            return null;
        }
        
        String Name = MediaFileAPI.GetMediaTitle(MediaFile);
        if (Name==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob: null Name.");
            return null;
        }

        return Name.replaceAll(" ", "");
    }

    public String getProgramToUse() {
        String ChannelName = this.getChannelName();
        return Configuration.GetServerProperty("cd/map_"+ChannelName, "Default");
    }

    // Look for ini file:
    // - In comskip dir with show name.
    // - In comskip dir with channel name.
    // - In default location.
    public String getComskipIni() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Looking for comskip ini.");

        String DirectoryName = ComskipManager.COMSKIP_INI_DIR;

        File Directory = new File(DirectoryName);
        if (Directory==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob: Error accessing comskip directory " + DirectoryName);
            return Configuration.GetServerProperty("cd/ini_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
        }

        if (!Directory.exists()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob: Comskip ini directory does not exist " + DirectoryName);
            if (!Directory.mkdirs()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob: Failed to create comskip directory.");
                return Configuration.GetServerProperty("cd/ini_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
            }
        }

        // Look in comskip dir for matching ShowName.ini.
        String Name = this.getShowName();
        if (Name!=null) {
            File IniFile = new File(DirectoryName + File.separator + Name + ".ini");
            String IniFileName = (IniFile==null ? "null" : IniFile.getAbsolutePath());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Looking for comskip ini file " + IniFileName);

            if (IniFile!=null && IniFile.exists()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Found ini for show " + IniFileName);
                return IniFileName;
            }
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob: null ShowName.");   
        }

        // Look in comskip dir for matching ChannelName.ini.
        String ChannelName = this.getChannelName();
        if (ChannelName!=null) {
            File IniFile = new File(DirectoryName + File.separator + ChannelName + ".ini");
            String IniFileName = (IniFile==null ? "null" : IniFile.getAbsolutePath());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Looking for comskip ini file " + IniFileName);

            if (IniFile!=null && IniFile.exists()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Found ini for channel " + IniFileName);
                return IniFileName;
            }
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob: null ChannelName.");
        }

        // If nothing else found use the default.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: No ini found, using default.");
        return Configuration.GetServerProperty("cd/ini_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
    }

    public String getShowAnalyzerProfile() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Looking for ShowAnalyzer profile.");

        String DirectoryName = Configuration.GetServerProperty("cd/profile_location", "Select");

        if (DirectoryName.equalsIgnoreCase("select")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: No ShowAnalyzer profile directory.");
            return null;
        }

        File Directory = new File(DirectoryName);
        if (Directory==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob: Error accessing ShowAnalyzer directory " + DirectoryName);
            return null;
        }

        if (!Directory.exists() || !Directory.isDirectory()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob: ShowAnalyzer directory does not exist " + DirectoryName);
            return null;
        }

        // Look in the directory for matching ShowName.saconfig
        String Name = this.getShowName();
        if (Name!=null) {
            File ProfileFile = new File(DirectoryName + File.separator + Name + ".saconfig");

            String ProfileName = (ProfileFile==null ? "null" : ProfileFile.getAbsolutePath());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Looking for ShowAnalyzer profile " + ProfileName);

            if (ProfileFile!=null && ProfileFile.exists()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Found profile for show " + ProfileName);
                return ProfileName;
            }

        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob: null ShowName.");
        }

        // Look in the directory for matching ChannelName.saconfig
        String ChannelName = this.getChannelName();
        if (ChannelName!=null) {
            File ProfileFile = new File(DirectoryName + File.separator + ChannelName + ".saconfig");

            String ProfileName = (ProfileFile==null ? "null" : ProfileFile.getAbsolutePath());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Looking for ShowAnalyzer profile " + ProfileName);

            if (ProfileFile!=null && ProfileFile.exists()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: Found profile for channel " + ProfileName);
                return ProfileName;
            }

        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob: null ChannelName.");
        }

        // If nothing else found use the default, or null.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob: No profile found.");
        return null;
    }
}
