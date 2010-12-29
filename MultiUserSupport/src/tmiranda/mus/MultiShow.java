/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

/**
 *
 * @author Tom Miranda.
 */
public class MultiShow extends MediaFileControl {
    
    private String userID = null;

    public MultiShow(String UserID, Object Show) {
        super(Show);
        userID = (UserID==null ? "null" : UserID);
    }

}
