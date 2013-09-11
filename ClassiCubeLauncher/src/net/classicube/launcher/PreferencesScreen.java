package net.classicube.launcher;

import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.border.EmptyBorder;

public class PreferencesScreen extends javax.swing.JDialog {

    public PreferencesScreen(JFrame parent) {
        super(parent, "Preferences", true);
        JRootPane root = getRootPane();
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        initComponents();
        root.setDefaultButton(bSave);
        loadPreferences();
        setLocationRelativeTo(parent);
    }

    private static Preferences getPrefs() {
        return Preferences.userNodeForPackage(PreferencesScreen.class);
    }

    public static void preparePrefs() {
        final Preferences prefs = getPrefs();
        prefs.put("WindowSize", prefs.get("WindowSize", "Normal"));
        prefs.put("UpdateMode", prefs.get("UpdateMode", "Notify"));
        prefs.putBoolean("RememberUsernames", prefs.getBoolean("RememberUsernames", true));
        prefs.putBoolean("RememberPasswords", prefs.getBoolean("RememberPasswords", true));
        prefs.putBoolean("RememberServers", prefs.getBoolean("RememberServers", true));
        prefs.put("JavaArgs", prefs.get("JavaArgs", ClientLauncher.getDefaultJavaArgs()));
        prefs.putInt("Memory", prefs.getInt("Memory", 800));
    }

    private void loadPreferences() {
        final Preferences prefs = getPrefs();
        loadWindowSize(prefs.get("WindowSize", "Normal"));
        loadUpdateMode(prefs.get("UpdateMode", "Notify"));
        xRememberUsernames.setSelected(prefs.getBoolean("RememberUsernames", true));
        xRememberPasswords.setSelected(prefs.getBoolean("RememberPasswords", true));
        xRememberServers.setSelected(prefs.getBoolean("RememberServers", true));
        tJavaArgs.setText(prefs.get("JavaArgs", ClientLauncher.getDefaultJavaArgs()));
        nMemory.setValue(prefs.getInt("Memory", 800));
    }

    private void loadWindowSize(String val) {
        JRadioButton btn;
        switch (val) {
            case "Maximized":
                btn = rWindowMaximized;
                break;
            case "Fullscreen":
                btn = rWindowFullscreen;
                break;
            default: // "Normal":
                btn = rWindowNormal;
                break;
        }
        rgWindowSize.setSelected(btn.getModel(), true);
    }

    private void loadUpdateMode(String val) {
        JRadioButton btn;
        switch (val) {
            case "Disable":
                btn = rUpdateDisable;
                break;
            case "Auto":
                btn = rUpdateAuto;
                break;
            default: // "Notify":
                btn = rUpdateNotify;
                break;
        }
        rgUpdateMode.setSelected(btn.getModel(), true);
    }

    private void storePreferences() {
        Preferences prefs = getPrefs();
        prefs.put("WindowSize", storeWindowSize());
        prefs.put("UpdateMode", storeUpdateMode());
        prefs.putBoolean("RememberUsernames", xRememberUsernames.isSelected());
        prefs.putBoolean("RememberPasswords", xRememberPasswords.isSelected());
        prefs.putBoolean("RememberServers", xRememberServers.isSelected());
        prefs.put("JavaArgs", tJavaArgs.getText());
        prefs.putInt("Memory", (int) nMemory.getValue());
    }

    private String storeWindowSize() {
        String val;
        if (rWindowMaximized.isSelected()) {
            val = "Maximized";
        } else if (rWindowFullscreen.isSelected()) {
            val = "Fullscreen";
        } else {
            val = "Normal";
        }
        return val;
    }

    private String storeUpdateMode() {
        String val;
        if (rUpdateDisable.isSelected()) {
            val = "Disable";
        } else if (rUpdateAuto.isSelected()) {
            val = "Auto";
        } else {
            val = "Notify";
        }
        return val;
    }

