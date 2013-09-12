package net.classicube.launcher;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

final class ClientUpdateScreen extends javax.swing.JFrame {

    public static void createAndShow() {
        ClientUpdateScreen sc = new ClientUpdateScreen();
        if (Prefs.getUpdateMode() != UpdateMode.DISABLED) {
            ClientUpdateTask.getInstance().registerUpdateScreen(sc);
            ClientUpdateTask.getInstance().execute();
            sc.setVisible(true);
        } else {
            ClientLauncher.launchClient();
        }
    }

    private ClientUpdateScreen() {
        initComponents();

        // center the form on screen (initially)
        setLocationRelativeTo(null);

        boolean auto = (Prefs.getUpdateMode() == UpdateMode.AUTOMATIC);
        if (auto) {
            this.lNotice.setText("The game will start as soon as updates are complete.");
        } else {
            this.lNotice.setText("A client update is being installed.");
            this.getRootPane().setDefaultButton(bContinue);
        }
        this.bContinue.setVisible(!auto);
        this.bViewReleaseNotes.setVisible(!auto);
        pack();
    }

    public void setStatus(ClientUpdateTask.ProgressUpdate dl) {
        System.out.println(String.format("setStatus(%d, \"%s\", \"%s\")", dl.progress, dl.fileName, dl.status));
        //LogUtil.getLogger().info("ClientUpdateScreen.setStatus");
        progress.setValue(dl.progress);
        lFileName.setText(dl.fileName);
        lStats.setText(dl.status);
    }

    public void onUpdateDone() {
        LogUtil.getLogger().info("onUpdateDone");
        try {
            // wait for updater to finish (if still running)
            ClientUpdateTask.getInstance().get();

        } catch (InterruptedException | ExecutionException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error updating", ex);
            LogUtil.die("Error while updating: " + ex);
            return;
        }

        if (Prefs.getUpdateMode() == UpdateMode.AUTOMATIC) {
            ClientLauncher.launchClient();
            dispose();
        } else {
            this.bContinue.setEnabled(true);
        }
    }

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

        lNotice.setText("The game will start as soon as update is complete.");
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
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        getContentPane().add(bViewReleaseNotes, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bContinueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bContinueActionPerformed
        ClientLauncher.launchClient();
        dispose();
    }//GEN-LAST:event_bContinueActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bContinue;
    private javax.swing.JButton bViewReleaseNotes;
    private javax.swing.JLabel lFileName;
    private javax.swing.JLabel lNotice;
    private javax.swing.JLabel lStats;
    private javax.swing.JProgressBar progress;
    // End of variables declaration//GEN-END:variables
}
