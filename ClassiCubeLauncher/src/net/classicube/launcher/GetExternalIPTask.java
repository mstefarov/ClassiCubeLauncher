package net.classicube.launcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import javax.swing.SwingWorker;

// Retrieves the client's external IP,
// to check whether server and client are located on the same machine or network.
public class GetExternalIPTask extends SwingWorker<InetAddress, Boolean> {

    // List of known plaintext external-IP-returning services
    private static final String[] ipCheckUrls = new String[]{
        "http://www.classicube.net/api/myip/", // primary
        "http://curlmyip.com",
        "http://www.fcraft.net/ipcheck.php"};

    @Override
    protected InetAddress doInBackground() throws Exception {
        for (final String ipCheckUrl : ipCheckUrls) {
            final String ipString = HttpUtil.downloadString(ipCheckUrl);
            if (ipString != null) {
                try {
                    return InetAddress.getByName(ipString);
                } catch (UnknownHostException ex) {
                    LogUtil.getLogger().log(Level.FINE,
                            "Error parsing external IP returned from {0}: {1}",
                            new Object[]{ipCheckUrl, ex});
                }
            }
        }
        // None of the available services gave us a valid IP; give up
        return null;
    }
}
