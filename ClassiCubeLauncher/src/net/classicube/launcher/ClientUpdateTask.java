package net.classicube.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import javax.swing.SwingWorker;

public class ClientUpdateTask extends SwingWorker<Boolean, Boolean> {

    @Override
    protected Boolean doInBackground() throws Exception {
        File targetPath = LogUtil.getLauncherDir();
        File clientFile = new File(targetPath, "ClassiCubeClient.jar");
        boolean needsUpdate;

        if (clientFile.exists()) {
            // TODO: download remote hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            final byte[] buffer = new byte[8192];
            try (FileInputStream is = new FileInputStream(clientFile)) {
                DigestInputStream dis = new DigestInputStream(is, digest);
                while (dis.read(buffer) != -1) { // ignore
                }
            }
            byte[] localHash = digest.digest();
            // TODO: check if local hash equals remote hash
            needsUpdate = false;
        } else {
            needsUpdate = true;
        }

        if(needsUpdate){
            // TODO: download update
        }
        
        return needsUpdate;
    }
}