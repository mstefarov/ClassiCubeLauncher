package net.classicube.launcher.gui;

import java.awt.Color;
import java.awt.event.ItemEvent;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.border.EmptyBorder;
import net.classicube.launcher.AccountManager;
import net.classicube.launcher.GameServiceType;
import net.classicube.launcher.Prefs;
import net.classicube.launcher.SessionManager;
import net.classicube.launcher.UpdateMode;

final class PreferencesScreen extends javax.swing.JDialog {
    // =============================================================================================
    //                                                                                INITIALIZATION
    // =============================================================================================

    public PreferencesScreen(final JFrame parent) {
        super(parent, "Preferences", true);
        final JRootPane root = getRootPane();
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        initComponents();

        root.setDefaultButton(bSave);

        // tweak BG colors
        root.setBackground(new Color(247, 247, 247));
        getContentPane().setBackground(new Color(247, 247, 247));

        // match save and cancel buttons' sizes
        bSave.setPreferredSize(bCancel.getSize());

        // fix for ugly spinner border
        nMemory.getEditor().setOpaque(false);

        pack();
        setLocationRelativeTo(parent);

        loadPreferences();
        checkIfForgetButtonsShouldBeEnabled();
    }

    void checkIfForgetButtonsShouldBeEnabled() {
        AccountManager curManager = SessionManager.getAccountManager();
        AccountManager otherManager;
        if (SessionManager.getSession().getServiceType() == GameServiceType.ClassiCubeNetService) {
            otherManager = new AccountManager(GameServiceType.MinecraftNetService);
        } else {
            otherManager = new AccountManager(GameServiceType.ClassiCubeNetService);
        }
        boolean hasUsers = curManager.hasAccounts() || otherManager.hasAccounts();
        boolean hasPasswords = hasUsers && (curManager.hasPasswords() || otherManager.hasPasswords());
        boolean hasResume = SessionManager.hasAnyResumeInfo();

        this.bForgetUsers.setEnabled(hasUsers);
        this.bForgetPasswords.setEnabled(hasPasswords);
        this.bForgetServer.setEnabled(hasResume);
    }

    // =============================================================================================
    //                                                                         LOADING/STORING PREFS
    // =============================================================================================
    private void loadPreferences() {
        xFullscreen.setSelected(Prefs.getFullscreen());
        loadUpdateMode(Prefs.getUpdateMode());
        xRememberPasswords.setSelected(Prefs.getRememberPasswords());
        xRememberUsers.setSelected(Prefs.getRememberUsers()); // should be loaded AFTER password
        xRememberServer.setSelected(Prefs.getRememberServer());
        tJavaArgs.setText(Prefs.getJavaArgs());
        nMemory.setValue(Prefs.getMaxMemory());
        xDebugMode.setSelected(Prefs.getDebugMode());
    }

    private void loadUpdateMode(final UpdateMode val) {
        final JRadioButton btn;
        switch (val) {
            case DISABLED:
                btn = rUpdateDisabled;
                break;
            case AUTOMATIC:
                btn = rUpdateAutomatic;
                break;
            default: // NOTIFY
                btn = rUpdateNotify;
                break;
        }
        rgUpdateMode.setSelected(btn.getModel(), true);
    }

    private void loadDefaults() {
        xFullscreen.setSelected(Prefs.FullscreenDefault);
        loadUpdateMode(Prefs.UpdateModeDefault);
        xRememberUsers.setSelected(Prefs.RememberUsersDefault);
        xRememberPasswords.setSelected(Prefs.RememberPasswordsDefault);
        xRememberServer.setSelected(Prefs.RememberServerDefault);
        tJavaArgs.setText(Prefs.JavaArgsDefault);
        nMemory.setValue(Prefs.MaxMemoryDefault);
        xDebugMode.setSelected(Prefs.DebugModeDefault);
    }

    private void storePreferences() {
        Prefs.setFullscreen(xFullscreen.isSelected());
        Prefs.setUpdateMode(storeUpdateMode());
        Prefs.setRememberUsers(xRememberUsers.isSelected());
        Prefs.setRememberPasswords(xRememberPasswords.isSelected());
        Prefs.setRememberServer(xRememberServer.isSelected());
        Prefs.setJavaArgs(tJavaArgs.getText());
        Prefs.setMaxMemory((int) nMemory.getValue());
        Prefs.setDebugMode(xDebugMode.isSelected());
    }

    private UpdateMode storeUpdateMode() {
        final UpdateMode val;
        if (rUpdateDisabled.isSelected()) {
            val = UpdateMode.DISABLED;
        } else if (rUpdateAutomatic.isSelected()) {
            val = UpdateMode.AUTOMATIC;
        } else {
            val = UpdateMode.NOTIFY;
        }
        return val;
    }

