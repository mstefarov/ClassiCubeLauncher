package net.classicube.launcher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingWorker.StateValue;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import net.classicube.launcher.AccountManager;
import net.classicube.launcher.GameServiceType;
import net.classicube.launcher.GameSession;
import net.classicube.launcher.GetExternalIPTask;
import net.classicube.launcher.LogUtil;
import net.classicube.launcher.Prefs;
import net.classicube.launcher.ServerJoinInfo;
import net.classicube.launcher.SessionManager;
import net.classicube.launcher.SignInResult;
import net.classicube.launcher.UserAccount;

// Sign-in screen! First thing the user sees.
// Instantiated and first shown by EntryPoint.main
public final class SignInScreen extends javax.swing.JFrame {
    // =============================================================================================
    //                                                                            FIELDS & CONSTANTS
    // =============================================================================================

    private AccountManager accountManager;
    private final ImagePanel bgPanel;
    private UsernameOrPasswordChangedListener fieldChangeListener;
    private GameSession.SignInTask signInTask;
    private JPopupMenu resumeMenu;
    private JMenuItem singlePlayerMenuItem;
    private JMenuItem directMenuItem;
    private boolean settingPasswordText;

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
        this.bResumeDropDown.setPreferredSize(
                new Dimension(20, this.bResume.getPreferredSize().height));
        this.bResumeDropDown.setMinimumSize(this.bResumeDropDown.getPreferredSize());
        hookUpListeners();
        getRootPane().setDefaultButton(bSignIn);
        this.ipLogo.setBorder(new EmptyBorder(8, 8, 8, 8));

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
        bResume.setEnabled(false);
        bResumeDropDown.setEnabled(false);
        directMenuItem.setEnabled(false);
        singlePlayerMenuItem.setEnabled(false);
        bSignIn.setEnabled(false);
        bPreferences.setEnabled(false);
        bChangeService.setEnabled(false);

        resumeMenu.setVisible(false);
        bResume.setComponentPopupMenu(null);

