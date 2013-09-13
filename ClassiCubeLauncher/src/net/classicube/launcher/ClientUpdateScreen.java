package net.classicube.launcher;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.JOptionPane;

final class ClientUpdateScreen extends javax.swing.JFrame {
    // =============================================================================================
    //                                                                            FIELDS & CONSTANTS
    // =============================================================================================
    private static final String ReleaseNotesUrl = "https://github.com/andrewphorn/ClassiCube-Client/commits/master";

    private Desktop desktop;
    private ServerJoinInfo joinInfo;

    // =============================================================================================
    //                                                                                INITIALIZATION
    // =============================================================================================
    public static void createAndShow(ServerJoinInfo joinInfo) {
        if (Prefs.getUpdateMode() != UpdateMode.DISABLED) {
            ClientUpdateScreen sc = new ClientUpdateScreen(joinInfo);
            sc.setVisible(true);
            ClientUpdateTask.getInstance().registerUpdateScreen(sc);
        } else {
            ClientLauncher.launchClient(joinInfo);
        }
    }

    private ClientUpdateScreen(ServerJoinInfo joinInfo) {
        this.joinInfo = joinInfo;
        initComponents();

        // center the form on screen (initially)
        setLocationRelativeTo(null);

        // tweak the UI for auto/notify preference
        boolean auto = (Prefs.getUpdateMode() == UpdateMode.AUTOMATIC);
        if (auto) {
            lNotice.setText("The game will start as soon as updates are complete.");
        } else {
            lNotice.setText("A client update is being installed.");
            getRootPane().setDefaultButton(bContinue);
            desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && !desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop = null;
            }
        }
        bContinue.setVisible(!auto);
        bViewReleaseNotes.setVisible(!auto);
        pack();
    }

    // =============================================================================================
    //                                                                                      UPDATING
    // =============================================================================================
    public void setStatus(ClientUpdateTask.ProgressUpdate dl) {
        System.out.println(String.format("setStatus(%d, \"%s\", \"%s\")", dl.progress, dl.fileName, dl.status));
        //LogUtil.getLogger().info("ClientUpdateScreen.setStatus");
        progress.setValue(dl.progress);
        lFileName.setText(dl.fileName);
        lStats.setText(dl.status);
    }

    public void onUpdateDone(boolean updatesApplied) {
        LogUtil.getLogger().info("onUpdateDone");
        try {
            // wait for updater to finish (if still running)
            ClientUpdateTask.getInstance().get();

        } catch (InterruptedException | ExecutionException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error updating", ex);
            LogUtil.die("Error while updating: " + ex);
            return;
        }

        if (!updatesApplied || Prefs.getUpdateMode() == UpdateMode.AUTOMATIC) {
            ClientLauncher.launchClient(joinInfo);
            dispose();
        } else {
            lNotice.setText(" ");
            bContinue.setEnabled(true);
        }
    }

    // =============================================================================================
    //                                                                           GUI EVENT LISTENERS
    // =============================================================================================
    private void bContinueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bContinueActionPerformed
        ClientLauncher.launchClient(joinInfo);
        dispose();
    }//GEN-LAST:event_bContinueActionPerformed

    private void bViewReleaseNotesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bViewReleaseNotesActionPerformed
        if (desktop != null) {
            try {
                desktop.browse(new URI(ReleaseNotesUrl));
            } catch (URISyntaxException | IOException ex) {
                LogUtil.getLogger().log(Level.WARNING, "Error opening release notes URL", ex);
                JOptionPane.showInputDialog(this, "Release notes URL", ReleaseNotesUrl);
            }
        }
    }//GEN-LAST:event_bViewReleaseNotesActionPerformed

    // =============================================================================================
    //                                                                            GENERATED GUI CODE
    // =============================================================================================
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        progress = new javax.swing.JProgressBar();
        lFileName = new javax.swing.JLabel();
        lStats = new javax.swing.JLabel();
        lNotice = new javax.swing.JLabel();
        bContinue = new javax.swing.JButton();
        bViewReleaseNotes = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(progress, gridBagConstraints);

        lFileName.setText("<fileName>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        getContentPane().add(lFileName, gridBagConstraints);

        lStats.setText("<status>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        getContentPane().add(lStats, gridBagConstraints);

        lNotice.setText("<notice>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(lNotice, gridBagConstraints);

        bContinue.setText("Play >");
        bContinue.setEnabled(false);
        bContinue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bContinueActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bContinue, gridBagConstraints);

        bViewReleaseNotes.setText("View Release Notes");
        bViewReleaseNotes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bViewReleaseNotesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 16);
        getContentPane().add(bViewReleaseNotes, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bContinue;
    private javax.swing.JButton bViewReleaseNotes;
    private javax.swing.JLabel lFileName;
    private javax.swing.JLabel lNotice;
    private javax.swing.JLabel lStats;
    private javax.swing.JProgressBar progress;
    // End of variables declaration//GEN-END:variables
}