    // =============================================================================================
    //                                                                                    FORGETTING
    // =============================================================================================
    private void bForgetUsersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bForgetUsersActionPerformed
        if (ConfirmScreen.show(this, "Warning", "Really erase all stored user information?")) {
            SessionManager.getAccountManager().clear();
            checkIfForgetButtonsShouldBeEnabled();
        }
    }//GEN-LAST:event_bForgetUsersActionPerformed

    private void bForgetPasswordsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bForgetPasswordsActionPerformed
        if (ConfirmScreen.show(this, "Warning", "Really erase all stored user passwords?")) {
            SessionManager.getAccountManager().clearPasswords();
            checkIfForgetButtonsShouldBeEnabled();
        }
    }//GEN-LAST:event_bForgetPasswordsActionPerformed

    private void bForgetServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bForgetServerActionPerformed
        if (ConfirmScreen.show(this, "Warning", "Really erase stored information about the last-joined server?")) {
            SessionManager.clearAllResumeInfo();
            checkIfForgetButtonsShouldBeEnabled();
        }
    }//GEN-LAST:event_bForgetServerActionPerformed

    // =============================================================================================
    //                                                                           GUI EVENT LISTENERS
    // =============================================================================================
    private void bCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bCancelActionPerformed
        dispose();
    }//GEN-LAST:event_bCancelActionPerformed

    private void bDefaultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bDefaultsActionPerformed
        loadDefaults();
    }//GEN-LAST:event_bDefaultsActionPerformed

    private void bSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSaveActionPerformed
        storePreferences();
        if (!this.xRememberUsers.isSelected()) {
            SessionManager.getAccountManager().clear();
        }
        if (!this.xRememberPasswords.isSelected()) {
            SessionManager.getAccountManager().clearPasswords();
        }
        if (!this.xRememberServer.isSelected()) {
            SessionManager.clearAllResumeInfo();
        }
        dispose();
    }//GEN-LAST:event_bSaveActionPerformed

    private void xRememberUsersItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_xRememberUsersItemStateChanged
        if (evt.getStateChange() == ItemEvent.DESELECTED) {
            this.xRememberPasswords.setEnabled(false);
            this.xRememberPasswords.setSelected(false);
            this.bForgetPasswords.setEnabled(false);
            this.bForgetUsers.setEnabled(false);
        } else {
            this.xRememberPasswords.setEnabled(true);
            this.bForgetUsers.setEnabled(true);
            this.bForgetPasswords.setEnabled(xRememberPasswords.isSelected());
        }
    }//GEN-LAST:event_xRememberUsersItemStateChanged

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

        rgUpdateMode = new javax.swing.ButtonGroup();
        xFullscreen = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        javax.swing.JLabel lUpdateMode = new javax.swing.JLabel();
        rUpdateDisabled = new javax.swing.JRadioButton();
        rUpdateNotify = new javax.swing.JRadioButton();
        rUpdateAutomatic = new javax.swing.JRadioButton();
        javax.swing.JSeparator jSeparator2 = new javax.swing.JSeparator();
        xRememberUsers = new javax.swing.JCheckBox();
        bForgetUsers = new net.classicube.launcher.gui.JNiceLookingButton();
        xRememberPasswords = new javax.swing.JCheckBox();
        bForgetPasswords = new net.classicube.launcher.gui.JNiceLookingButton();
        xRememberServer = new javax.swing.JCheckBox();
        bForgetServer = new net.classicube.launcher.gui.JNiceLookingButton();
        javax.swing.JSeparator jSeparator3 = new javax.swing.JSeparator();
        javax.swing.JLabel lParameters = new javax.swing.JLabel();
        tJavaArgs = new javax.swing.JTextField();
        javax.swing.JLabel lMemory = new javax.swing.JLabel();
        nMemory = new javax.swing.JSpinner();
        javax.swing.JSeparator jSeparator4 = new javax.swing.JSeparator();
        bDefaults = new net.classicube.launcher.gui.JNiceLookingButton();
        bSave = new net.classicube.launcher.gui.JNiceLookingButton();
        bCancel = new net.classicube.launcher.gui.JNiceLookingButton();
        javax.swing.Box.Filler filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        xDebugMode = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        xFullscreen.setText("Start the game in fullscreen");
        xFullscreen.setToolTipText("<html>Choose whether ClassiCube games should start in fullscreen mode.<br>\nYou can also toggle fullscreen mode in-game by pressing <b>F11</b>.<br>\nDefault is OFF.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(xFullscreen, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(jSeparator1, gridBagConstraints);

        lUpdateMode.setText("Install game updates...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        getContentPane().add(lUpdateMode, gridBagConstraints);

        rgUpdateMode.add(rUpdateDisabled);
        rUpdateDisabled.setText("Disable");
        rUpdateDisabled.setToolTipText("<html><b>Disable</b>: No game updates will ever be downloaded or installed.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        getContentPane().add(rUpdateDisabled, gridBagConstraints);

        rgUpdateMode.add(rUpdateNotify);
        rUpdateNotify.setText("Enable (notify me)");
        rUpdateNotify.setToolTipText("<html><b>Enable (notify me)</b>: Game updates will be downloaded and installed.<br>\nYou will be notified when that happens, and you'll have an option to review changes in the latest update.<br>\nThis is the default option.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        getContentPane().add(rUpdateNotify, gridBagConstraints);

        rgUpdateMode.add(rUpdateAutomatic);
        rUpdateAutomatic.setText("Enable (automatic)");
        rUpdateAutomatic.setToolTipText("<html><b>Enable (automatic)</b>: Game updates will be installed automatically and silently.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        getContentPane().add(rUpdateAutomatic, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(jSeparator2, gridBagConstraints);

        xRememberUsers.setText("Remember usernames");
        xRememberUsers.setToolTipText("<html>Choose whether the launcher should remember usernames of players who sign in.<br>\nWhen enabled (default), most-recently-used name is filled in when the launcher starts,<br>\nand names of other accounts are available from a drop-down menu. When disabled,<br>\nyou will have to re-enter both username and password every time you sign in.");
        xRememberUsers.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                xRememberUsersItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(xRememberUsers, gridBagConstraints);

        bForgetUsers.setText("Forget all users");
        bForgetUsers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bForgetUsersActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(bForgetUsers, gridBagConstraints);

        xRememberPasswords.setText("Remember passwords");
        xRememberPasswords.setToolTipText("<html>Choose whether the launcher should remember passwords of players who sign in.<br>\nWhen enabled, selecting a previously-used username will fill in the password field.<br>\nWhen disabled (default), you will have to re-enter the password every time you sign in.<br>\nNote that entered passwords are stored on your PC in plain text.");
        xRememberPasswords.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                xRememberPasswordsItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(xRememberPasswords, gridBagConstraints);

        bForgetPasswords.setText("Forget all passwords");
        bForgetPasswords.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bForgetPasswordsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(bForgetPasswords, gridBagConstraints);

        xRememberServer.setText("Remember last-joined server");
        xRememberServer.setToolTipText("<html>Choose whether the launcher should remember last-joined server.<br>\nWhen enabled, the [Resume] button will become available, which will reconnect<br>\nyou to the most-recently-joined server using the same username/credentials as last time.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        getContentPane().add(xRememberServer, gridBagConstraints);

        bForgetServer.setText("Forget last server");
        bForgetServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bForgetServerActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(bForgetServer, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(jSeparator3, gridBagConstraints);

        lParameters.setText("Java args");
        lParameters.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        getContentPane().add(lParameters, gridBagConstraints);

        tJavaArgs.setToolTipText("<html>Command-line arguments to pass to the client's Java runtime.<br>\nDon't mess with these unless you know exactly what you're doing!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(tJavaArgs, gridBagConstraints);

        lMemory.setText("Max memory");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        getContentPane().add(lMemory, gridBagConstraints);

        nMemory.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(64), Integer.valueOf(64), null, Integer.valueOf(16)));
        nMemory.setToolTipText("<html>The maximum amount of memory, in megabytes, that the game is allowed to use.<br>\nDon't raise this amount unless your game keeps running out of memory on large maps.<br>\nDefault is 800 MB. Going any lower may cause lag and/or crashes.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(nMemory, gridBagConstraints);

        jSeparator4.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 8, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(jSeparator4, gridBagConstraints);

        bDefaults.setText("Defaults");
        bDefaults.setToolTipText("Reset all preferences to their default values.");
        bDefaults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bDefaultsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        getContentPane().add(bDefaults, gridBagConstraints);

        bSave.setText("Save");
        bSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSaveActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bSave, gridBagConstraints);

        bCancel.setText("Cancel");
        bCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bCancel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(filler1, gridBagConstraints);

        xDebugMode.setText("Debug mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        getContentPane().add(xDebugMode, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void xRememberPasswordsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_xRememberPasswordsItemStateChanged
        if (evt.getStateChange() == ItemEvent.DESELECTED) {
            this.bForgetPasswords.setEnabled(false);
        } else {
            this.bForgetPasswords.setEnabled(true);
        }
    }//GEN-LAST:event_xRememberPasswordsItemStateChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private net.classicube.launcher.gui.JNiceLookingButton bCancel;
    private net.classicube.launcher.gui.JNiceLookingButton bDefaults;
    private net.classicube.launcher.gui.JNiceLookingButton bForgetPasswords;
    private net.classicube.launcher.gui.JNiceLookingButton bForgetServer;
    private net.classicube.launcher.gui.JNiceLookingButton bForgetUsers;
    private net.classicube.launcher.gui.JNiceLookingButton bSave;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSpinner nMemory;
    private javax.swing.JRadioButton rUpdateAutomatic;
    private javax.swing.JRadioButton rUpdateDisabled;
    private javax.swing.JRadioButton rUpdateNotify;
    private javax.swing.ButtonGroup rgUpdateMode;
    private javax.swing.JTextField tJavaArgs;
    private javax.swing.JCheckBox xDebugMode;
    private javax.swing.JCheckBox xFullscreen;
    private javax.swing.JCheckBox xRememberPasswords;
    private javax.swing.JCheckBox xRememberServer;
    private javax.swing.JCheckBox xRememberUsers;
    // End of variables declaration//GEN-END:variables
}
