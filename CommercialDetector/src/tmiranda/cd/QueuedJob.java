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
    private int MediaFileID = -1;

    public QueuedJob(List<String> nFileNames, int ID) {
        FileNames = nFileNames;
        MediaFileID = ID;
    }

    public List<String> getFileName() {
        return FileNames;
    }

    public int getMediaFileID() {
        return MediaFileID;
    }

    public Object getMediaFile() {
        return MediaFileAPI.GetMediaFileForID(MediaFileID);
    }

    private String getChannelName() {
        Object MediaFile = this.getMediaFile();
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob.getChannelName: null MediaFile.");
            return null;
        }

        return AiringAPI.GetAiringChannelName(MediaFile);
    }
    
    private String getShowName() {
        Object MediaFile = this.getMediaFile();
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob.getShowName: null MediaFile.");
            return null;
        }
        
        String Name = MediaFileAPI.GetMediaTitle(MediaFile);
        if (Name==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob.getShowName: null Name.");
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

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getComskipIni: Looking for comskip ini.");

        String DirectoryName = ComskipManager.COMSKIP_INI_DIR;

        File Directory = new File(DirectoryName);
        if (Directory==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob.getComskipIni: Error accessing comskip directory " + DirectoryName);
            return Configuration.GetServerProperty("cd/ini_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
        }

        if (!Directory.exists()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob.getComskipIni: Comskip ini directory does not exist " + DirectoryName);
            if (!Directory.mkdirs()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob.getComskipIni: Failed to create comskip directory.");
                return Configuration.GetServerProperty("cd/ini_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
            }
        }

        // Look in comskip dir for matching ShowName.ini.
        String Name = this.getShowName();
        if (Name!=null) {
            File IniFile = new File(DirectoryName + File.separator + Name + ".ini");
            String IniFileName = (IniFile==null ? "null" : IniFile.getAbsolutePath());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getComskipIni: Looking for comskip ini file " + IniFileName);

            if (IniFile!=null && IniFile.exists()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getComskipIni: Found ini for show " + IniFileName);
                return IniFileName;
            }
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob.getComskipIni: null ShowName.");
        }

        // Look in comskip dir for matching ChannelName.ini.
        String ChannelName = this.getChannelName();
        if (ChannelName!=null) {
            File IniFile = new File(DirectoryName + File.separator + ChannelName + ".ini");
            String IniFileName = (IniFile==null ? "null" : IniFile.getAbsolutePath());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getComskipIni: Looking for comskip ini file " + IniFileName);

            if (IniFile!=null && IniFile.exists()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getComskipIni: Found ini for channel " + IniFileName);
                return IniFileName;
            }
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob.getComskipIni: null ChannelName.");
        }

        // If nothing else found use the default.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getComskipIni: No ini found, using default.");
        return Configuration.GetServerProperty("cd/ini_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
    }

    public String getShowAnalyzerProfile() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getShowAnalyzerProfile: Looking for ShowAnalyzer profile.");

        String DirectoryName = Configuration.GetServerProperty("cd/profile_location", "Select");

        if (DirectoryName.equalsIgnoreCase("select")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getShowAnalyzerProfile: No ShowAnalyzer profile directory.");
            return null;
        }

        File Directory = new File(DirectoryName);
        if (Directory==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob.getShowAnalyzerProfile: Error accessing ShowAnalyzer directory " + DirectoryName);
            return null;
        }

        if (!Directory.exists() || !Directory.isDirectory()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "QueuedJob.getShowAnalyzerProfile: ShowAnalyzer directory does not exist " + DirectoryName);
            return null;
        }

        // Look in the directory for matching ShowName.saconfig
        String Name = this.getShowName();
        if (Name!=null) {
            File ProfileFile = new File(DirectoryName + File.separator + Name + ".saconfig");

            String ProfileName = (ProfileFile==null ? "null" : ProfileFile.getAbsolutePath());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getShowAnalyzerProfile: Looking for ShowAnalyzer profile " + ProfileName);

            if (ProfileFile!=null && ProfileFile.exists()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getShowAnalyzerProfile: Found profile for show " + ProfileName);
                return ProfileName;
            }

        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob.getShowAnalyzerProfile: null ShowName.");
        }

        // Look in the directory for matching ChannelName.saconfig
        String ChannelName = this.getChannelName();
        if (ChannelName!=null) {
            File ProfileFile = new File(DirectoryName + File.separator + ChannelName + ".saconfig");

            String ProfileName = (ProfileFile==null ? "null" : ProfileFile.getAbsolutePath());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getShowAnalyzerProfile: Looking for ShowAnalyzer profile " + ProfileName);

            if (ProfileFile!=null && ProfileFile.exists()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getShowAnalyzerProfile: Found profile for channel " + ProfileName);
                return ProfileName;
            }

        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "QueuedJob.getShowAnalyzerProfile: null ChannelName.");
        }

        // If nothing else found use the default, or null.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "QueuedJob.getShowAnalyzerProfile: No profile found.");
        return null;
    }

    public String getShowTitle() {
        Object MediaFile = MediaFileAPI.GetMediaFileForID(MediaFileID);
        return (MediaFile==null ? "<Unknown>" : ShowAPI.GetShowTitle(MediaFile));
    }

    public String getShowEpisode() {
        Object MediaFile = MediaFileAPI.GetMediaFileForID(MediaFileID);
        return (MediaFile==null ? "<Unknown>" : ShowAPI.GetShowEpisode(MediaFile));
    }

    public String getShowTitleEpisode() {
        return getShowTitle() + ":" + getShowEpisode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QueuedJob other = (QueuedJob) obj;
        if (this.MediaFileID != other.MediaFileID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + this.MediaFileID;
        return hash;
    }
}
