/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SageFileName other = (SageFileName) obj;
        if ((this.FileName == null) ? (other.FileName != null) : !this.FileName.equals(other.FileName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.FileName != null ? this.FileName.hashCode() : 0);
        return hash;
    }
    
}
