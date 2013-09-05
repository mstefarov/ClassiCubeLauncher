package net.classicube.launcher;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

final class SignInScreen extends javax.swing.JFrame {

    static final long serialVersionUID = 1L;

    public SignInScreen() {
        bgPanel = new ImagePanel(null, true);
        this.setContentPane(bgPanel);
        bgPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        initComponents();

        // hook up listeners for username/password field changes
        fieldChangeListener = new UsernameOrPasswordChangedListener();
        JTextComponent usernameEditor = (JTextComponent) cUsername.getEditor().getEditorComponent();
        usernameEditor.getDocument().addDocumentListener(fieldChangeListener);
        tPassword.getDocument().addDocumentListener(fieldChangeListener);
        cUsername.addActionListener(fieldChangeListener);
        tPassword.addActionListener(fieldChangeListener);

        // some last-minute UI tweaks
        progressFiller.setSize(progress.getHeight(), progress.getWidth());
        progress.setVisible(false);
        SelectClassiCube();
        enableGUI();
    }

    class UsernameOrPasswordChangedListener implements DocumentListener, ActionListener {

        public int realPasswordLength,
                realUsernameLength;

        public UsernameOrPasswordChangedListener() {
            realPasswordLength = tPassword.getPassword().length;
            String username = (String) cUsername.getSelectedItem();
            if (username == null) {
                realUsernameLength = 0;
            } else {
                realUsernameLength = username.length();
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            somethingEdited(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            somethingEdited(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            somethingEdited(e);
        }

        private void somethingEdited(DocumentEvent e) {
            Document doc = e.getDocument();
            if (doc.equals(tPassword.getDocument())) {
                realPasswordLength = doc.getLength();
            } else {
                realUsernameLength = doc.getLength();
            }
            checkIfSignInAllowed();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            realPasswordLength = tPassword.getPassword().length;
            String username = (String) cUsername.getSelectedItem();
            if (username == null) {
                realUsernameLength = 0;
            } else {
                realUsernameLength = username.length();
            }
            checkIfSignInAllowed();
        }
    }

    void checkIfSignInAllowed() {
        final boolean enableSignIn = (fieldChangeListener.realUsernameLength > 0)
                && (fieldChangeListener.realPasswordLength > 0);
        bSignIn.setEnabled(enableSignIn);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        bClassiCubeNet = new javax.swing.JToggleButton();
        bMinecraftNet = new javax.swing.JToggleButton();
        cUsername = new javax.swing.JComboBox<String>();
        tPassword = new javax.swing.JPasswordField();
        xRememberMe = new javax.swing.JCheckBox();
        bSignIn = new javax.swing.JButton();
        ipLogo = new net.classicube.launcher.ImagePanel();
        progress = new javax.swing.JProgressBar();
        progressFiller = new javax.swing.Box.Filler(new java.awt.Dimension(0, 14), new java.awt.Dimension(0, 14), new java.awt.Dimension(32767, 14));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ClassiCube Launcher");
        setBackground(new java.awt.Color(153, 128, 173));
        setName("ClassiCube Launcher"); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());

        bClassiCubeNet.setText("ClassiCube.net");
        bClassiCubeNet.setEnabled(false);
        bClassiCubeNet.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                bClassiCubeNetItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(bClassiCubeNet, gridBagConstraints);

        bMinecraftNet.setText("Minecraft.net");
        bMinecraftNet.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                bMinecraftNetItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        getContentPane().add(bMinecraftNet, gridBagConstraints);

        cUsername.setEditable(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(cUsername, gridBagConstraints);

        tPassword.setText("password");
        tPassword.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(tPassword, gridBagConstraints);

        xRememberMe.setForeground(new java.awt.Color(255, 255, 255));
        xRememberMe.setText("Remember me");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        getContentPane().add(xRememberMe, gridBagConstraints);

        bSignIn.setText("Sign In");
        bSignIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSignInActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_TRAILING;
        getContentPane().add(bSignIn, gridBagConstraints);

        ipLogo.setPreferredSize(new java.awt.Dimension(250, 75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        getContentPane().add(ipLogo, gridBagConstraints);

        progress.setIndeterminate(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(progress, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(progressFiller, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bMinecraftNetItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_bMinecraftNetItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            LogUtil.Log(Level.FINE, "[Minecraft.Net]");
            SelectMinecraftNet();
        }
    }//GEN-LAST:event_bMinecraftNetItemStateChanged

    private void bClassiCubeNetItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_bClassiCubeNetItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            LogUtil.Log(Level.FINE, "[ClassiCube.Net]");
            SelectClassiCube();
        }
    }//GEN-LAST:event_bClassiCubeNetItemStateChanged

    private void bSignInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSignInActionPerformed
        String username = (String) cUsername.getSelectedItem();
        String password = new String(tPassword.getPassword());
        UserAccount newAcct = new UserAccount(username, password);
        GameService.activeService = new MinecraftNetService(newAcct);
        boolean remember = this.xRememberMe.isSelected();
        signInTask = GameService.activeService.signInAsync(remember);
        signInTask.addPropertyChangeListener(
                new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("state".equals(evt.getPropertyName())) {
                    if (evt.getNewValue().equals(StateValue.DONE)) {
                        onSignInDone(signInTask);
                    }
                }
            }
        });
        disableGUI();
        signInTask.execute();
    }//GEN-LAST:event_bSignInActionPerformed

    private void onSignInDone(SwingWorker<SignInResult, String> signInTask) {
        try {
            SignInResult result = signInTask.get();
            if (result == SignInResult.SUCCESS) {
                EntryPoint.ShowServerListScreen();
            } else {
                LogUtil.ShowInfo(result.name(), "Sign in result.");
            }
        } catch (InterruptedException | ExecutionException ex) {
            LogUtil.ShowWarning(ex.toString(), "Problem signing in");
        }
        enableGUI();
    }

    private void disableGUI() {
        bSignIn.setEnabled(false);
        cUsername.setEditable(false);
        tPassword.setEditable(false);
        buttonToDisableOnSignIn.setEnabled(false);
        xRememberMe.setEnabled(false);
        progress.setVisible(true);
    }

    private void enableGUI() {
        checkIfSignInAllowed();
        cUsername.setEditable(true);
        tPassword.setEditable(true);
        buttonToDisableOnSignIn.setEnabled(true);
        xRememberMe.setEnabled(true);
        progress.setVisible(false);
    }

    void SelectClassiCube() {
        LogUtil.Log(Level.FINE, "SignInScreen.SelectClassiCube");
        bgPanel.setImage(Resources.getClassiCubeBackground());
        ipLogo.setImage(Resources.getClassiCubeLogo());
        bMinecraftNet.setEnabled(true);
        bMinecraftNet.setSelected(false);
        bClassiCubeNet.setEnabled(false);
        buttonToDisableOnSignIn = bMinecraftNet;
        SetAccountManager("ClassiCube.net");
        this.repaint();
        this.cUsername.requestFocus();
    }

    void SelectMinecraftNet() {
        LogUtil.Log(Level.FINE, "SignInScreen.SelectMinecraftNet");
        bgPanel.setImage(Resources.getMinecraftNetBackground());
        ipLogo.setImage(Resources.getMinecraftNetLogo());
        bClassiCubeNet.setEnabled(true);
        bClassiCubeNet.setSelected(false);
        bMinecraftNet.setEnabled(false);
        buttonToDisableOnSignIn = bClassiCubeNet;
        SetAccountManager("Minecraft.net");
        this.repaint();
        this.cUsername.requestFocus();
    }

    void SetAccountManager(String serviceName) {
        accountManager = new AccountManager(serviceName);
        this.cUsername.removeAllItems();
        for (UserAccount account : accountManager.GetAccountsBySignInDate()) {
            this.cUsername.addItem(account.SignInUsername);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton bClassiCubeNet;
    private javax.swing.JToggleButton bMinecraftNet;
    private javax.swing.JButton bSignIn;
    private javax.swing.JComboBox<String> cUsername;
    private net.classicube.launcher.ImagePanel ipLogo;
    private javax.swing.JProgressBar progress;
    private javax.swing.Box.Filler progressFiller;
    private javax.swing.JPasswordField tPassword;
    private javax.swing.JCheckBox xRememberMe;
    // End of variables declaration//GEN-END:variables
    AccountManager accountManager;
    ImagePanel bgPanel;
    JToggleButton buttonToDisableOnSignIn;
    UsernameOrPasswordChangedListener fieldChangeListener;
    SwingWorker<SignInResult, String> signInTask;
}
