package net.classicube.launcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import javax.swing.SwingWorker;
import net.classicube.launcher.gui.ErrorScreen;

// Retrieves the client's external IP,
// to check whether server and client are located on the same machine or network.
public class GetExternalIPTask extends SwingWorker<InetAddress, Boolean> {

    private static final GetExternalIPTask instance = new GetExternalIPTask();

    public static GetExternalIPTask getInstance() {
        return instance;
    }

    public static void logAndShowError(Exception ex) {
        LogUtil.getLogger().log(Level.SEVERE, "Error checking external IP address.", ex);
        ErrorScreen.show("Error checking external IP address",
                "An error occured while trying to look up external IP address. "
                + "You may have trouble connecting to the server if it is hosted "
                + "on the same computer, or on another computer on this network.",
                ex);
    }

    // List of known plaintext external-IP-returning services
    private static final String[] ipCheckUrls = new String[]{
        "http://www.classicube.net/api/myip/", // primary
        "http://curlmyip.com",
        "http://www.fcraft.net/ipcheck.php"};

    private GetExternalIPTask() {
    }

    @Override
    protected InetAddress doInBackground() throws Exception {
        for (final String ipCheckUrl : ipCheckUrls) {
            final String ipString = HttpUtil.downloadString(ipCheckUrl);
            if (ipString != null) {
                try {
                    return InetAddress.getByName(ipString.trim());
                } catch (UnknownHostException ex) {
                    LogUtil.getLogger().log(Level.WARNING,
                            "Error parsing external IP returned from {0}: {1}",
                            new Object[]{ipCheckUrl, ex});
                }
            }
        }
        // None of the available services gave us a valid IP; give up
        LogUtil.getLogger().log(Level.SEVERE, "Unable to determine external IP");
        return null;
    }
}
