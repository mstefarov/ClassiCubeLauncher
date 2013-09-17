package net.classicube.launcher;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker.StateValue;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

// Sign-in screen! First thing the user sees.
// Instantiated and first shown by EntryPoint.main
final class SignInScreen extends javax.swing.JFrame {
    // =============================================================================================
    //                                                                            FIELDS & CONSTANTS
    // =============================================================================================

    private AccountManager accountManager;
    private final ImagePanel bgPanel;
    private JToggleButton buttonToDisableOnSignIn;
    private UsernameOrPasswordChangedListener fieldChangeListener;
    private GameSession.SignInTask signInTask;

    // =============================================================================================
    //                                                                                INITIALIZATION
    // =============================================================================================
    public SignInScreen() {
        LogUtil.getLogger().log(Level.FINE, "SignInScreen");

        // add our fancy custom background
        bgPanel = new ImagePanel(null, true);
        bgPanel.setGradient(true);
        setContentPane(bgPanel);
        bgPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // create the rest of components
        initComponents();

        // some UI tweaks
        hookUpListeners();
        getRootPane().setDefaultButton(bSignIn);

        // center the form on screen (initially)
        setLocationRelativeTo(null);

        // pick the appropriate game service
        if (Prefs.getSelectedGameService() == GameServiceType.ClassiCubeNetService) {
            selectClassiCubeNet();
        } else {
            selectMinecraftNet();
        }

        // Alright, we're good to go.
        enableGUI();
    }

    // Grays out the UI, and shows a progress bar
    private void disableGUI() {
        cUsername.setEnabled(false);
        tPassword.setEnabled(false);
        bDirect.setEnabled(false);
        bResume.setEnabled(false);
        bSignIn.setEnabled(false);
        buttonToDisableOnSignIn.setEnabled(false);

        progress.setVisible(true);
        pack();
    }

    // Re-enabled the UI, and hides the progress bar
    private void enableGUI() {
        cUsername.setEnabled(true);
        tPassword.setEnabled(true);
        bDirect.setEnabled(true);
        enableResumeIfNeeded();
        checkIfSignInAllowed();
        buttonToDisableOnSignIn.setEnabled(true);

        progress.setVisible(false);
        pack();
    }