    private void loadDefaults() {
        loadWindowSize("Normal");
        loadUpdateMode("Notify");
        xRememberUsernames.setSelected(true);
        xRememberPasswords.setSelected(true);
        xRememberServers.setSelected(true);
        tJavaArgs.setText(ClientLauncher.getDefaultJavaArgs());
        nMemory.setValue(800);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        rgWindowSize = new javax.swing.ButtonGroup();
        rgUpdateMode = new javax.swing.ButtonGroup();
        javax.swing.JLabel lWindowSize = new javax.swing.JLabel();
        rWindowNormal = new javax.swing.JRadioButton();
        rWindowMaximized = new javax.swing.JRadioButton();
        rWindowFullscreen = new javax.swing.JRadioButton();
        javax.swing.JLabel lUpdateMode = new javax.swing.JLabel();
        rUpdateDisable = new javax.swing.JRadioButton();
        rUpdateNotify = new javax.swing.JRadioButton();
        rUpdateAuto = new javax.swing.JRadioButton();
        javax.swing.JSeparator jSeparator1 = new javax.swing.JSeparator();
        xRememberUsernames = new javax.swing.JCheckBox();
        bForgetUsers = new javax.swing.JButton();
        xRememberPasswords = new javax.swing.JCheckBox();
        bForgetPasswords = new javax.swing.JButton();
        xRememberServers = new javax.swing.JCheckBox();
        bForgetServer = new javax.swing.JButton();
        javax.swing.JSeparator jSeparator2 = new javax.swing.JSeparator();
        javax.swing.JLabel lParameters = new javax.swing.JLabel();
        tJavaArgs = new javax.swing.JTextField();
        javax.swing.JLabel lMemory = new javax.swing.JLabel();
        nMemory = new javax.swing.JSpinner();
        javax.swing.JLabel lMB = new javax.swing.JLabel();
        javax.swing.JSeparator jSeparator3 = new javax.swing.JSeparator();
        bDefaults = new javax.swing.JButton();
        bSave = new javax.swing.JButton();
        bCancel = new javax.swing.JButton();
        javax.swing.Box.Filler filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        javax.swing.Box.Filler filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));
        javax.swing.Box.Filler filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        lWindowSize.setText("Game window size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        getContentPane().add(lWindowSize, gridBagConstraints);

        rgWindowSize.add(rWindowNormal);
        rWindowNormal.setText("Normal");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(rWindowNormal, gridBagConstraints);

        rgWindowSize.add(rWindowMaximized);
        rWindowMaximized.setText("Maximized");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(rWindowMaximized, gridBagConstraints);

        rgWindowSize.add(rWindowFullscreen);
        rWindowFullscreen.setText("Fullscreen");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(rWindowFullscreen, gridBagConstraints);

        lUpdateMode.setText("Install game updates...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        getContentPane().add(lUpdateMode, gridBagConstraints);

        rgUpdateMode.add(rUpdateDisable);
        rUpdateDisable.setText("Disable");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(rUpdateDisable, gridBagConstraints);

        rgUpdateMode.add(rUpdateNotify);
        rUpdateNotify.setText("Enable (notify me)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(rUpdateNotify, gridBagConstraints);

        rgUpdateMode.add(rUpdateAuto);
        rUpdateAuto.setSelected(true);
        rUpdateAuto.setText("Enable (automatic)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(rUpdateAuto, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(jSeparator1, gridBagConstraints);

        xRememberUsernames.setText("Remember usernames");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(xRememberUsernames, gridBagConstraints);

        bForgetUsers.setText("Forget all users");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, bForgetPasswords, org.jdesktop.beansbinding.ELProperty.create("${preferredSize}"), bForgetUsers, org.jdesktop.beansbinding.BeanProperty.create("preferredSize"));
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(bForgetUsers, gridBagConstraints);

        xRememberPasswords.setText("Remember passwords");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(xRememberPasswords, gridBagConstraints);

        bForgetPasswords.setText("Forget all passwords");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(bForgetPasswords, gridBagConstraints);

        xRememberServers.setText("Remember last-joined server");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(xRememberServers, gridBagConstraints);

        bForgetServer.setText("Forget last server");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, bForgetPasswords, org.jdesktop.beansbinding.ELProperty.create("${preferredSize}"), bForgetServer, org.jdesktop.beansbinding.BeanProperty.create("preferredSize"));
        bindingGroup.addBinding(binding);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(bForgetServer, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(jSeparator2, gridBagConstraints);

        lParameters.setText("Java args");
        lParameters.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        getContentPane().add(lParameters, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(tJavaArgs, gridBagConstraints);

        lMemory.setText("Max memory");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        getContentPane().add(lMemory, gridBagConstraints);

        nMemory.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(64), Integer.valueOf(64), null, Integer.valueOf(16)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(nMemory, gridBagConstraints);

        lMB.setText("MB");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        getContentPane().add(lMB, gridBagConstraints);

        jSeparator3.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 8, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        getContentPane().add(jSeparator3, gridBagConstraints);

        bDefaults.setText("Defaults");
        bDefaults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bDefaultsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        getContentPane().add(bDefaults, gridBagConstraints);

        bSave.setText("Save");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, bCancel, org.jdesktop.beansbinding.ELProperty.create("${preferredSize}"), bSave, org.jdesktop.beansbinding.BeanProperty.create("preferredSize"));
        bindingGroup.addBinding(binding);

        bSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSaveActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bSave, gridBagConstraints);

        bCancel.setText("Cancel");
        bCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bCancel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(filler1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        getContentPane().add(filler2, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        getContentPane().add(filler3, gridBagConstraints);

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSaveActionPerformed
        storePreferences();
        dispose();
    }//GEN-LAST:event_bSaveActionPerformed

    private void bCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bCancelActionPerformed
        dispose();
    }//GEN-LAST:event_bCancelActionPerformed

    private void bDefaultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bDefaultsActionPerformed
        loadDefaults();
    }//GEN-LAST:event_bDefaultsActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bCancel;
    private javax.swing.JButton bDefaults;
    private javax.swing.JButton bForgetPasswords;
    private javax.swing.JButton bForgetServer;
    private javax.swing.JButton bForgetUsers;
    private javax.swing.JButton bSave;
    private javax.swing.JSpinner nMemory;
    private javax.swing.JRadioButton rUpdateAuto;
    private javax.swing.JRadioButton rUpdateDisable;
    private javax.swing.JRadioButton rUpdateNotify;
    private javax.swing.JRadioButton rWindowFullscreen;
    private javax.swing.JRadioButton rWindowMaximized;
    private javax.swing.JRadioButton rWindowNormal;
    private javax.swing.ButtonGroup rgUpdateMode;
    private javax.swing.ButtonGroup rgWindowSize;
    private javax.swing.JTextField tJavaArgs;
    private javax.swing.JCheckBox xRememberPasswords;
    private javax.swing.JCheckBox xRememberServers;
    private javax.swing.JCheckBox xRememberUsernames;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
