
package tmiranda.aar;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class Mode {

    private List<String> modes;

    public Mode() {

        modes = new ArrayList<String>();

        modes.addAll(Arrays.asList(Configuration.GetAspectRatioModes()));

        String extender = Utility.LocalizeString("HD_Media_Extender");
        String player = Utility.LocalizeString("HD_Media_Player");
        String remoteUIType = Global.GetRemoteUIType();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Mode: " + remoteUIType + ":" + extender + ":" + player);

        String canEdit = null;

        if (remoteUIType.equals(player) || remoteUIType.equals(extender)) {
            canEdit = Configuration.GetProperty("aspect_ratios/can_edit_modes", "true");
        } else {
            canEdit = Configuration.GetProperty("aspect_ratios/force_can_edit", "false");
        }

        if (canEdit.equalsIgnoreCase("true")) {
            String customARPropertyVal = Configuration.GetProperty("advanced_aspect_ratio_extra_modes", null);

            if (customARPropertyVal==null || customARPropertyVal.isEmpty())
                return;
        }
    }

    public String getCurrentMode() {
        return Configuration.GetAspectRatioMode();
    }

    public List<String> getAllModes() {
        return modes;
    }
}