    // =============================================================================================
    //                                                      MINECRAFT / CLASSICUBE SERVICE SWITCHING
    // =============================================================================================
    private void bMinecraftNetItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_bMinecraftNetItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            LogUtil.getLogger().log(Level.INFO, "[Minecraft.Net]");
            selectMinecraftNet();
        }
    }//GEN-LAST:event_bMinecraftNetItemStateChanged

    private void bClassiCubeNetItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_bClassiCubeNetItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            LogUtil.getLogger().log(Level.INFO, "[ClassiCube.Net]");
            selectClassiCubeNet();
        }
    }//GEN-LAST:event_bClassiCubeNetItemStateChanged

    void selectClassiCubeNet() {
        LogUtil.getLogger().log(Level.FINE, "SignInScreen.SelectClassiCube");
        bgPanel.setImage(Resources.getClassiCubeBackground());
        bgPanel.setGradientColor(new Color(124, 104, 141));
        ipLogo.setImage(Resources.getClassiCubeLogo());
        bMinecraftNet.setEnabled(true);
        bMinecraftNet.setSelected(false);
        bClassiCubeNet.setEnabled(false);
        buttonToDisableOnSignIn = bMinecraftNet;
        SessionManager.selectService(GameServiceType.ClassiCubeNetService);
        onAfterServiceChanged();

    }

    void selectMinecraftNet() {
        LogUtil.getLogger().log(Level.FINE, "SignInScreen.SelectMinecraftNet");
        bgPanel.setImage(Resources.getMinecraftNetBackground());
        ipLogo.setImage(Resources.getMinecraftNetLogo());
        bgPanel.setGradientColor(new Color(36, 36, 36));
        bClassiCubeNet.setEnabled(true);
        bClassiCubeNet.setSelected(false);
        bMinecraftNet.setEnabled(false);
        buttonToDisableOnSignIn = bClassiCubeNet;
        SessionManager.selectService(GameServiceType.MinecraftNetService);
        onAfterServiceChanged();
    }

    // Called after either [Minecraft.net] or [ClassiCube] button is pressed.
    // Loads accounts, changes the background/logo, switches focus back to username/password fields
    void onAfterServiceChanged() {
        accountManager = SessionManager.getAccountManager();
        cUsername.removeAllItems();
        tPassword.setText("");
        // fill the account list
        final UserAccount[] accounts = accountManager.getAccountsBySignInDate();
        for (UserAccount account : accounts) {
            cUsername.addItem(account.signInUsername);
        }
        if (cUsername.getItemCount() > 0) {
            cUsername.setSelectedIndex(0);
        }
        repaint();

        // focus on either username (if empty) or password field
        final String username = (String) cUsername.getSelectedItem();
        if (username == null || username.isEmpty()) {
            cUsername.requestFocus();
        } else {
            tPassword.requestFocus();
        }

        enableResumeIfNeeded();
        // check if we have "resume" info
    }

    // =============================================================================================
    //                                                                              SIGN-IN HANDLING
    // =============================================================================================
    private void bSignInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSignInActionPerformed
        // Grab user information from the form
        LogUtil.getLogger().log(Level.INFO, "[Sign In]");
        final String username = (String) cUsername.getSelectedItem();
        final String password = new String(tPassword.getPassword());
        final UserAccount account = accountManager.onSignInBegin(username, password);
        final boolean remember = Prefs.getRememberPasswords();

        // Create an async task for signing in
        final GameSession session = SessionManager.getSession();
        signInTask = session.signInAsync(account, remember);

        // Get ready to handle the task completion
        signInTask.addPropertyChangeListener(
                new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if ("state".equals(evt.getPropertyName())) {
                    if (evt.getNewValue().equals(StateValue.DONE)) {
                        onSignInDone(signInTask);
                    }
                }
            }
        });

        // Gray everything out and show a progress bar
        disableGUI();

        // Begin signing in asynchronously
        signInTask.execute();
    }//GEN-LAST:event_bSignInActionPerformed

    // Called when signInAsync finishes.
    // If we signed in, advance to the server list screen.
    // Otherwise, inform the user that something went wrong.
    private void onSignInDone(final GameSession.SignInTask signInTask) {
        LogUtil.getLogger().log(Level.FINE, "onSignInDone");
        try {
            final SignInResult result = signInTask.get();
            if (result == SignInResult.SUCCESS) {
                final UserAccount acct = SessionManager.getSession().getAccount();
                acct.signInDate = new Date();
                accountManager.store();
                new ServerListScreen().setVisible(true);
                dispose();
            } else {
                final String errorMsg;
                switch (result) {
                    case WRONG_USER_OR_PASS:
                        errorMsg = "Wrong username or password.";
                        break;
                    case MIGRATED_ACCOUNT:
                        errorMsg = "Your account has been migrated. "
                                + "Use your Mojang account (email) to sign in.";
                        break;
                    case CONNECTION_ERROR:
                        errorMsg = "Connection problem. The website may be down.";
                        break;
                    default:
                        errorMsg = result.name();
                        break;
                }
                LogUtil.showWarning(errorMsg, "Could not sign in.");
            }
        } catch (final InterruptedException | ExecutionException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error singing in", ex);
            LogUtil.showError(ex.toString(), "Could not sign in.");
        }
        enableGUI();
    }

    // =============================================================================================
    //                                                                     DIRECT-CONNECT AND RESUME
    // =============================================================================================
    private void bDirectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bDirectActionPerformed
        final String prompt = "mc://";
        final String input = JOptionPane.showInputDialog(this, "Connect to a server directly:", prompt);
        if (input != null && !prompt.equals(input)) {
            final String trimmedInput = input.replaceAll("[\\r\\n\\s]", "");
            final ServerJoinInfo joinInfo = SessionManager.getSession().getDetailsFromUrl(trimmedInput);
            if (joinInfo == null) {
                LogUtil.showWarning("Cannot join server directly: Unrecognized link format.", "Unrecognized link");
            } else if (joinInfo.signInNeeded) {
                LogUtil.showWarning("Cannot join server directly: Sign in before using this URL.", "Not a direct link");
            } else {
                dispose();
                ClientUpdateScreen.createAndShow(joinInfo);
            }
        }
    }//GEN-LAST:event_bDirectActionPerformed

    private void enableResumeIfNeeded() {
        final ServerJoinInfo resumeInfo = SessionManager.getSession().loadResumeInfo();
        bResume.setEnabled(resumeInfo != null);
    }

    private void bResumeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bResumeActionPerformed
        final ServerJoinInfo joinInfo = SessionManager.getSession().loadResumeInfo();
        dispose();
        ClientUpdateScreen.createAndShow(joinInfo);
    }//GEN-LAST:event_bResumeActionPerformed

    // =============================================================================================
    //                                                                           GUI EVENT LISTENERS
    // =============================================================================================
    private void hookUpListeners() {
        // hook up listeners for username/password field changes
        fieldChangeListener = new UsernameOrPasswordChangedListener();
        final JTextComponent usernameEditor = (JTextComponent) cUsername.getEditor().getEditorComponent();
        usernameEditor.getDocument().addDocumentListener(fieldChangeListener);
        tPassword.getDocument().addDocumentListener(fieldChangeListener);
        cUsername.addActionListener(fieldChangeListener);
        tPassword.addActionListener(fieldChangeListener);

        // Allow pressing <Enter> to sign in, while in the password textbox
        tPassword.addKeyListener(new PasswordEnterListener());

        // Selects all text in the username field on-focus
        usernameEditor.addFocusListener(new UsernameFocusListener());
    }

    // Select all text in password field, when focused
    private void tPasswordFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tPasswordFocusGained
        tPassword.selectAll();
    }//GEN-LAST:event_tPasswordFocusGained

    private void cUsernameItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cUsernameItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            final String newName = (String) evt.getItem();
            final UserAccount curAccount = accountManager.findAccount(newName);
            if (curAccount != null) {
                tPassword.setText(curAccount.password);
            }
        }
    }//GEN-LAST:event_cUsernameItemStateChanged

    // Selects all text in the username field on-focus (you'd think this would be easier)
    class UsernameFocusListener implements FocusListener {

        @Override
        public void focusGained(final FocusEvent e) {
            final JTextComponent editor = ((JTextField) cUsername.getEditor().getEditorComponent());
            final String selectedUsername = (String) cUsername.getSelectedItem();
            if (selectedUsername != null) {
                editor.setCaretPosition(selectedUsername.length());
                editor.moveCaretPosition(0);
            }
        }

        @Override
        public void focusLost(final FocusEvent e) {
        }
    }

    // Allows pressing <Enter> to sign in, while in the password textbox
    class PasswordEnterListener implements KeyListener {

        @Override
        public void keyTyped(final KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                if (bSignIn.isEnabled()) {
                    bSignIn.doClick();
                }
            }
        }

        @Override
        public void keyPressed(final KeyEvent e) { // do nothing
        }

        @Override
        public void keyReleased(final KeyEvent e) { // do nothing
        }
    }

    // Allows enabling/disabling [Sign In] button dynamically,
    // depending on whether username/password fields are empty,
    // while user is still focused on those fields.
    class UsernameOrPasswordChangedListener implements DocumentListener, ActionListener {

        public int realPasswordLength,
                realUsernameLength;

        public UsernameOrPasswordChangedListener() {
            realPasswordLength = tPassword.getPassword().length;
            final String username = (String) cUsername.getSelectedItem();
            if (username == null) {
                realUsernameLength = 0;
            } else {
                realUsernameLength = username.length();
            }
        }

        @Override
        public void insertUpdate(final DocumentEvent e) {
            somethingEdited(e);
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            somethingEdited(e);
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            somethingEdited(e);
        }

        private void somethingEdited(final DocumentEvent e) {
            final Document doc = e.getDocument();
            if (doc.equals(tPassword.getDocument())) {
                realPasswordLength = doc.getLength();
            } else {
                realUsernameLength = doc.getLength();
            }
            checkIfSignInAllowed();
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            realPasswordLength = tPassword.getPassword().length;
            final String username = (String) cUsername.getSelectedItem();
            if (username == null) {
                realUsernameLength = 0;
            } else {
                realUsernameLength = username.length();
            }
            checkIfSignInAllowed();
        }
    }

    // Enable/disable [Sign In] depending on whether username/password are given.
    void checkIfSignInAllowed() {
        final boolean enableSignIn = (fieldChangeListener.realUsernameLength > 1)
                && (fieldChangeListener.realPasswordLength > 0);
        bSignIn.setEnabled(enableSignIn);
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

        bClassiCubeNet = new net.classicube.launcher.JNiceLookingToggleButton();
        bMinecraftNet = new net.classicube.launcher.JNiceLookingToggleButton();
        ipLogo = new net.classicube.launcher.ImagePanel();
        cUsername = new javax.swing.JComboBox<String>();
        tPassword = new javax.swing.JPasswordField();
        progress = new javax.swing.JProgressBar();
        bDirect = new net.classicube.launcher.JNiceLookingButton();
        bResume = new net.classicube.launcher.JNiceLookingButton();
        bSignIn = new net.classicube.launcher.JNiceLookingButton();

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
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        getContentPane().add(bMinecraftNet, gridBagConstraints);

        ipLogo.setPreferredSize(new java.awt.Dimension(250, 75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        getContentPane().add(ipLogo, gridBagConstraints);

        cUsername.setEditable(true);
        cUsername.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cUsernameItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        getContentPane().add(cUsername, gridBagConstraints);

        tPassword.setToolTipText("");
        tPassword.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tPasswordFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        getContentPane().add(tPassword, gridBagConstraints);

        progress.setIndeterminate(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(progress, gridBagConstraints);

        bDirect.setText("Direct...");
        bDirect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bDirectActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        getContentPane().add(bDirect, gridBagConstraints);

        bResume.setText("Resume");
        bResume.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bResumeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        getContentPane().add(bResume, gridBagConstraints);

        bSignIn.setText("Sign In >");
        bSignIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSignInActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bSignIn, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private net.classicube.launcher.JNiceLookingToggleButton bClassiCubeNet;
    private net.classicube.launcher.JNiceLookingButton bDirect;
    private net.classicube.launcher.JNiceLookingToggleButton bMinecraftNet;
    private net.classicube.launcher.JNiceLookingButton bResume;
    private net.classicube.launcher.JNiceLookingButton bSignIn;
    private javax.swing.JComboBox<String> cUsername;
    private net.classicube.launcher.ImagePanel ipLogo;
    private javax.swing.JProgressBar progress;
    private javax.swing.JPasswordField tPassword;
    // End of variables declaration//GEN-END:variables
}
