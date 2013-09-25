package net.classicube.launcher.gui;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.JRootPane;
import javax.swing.border.EmptyBorder;
import net.classicube.launcher.ClientLauncher;
import net.classicube.launcher.ClientUpdateTask;
import net.classicube.launcher.LogUtil;
import net.classicube.launcher.Prefs;
import net.classicube.launcher.ServerJoinInfo;
import net.classicube.launcher.UpdateMode;

public final class ClientUpdateScreen extends javax.swing.JFrame {
    // =============================================================================================
    //                                                                            FIELDS & CONSTANTS
    // =============================================================================================

    private static final String RELEASE_NOTES_URL =
            "https://github.com/andrewphorn/ClassiCube-Client/commits/master";
    private Desktop desktop;
    private final ServerJoinInfo joinInfo;

    // =============================================================================================
    //                                                                                INITIALIZATION
    // =============================================================================================
    public static void createAndShow(final ServerJoinInfo joinInfo) {
        if (joinInfo == null) {
            throw new NullPointerException("info");
        }
        if (Prefs.getUpdateMode() != UpdateMode.DISABLED) {
            ClientUpdateScreen sc = new ClientUpdateScreen(joinInfo);
            sc.setVisible(true);
            ClientUpdateTask.getInstance().registerUpdateScreen(sc);
        } else {
            ClientLauncher.launchClient(joinInfo);
        }
    }

    private ClientUpdateScreen(final ServerJoinInfo joinInfo) {
        this.joinInfo = joinInfo;
        final JRootPane root = getRootPane();
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        initComponents();

        // center the form on screen (initially)
        setLocationRelativeTo(null);

        // tweak the UI for auto/notify preference
        final boolean auto = (Prefs.getUpdateMode() == UpdateMode.AUTOMATIC);
        if (auto) {
            lNotice.setText("The game will start as soon as updates are complete.");
        } else {
            lNotice.setText("A client update is being installed.");
            root.setDefaultButton(bPlay);
            this.desktop = (Desktop.isDesktopSupported() ? Desktop.getDesktop() : null);
            if (this.desktop != null && !this.desktop.isSupported(Desktop.Action.BROWSE)) {
                this.desktop = null;
            }
        }
        bPlay.setVisible(!auto);
        bViewChanges.setVisible(!auto);
        pack();
    }

    // =============================================================================================
    //                                                                                      UPDATING
    // =============================================================================================
    public void setStatus(final ClientUpdateTask.ProgressUpdate dl) {
        if (dl.progress < 0) {
            this.progress.setIndeterminate(true);
        } else {
            this.progress.setIndeterminate(false);
            this.progress.setValue(dl.progress);
        }
        this.lFileName.setText(dl.fileName);
        this.lStats.setText(dl.status);
    }

    public void onUpdateDone(final boolean updatesApplied) {
        LogUtil.getLogger().info("onUpdateDone");
        try {
            // wait for updater to finish (if still running)
            ClientUpdateTask.getInstance().get();

        } catch (final InterruptedException | ExecutionException ex) {
            ErrorScreen.show(this, "Error updating", ex.getMessage(), ex);
            System.exit(3);
            return;
        }

        if (!updatesApplied || Prefs.getUpdateMode() == UpdateMode.AUTOMATIC) {
            dispose();
            ClientLauncher.launchClient(this.joinInfo);
        } else {
            this.lNotice.setText(" ");
            this.bPlay.setEnabled(true);
        }
    }

    // =============================================================================================
    //                                                                           GUI EVENT LISTENERS
    // =============================================================================================
    private void bPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bPlayActionPerformed
        dispose();
        ClientLauncher.launchClient(this.joinInfo);
    }//GEN-LAST:event_bPlayActionPerformed

    private void bViewChangesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bViewChangesActionPerformed
        if (this.desktop != null) {
            try {
                this.desktop.browse(new URI(RELEASE_NOTES_URL));
            } catch (final Exception ex) {
                LogUtil.getLogger().log(Level.WARNING, "Error opening release notes URL", ex);
                showReleaseNotesUrl();
            }
        } else {
            showReleaseNotesUrl();
        }
    }//GEN-LAST:event_bViewChangesActionPerformed

    void showReleaseNotesUrl() {
        PromptScreen.show(this, "Release notes link",
                "Here you can find a list of changes in this game update.", RELEASE_NOTES_URL);
    }

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

        lFileName = new javax.swing.JLabel();
        lStats = new javax.swing.JLabel();
        progress = new javax.swing.JProgressBar();
        lNotice = new javax.swing.JLabel();
        bViewChanges = new net.classicube.launcher.gui.JNiceLookingButton();
        bPlay = new net.classicube.launcher.gui.JNiceLookingButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Game Update");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        lFileName.setForeground(new java.awt.Color(255, 255, 255));
        lFileName.setText("Preparing to update...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        getContentPane().add(lFileName, gridBagConstraints);

        lStats.setForeground(new java.awt.Color(255, 255, 255));
        lStats.setText("...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        getContentPane().add(lStats, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(progress, gridBagConstraints);

        lNotice.setForeground(new java.awt.Color(255, 255, 255));
        lNotice.setText("<notice>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(lNotice, gridBagConstraints);

        bViewChanges.setText("View Changes");
        bViewChanges.setToolTipText("Check out list of changes included in this update (opens a web browser).");
        bViewChanges.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bViewChangesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 16);
        getContentPane().add(bViewChanges, gridBagConstraints);

        bPlay.setText("Play >");
        bPlay.setEnabled(false);
        bPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bPlayActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bPlay, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private net.classicube.launcher.gui.JNiceLookingButton bPlay;
    private net.classicube.launcher.gui.JNiceLookingButton bViewChanges;
    private javax.swing.JLabel lFileName;
    private javax.swing.JLabel lNotice;
    private javax.swing.JLabel lStats;
    private javax.swing.JProgressBar progress;
    // End of variables declaration//GEN-END:variables
}
