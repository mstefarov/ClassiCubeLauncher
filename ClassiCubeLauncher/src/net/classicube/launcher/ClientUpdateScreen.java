package net.classicube.launcher;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class ClientUpdateScreen extends javax.swing.JFrame {

    public ClientUpdateScreen() {
        initComponents();
        
        // center the form on screen (initially)
        setLocationRelativeTo(null);
    }

    public void setStatus(ClientUpdateStatus dl) {
        System.out.println(String.format("setStatus(%d, \"%s\", \"%s\")", dl.progress, dl.fileName, dl.status));
        //LogUtil.getLogger().info("ClientUpdateScreen.setStatus");
        progress.setValue(dl.progress);
        this.lFileName.setText(dl.fileName);
        this.lStats.setText(dl.status);
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

        ClientLauncher.launchClient();
        setVisible(false);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        progress = new javax.swing.JProgressBar();
        lFileName = new javax.swing.JLabel();
        lStats = new javax.swing.JLabel();
        lNotice = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        bContinue = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWidths = new int[] {0, 5, 0};
        layout.rowHeights = new int[] {0, 5, 0, 5, 0, 5, 0, 5, 0};
        getContentPane().setLayout(layout);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(progress, gridBagConstraints);

        lFileName.setText("<fileName>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        getContentPane().add(lFileName, gridBagConstraints);

        lStats.setText("<status>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        getContentPane().add(lStats, gridBagConstraints);

        lNotice.setText("The game will start as soon as update is complete.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        getContentPane().add(lNotice, gridBagConstraints);

        jCheckBox1.setText("Do not notify me about updates");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        getContentPane().add(jCheckBox1, gridBagConstraints);

        bContinue.setText("Continue >");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bContinue, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bContinue;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel lFileName;
    private javax.swing.JLabel lNotice;
    private javax.swing.JLabel lStats;
    private javax.swing.JProgressBar progress;
    // End of variables declaration//GEN-END:variables

    void registerWithUpdateTask() {
        ClientUpdateTask.getInstance().registerUpdateScreen(this);
    }
}