        progress.setVisible(true);
        pack();
    }

    // Re-enabled the UI, and hides the progress bar
    private void enableGUI() {
        cUsername.setEnabled(true);
        tPassword.setEnabled(true);
        bResumeDropDown.setEnabled(true);
        directMenuItem.setEnabled(true);
        singlePlayerMenuItem.setEnabled(true);
        enableResumeIfNeeded();
        checkIfSignInAllowed();
        bPreferences.setEnabled(true);
        bChangeService.setEnabled(true);

        progress.setVisible(false);
        bResume.setComponentPopupMenu(resumeMenu);
        pack();
    }

    void selectClassiCubeNet() {
        LogUtil.getLogger().log(Level.FINE, "SignInScreen.SelectClassiCube");
        bgPanel.setImage(Resources.getClassiCubeBackground());
        bgPanel.setGradientColor(new Color(124, 104, 141));
        ipLogo.setImage(Resources.getClassiCubeLogo());

        bChangeService.setText("Switch to Minecraft.net");
        SessionManager.selectService(GameServiceType.ClassiCubeNetService);
        onAfterServiceChanged();
    }

    void selectMinecraftNet() {
        LogUtil.getLogger().log(Level.FINE, "SignInScreen.SelectMinecraftNet");
        bgPanel.setImage(Resources.getMinecraftNetBackground());
        ipLogo.setImage(Resources.getMinecraftNetLogo());
        bgPanel.setGradientColor(new Color(36, 36, 36));
        bChangeService.setText("Switch to ClassiCube");
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
                accountManager.store(acct);
                new ServerListScreen().setVisible(true);
                dispose();
            } else {
                final String errorMsg = SignInResult.getMessage(result);
                ErrorScreen.show("Could not sign in", errorMsg, null);
            }
        } catch (final Exception ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error singing in", ex);
            ErrorScreen.show("Error signing in", ex.getMessage(), ex);
        }
        enableGUI();
    }

    // =============================================================================================
    //                                                                     DIRECT-CONNECT AND RESUME
    // =============================================================================================
    private class DirectMenuItemActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            LogUtil.getLogger().log(Level.FINE, "[Direct]");
            final String prompt = "mc://";
            final String input = PromptScreen.show("Direct connect",
                    "You can connect to a server directly, bypassing sign-in,<br>"
                    + "if you have a direct-connect URL in the form:<br>"
                    + "<code>mc://address:port/username/mppass</code>",
                    prompt, true);
            if (input != null && !prompt.equals(input)) {
                final String trimmedInput = input.replaceAll("[\\r\\n\\s]", "");
                final ServerJoinInfo joinInfo = SessionManager.getSession().getDetailsFromUrl(trimmedInput);
                if (joinInfo == null) {
                    ErrorScreen.show("Unrecognized link", "Cannot join server directly: Unrecognized link format.", null);
                } else if (joinInfo.signInNeeded) {
                    ErrorScreen.show("Not a direct link", "Cannot join server directly: Sign in before using this URL.", null);
                } else {
                    // check if server IP is same as local 
                    InetAddress serverAddress = joinInfo.address;
                    try {
                        InetAddress localAddress = GetExternalIPTask.getInstance().get();
                        if (serverAddress.equals(localAddress)) {
                            InetAddress correctedAddress = SameIPScreen.show(serverAddress, joinInfo.port);
                            if (correctedAddress == null) {
                                return; // player canceled/closed dialog
                            } else {
                                joinInfo.address = correctedAddress;
                            }
                        }

                    } catch (InterruptedException | ExecutionException ex) {
                        GetExternalIPTask.logAndShowError(ex);
                    }
                    if (!Prefs.getKeepOpen()) {
                        dispose();
                    }
                    UpdateScreen.createAndShow(joinInfo);
                }
            }
        }
    }

    private void enableResumeIfNeeded() {
        final ServerJoinInfo resumeInfo = SessionManager.getSession().loadResumeInfo();
        bResume.setEnabled(resumeInfo != null);
        if (resumeInfo != null) {
            bResume.setToolTipText("<html>Re-join the last server (as <b>" + resumeInfo.playerName + "</b>)");
        }
    }

    private void bResumeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bResumeActionPerformed
        LogUtil.getLogger().log(Level.FINE, "[Resume]");
        final ServerJoinInfo joinInfo = SessionManager.getSession().loadResumeInfo();
        if (!Prefs.getKeepOpen()) {
            dispose();
        }
        UpdateScreen.createAndShow(joinInfo);
    }//GEN-LAST:event_bResumeActionPerformed

    private void bResumeDropDownMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bResumeDropDownMousePressed
        if (!bResumeDropDown.isEnabled()) {
            return;
        }

        // Disarm the button here to prevent toggling the "selected" property twice
        // (once by popup menu handler, and then again by JToggleButton's internal model)
        bResumeDropDown.getModel().setArmed(false);

        resumeMenu.show(bResume, 0, 0);
    }//GEN-LAST:event_bResumeDropDownMousePressed

    private void bResumeDropDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bResumeDropDownActionPerformed
        resumeMenu.show(bResume, 0, 0);
    }//GEN-LAST:event_bResumeDropDownActionPerformed

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

        // Selects all text in the username field on-focus,
        // and fills in the password field for known usernames
        usernameEditor.addFocusListener(new UsernameFocusListener());

        // Add a context menu to username text
        CutCopyPasteAdapter.addToComponent(usernameEditor, true, true);
        CutCopyPasteAdapter.addToComponent(tPassword, false, true);

        // Create the drop-down menu for [Resume|v] split button
        resumeMenu = new JPopupMenu();
        resumeMenu.addPopupMenuListener(new ResumePopupMenuListener());

        directMenuItem = new JMenuItem("Direct...");
        directMenuItem.addActionListener(new DirectMenuItemActionListener());
        resumeMenu.add(directMenuItem);

        singlePlayerMenuItem = new JMenuItem("Single Player");
        singlePlayerMenuItem.addActionListener(new SinglePlayerMenuItemActionListener());
        resumeMenu.add(singlePlayerMenuItem);

        // The created menu is assigned to bResume in enableGUI()
    }

    // Cosmetic enhancements for the [Resume|v] drop-down menu
    class ResumePopupMenuListener implements PopupMenuListener {

        @Override
        public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            // Align the drop-down menu to the [Resume|v] button
            final Point buttonLoc = bResume.getLocationOnScreen();
            resumeMenu.setLocation(buttonLoc.x, buttonLoc.y + bResume.getHeight() - 1);
            // Make the |v] appear pressed
            bResumeDropDown.setSelected(true);
            bResumeDropDown.repaint();
        }

        @Override
        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            // Make the |v] appear released
            bResumeDropDown.setSelected(false);
            bResumeDropDown.repaint();
        }

        @Override
        public void popupMenuCanceled(final PopupMenuEvent e) { // do nothing
        }
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

        boolean wasTemp;

        @Override
        public void focusGained(final FocusEvent e) {
            final String selectedUsername;
            final JTextComponent editor = (JTextField) cUsername.getEditor().getEditorComponent();
            if (wasTemp) {
                selectedUsername = editor.getText();
                locatePassword(selectedUsername);
            } else {
                selectedUsername = (String) cUsername.getSelectedItem();
                if (selectedUsername != null) {
                    editor.setCaretPosition(selectedUsername.length());
                    editor.moveCaretPosition(0);
                }
            }
        }

        @Override
        public void focusLost(final FocusEvent e) {
            wasTemp = e.isTemporary();
            if (!wasTemp) {
                final String selectedUsername = (String) cUsername.getSelectedItem();
                locatePassword(selectedUsername);
            }
        }

        void locatePassword(final String selectedUsername) {
            if (selectedUsername != null && Prefs.getRememberPasswords()) {
                final UserAccount curAccount = accountManager.findAccount(selectedUsername);
                if (curAccount != null) {
                    // We have to temporarily prevent checkIfSignInAllowed() from being called here.
                    // There are two DocumentEvents fired here by tPassword (first for removal, second for insertion)
                    // which causes [Sign In] button to briefly flicker, preventing user from
                    // clicking it right after selecting a username from drop-down list.
                    settingPasswordText = true;
                    tPassword.setText(curAccount.password);
                    settingPasswordText = false;
                    // Cause checkIfSignInAllowed() to be called manually once, after password text was changed
                    fieldChangeListener.actionPerformed(null);
                    checkIfSignInAllowed();
                }
            }
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
            if (settingPasswordText) {
                // See notes on UsernameFocusListener.locatePassword
                return;
            }
            final Document doc = e.getDocument();
            if (doc.equals(tPassword.getDocument())) {
                realPasswordLength = doc.getLength();
            } else {
                realUsernameLength = doc.getLength();
                tPassword.setText("");
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
    private void checkIfSignInAllowed() {
        final boolean enableSignIn = (fieldChangeListener.realUsernameLength > 1)
                && (fieldChangeListener.realPasswordLength > 0);
        bSignIn.setEnabled(enableSignIn);
    }

    private void bChangeServiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bChangeServiceActionPerformed
        LogUtil.getLogger().log(Level.FINE, "[{0}]", bChangeService.getText());
        // pick the other game service
        if (Prefs.getSelectedGameService() == GameServiceType.ClassiCubeNetService) {
            selectMinecraftNet();
        } else {
            selectClassiCubeNet();
        }
    }//GEN-LAST:event_bChangeServiceActionPerformed

    private void bPreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bPreferencesActionPerformed
        LogUtil.getLogger().log(Level.FINE, "[Preferences]");
        accountManager = SessionManager.getAccountManager();
        boolean hadAccounts = accountManager.hasAccounts();
        
        new PreferencesScreen(this).setVisible(true);

        // if usernames have just been forgotten, clear the list
        if (hadAccounts && !accountManager.hasAccounts()) {
            cUsername.removeAllItems();
        }
    }//GEN-LAST:event_bPreferencesActionPerformed

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

        bChangeService = new net.classicube.launcher.gui.JNiceLookingButton();
        bPreferences = new net.classicube.launcher.gui.JNiceLookingButton();
        ipLogo = new net.classicube.launcher.gui.ImagePanel();
        cUsername = new javax.swing.JComboBox<String>();
        tPassword = new javax.swing.JPasswordField();
        bResume = new net.classicube.launcher.gui.JNiceLookingButton();
        bResumeDropDown = new net.classicube.launcher.gui.JNiceLookingToggleButton();
        bSignIn = new net.classicube.launcher.gui.JNiceLookingButton();
        progress = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ClassiCube Launcher");
        setBackground(new java.awt.Color(153, 128, 173));
        setName("ClassiCube Launcher"); // NOI18N
        setPreferredSize(new java.awt.Dimension(320, 270));
        getContentPane().setLayout(new java.awt.GridBagLayout());

        bChangeService.setText("Switch to ServiceName");
        bChangeService.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bChangeServiceActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        getContentPane().add(bChangeService, gridBagConstraints);

        bPreferences.setText("Preferences");
        bPreferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bPreferencesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        getContentPane().add(bPreferences, gridBagConstraints);

        ipLogo.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.weighty = 0.1;
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

        bResume.setText("Resume");
        bResume.setWidthAdjust(1);
        bResume.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bResumeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        getContentPane().add(bResume, gridBagConstraints);

        bResumeDropDown.setText("v");
        bResumeDropDown.setMargin(new java.awt.Insets(2, 2, 2, 2));
        bResumeDropDown.setWidthAdjust(-2);
        bResumeDropDown.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                bResumeDropDownMousePressed(evt);
            }
        });
        bResumeDropDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bResumeDropDownActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(bResumeDropDown, gridBagConstraints);

        bSignIn.setText("Sign In >");
        bSignIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSignInActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(bSignIn, gridBagConstraints);

        progress.setIndeterminate(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(progress, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private net.classicube.launcher.gui.JNiceLookingButton bChangeService;
    private net.classicube.launcher.gui.JNiceLookingButton bPreferences;
    private net.classicube.launcher.gui.JNiceLookingButton bResume;
    private net.classicube.launcher.gui.JNiceLookingToggleButton bResumeDropDown;
    private net.classicube.launcher.gui.JNiceLookingButton bSignIn;
    private javax.swing.JComboBox<String> cUsername;
    private net.classicube.launcher.gui.ImagePanel ipLogo;
    private javax.swing.JProgressBar progress;
    private javax.swing.JPasswordField tPassword;
    // End of variables declaration//GEN-END:variables

    private class SinglePlayerMenuItemActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO
            LogUtil.getLogger().log(Level.FINE, "[SinglePlayer]");
            if (!Prefs.getKeepOpen()) {
                dispose();
            }
            UpdateScreen.createAndShow(null);
        }
    }
}
