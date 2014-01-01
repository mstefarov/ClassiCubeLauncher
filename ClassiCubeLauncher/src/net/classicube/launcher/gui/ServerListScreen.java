package net.classicube.launcher.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SwingWorker.StateValue;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import net.classicube.launcher.GameServiceType;
import net.classicube.launcher.GameSession;
import net.classicube.launcher.LogUtil;
import net.classicube.launcher.ServerJoinInfo;
import net.classicube.launcher.ServerListEntry;
import net.classicube.launcher.SessionManager;

public final class ServerListScreen extends javax.swing.JFrame {
    // =============================================================================================
    //                                                                            FIELDS & CONSTANTS
    // =============================================================================================

    private final List<ServerListEntry> displayedServerList = new ArrayList<>();
    private GameSession.GetServerDetailsTask getServerDetailsTask;
    private final GameSession.GetServerListTask getServerListTask;
    private ServerListEntry selectedServer;
    private ServerListEntry[] serverList;
    private final GameSession session;
    private final TableColumnAdjuster tableColumnAdjuster;

    // =============================================================================================
    //                                                                                INITIALIZATION
    // =============================================================================================
    public ServerListScreen() {
        LogUtil.getLogger().log(Level.FINE, "ServerListScreen");

        // Make a pretty background
        final ImagePanel bgPanel = new ImagePanel(null, true);
        bgPanel.setGradient(true);
        setContentPane(bgPanel);
        bgPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // init components and stuff
        initComponents();
        this.serverTableContainer.getViewport().setBackground(new Color(247, 247, 247));

        // set window title
        session = SessionManager.getSession();
        final String playerName = session.getAccount().playerName;
        if (session.getServiceType() == GameServiceType.ClassiCubeNetService) {
            setTitle(playerName + " @ ClassiCube.net - servers");
            bgPanel.setImage(Resources.getClassiCubeBackground());
            bgPanel.setGradientColor(new Color(124, 104, 141));
        } else {
            setTitle(playerName + " @ Minecraft.net - servers");
            bgPanel.setImage(Resources.getMinecraftNetBackground());
            bgPanel.setGradientColor(new Color(36, 36, 36));
        }

        // prepare to auto-adjust table columns (when the data arrives)
        serverTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableColumnAdjuster = new TableColumnAdjuster(serverTable);

        // configure table sorting and selection
        serverTable.setAutoCreateRowSorter(true);
        serverTable.setCellSelectionEnabled(false);
        serverTable.setRowSelectionAllowed(true);

        // set table shortcuts
        setHandlers();

        // center the form on screen (initially)
        setLocationRelativeTo(null);

        getRootPane().setDefaultButton(this.bConnect);

        // start fetching the server list
        tSearch.setPlaceholder("Loading server list...");
        tSearch.setEnabled(false);

        getServerListTask = session.getServerListAsync();
        getServerListTask.addPropertyChangeListener(
                new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if ("state".equals(evt.getPropertyName())) {
                    if (evt.getNewValue().equals(StateValue.DONE)) {
                        onServerListDone();
                    }
                }
            }
        });
        getServerListTask.execute();
    }

    private void enableGui() {
        bChangeUser.setEnabled(true);
        bPreferences.setEnabled(true);
        tSearch.setEnabled(true);
        serverTable.setEnabled(true);
        tServerURL.setEnabled(true);
        bConnect.setEnabled(true);
    }

    private void disableGui() {
        bChangeUser.setEnabled(false);
        bPreferences.setEnabled(false);
        tSearch.setEnabled(false);
        serverTable.setEnabled(false);
        tServerURL.setEnabled(false);
        bConnect.setEnabled(false);
    }

    // =============================================================================================
    //                                                                           SERVER LIST FILLING
    // =============================================================================================
    private void onServerListDone() {
        LogUtil.getLogger().log(Level.FINE, "ServerListScreen.onServerListDone");
        try {
            serverList = getServerListTask.get();
            fillServerTable();
            tSearch.setPlaceholder("Search servers...");
            tSearch.setEnabled(true);
            tSearch.selectAll();
            tSearch.requestFocus();
            progress.setVisible(false);

            tableColumnAdjuster.adjustColumns();

        } catch (InterruptedException | ExecutionException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading server list", ex);
            ErrorScreen.show(this, "Could not load server list",
                    "An error occured while loading the server list:<br>" + ex.getMessage(), ex);
            tSearch.setText("Could not load server list.");
        }
    }

    private void fillServerTable() {
        final DefaultTableModel model = (DefaultTableModel) serverTable.getModel();

        // reset sort order
        final RowSorter<? extends TableModel> rowSorter = serverTable.getRowSorter();
        rowSorter.setSortKeys(null);

        // remove all rows
        model.setNumRows(0);
        displayedServerList.clear();
        tServerURL.setText("");

        // add new rows
        final String searchTerm = tSearch.getText().toLowerCase();
        for (final ServerListEntry server : serverList) {
            if (server.name.toLowerCase().contains(searchTerm)) {
                displayedServerList.add(server);
                model.addRow(new Object[]{
                    server.name,
                    server.players,
                    server.maxPlayers,
                    server.uptime,
                    ServerListEntry.toCountryName(server.flag)
                });
            }
        }

        // select first server
        if (model.getRowCount() > 0) {
            serverTable.setRowSelectionInterval(0, 0);
        }
    }

    // =============================================================================================
    //                                                                              JOINING A SERVER
    // =============================================================================================
    private void bConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bConnectActionPerformed
        LogUtil.getLogger().log(Level.FINE, "[Connect]");
        joinSelectedServer();
    }//GEN-LAST:event_bConnectActionPerformed

    private ServerListEntry getSelectedServer() {
        final int[] rowIndex = serverTable.getSelectedRows();
        if (rowIndex.length == 1) {
            final int trueIndex = serverTable.convertRowIndexToModel(rowIndex[0]);
            return displayedServerList.get(trueIndex);
        }
        return null;
    }

    private void joinSelectedServer() {
        LogUtil.getLogger().log(Level.INFO,
                "Fetching details for server: {0}", selectedServer.name);
        final String url = tServerURL.getText();
        final String trimmedInput = url.replaceAll("[\\r\\n\\s]", "");
        final ServerJoinInfo joinInfo = session.getDetailsFromUrl(trimmedInput);
        if (joinInfo == null) {
            ErrorScreen.show(this, "Cannot connect to given server",
                    "Unrecognized server URL. Make sure that you are using the correct link.", null);
        } else if (joinInfo.passNeeded) {
            getServerDetailsTask = session.getServerDetailsAsync(trimmedInput);
            getServerDetailsTask.addPropertyChangeListener(
                    new PropertyChangeListener() {
                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                    if ("state".equals(evt.getPropertyName())) {
                        if (evt.getNewValue().equals(StateValue.DONE)) {
                            onServerDetailsDone();
                        }
                    }
                }
            });
            progress.setVisible(true);
            disableGui();
            getServerDetailsTask.execute();
        } else {
            joinServer(joinInfo);
        }
    }

    private void onServerDetailsDone() {
        LogUtil.getLogger().log(Level.FINE, "onServerDetailsDone");
        try {
            final boolean result = getServerDetailsTask.get();
            if (result) {
                final ServerJoinInfo joinInfo = getServerDetailsTask.getJoinInfo();
                joinServer(joinInfo);
            } else {
                ErrorScreen.show(this, "Cannot connect", "There was a problem fetching server details.", null);
                enableGui();
            }
        } catch (final InterruptedException | ExecutionException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading server details", ex);
            ErrorScreen.show(this, "Error fetching server details", ex.getMessage(), ex);
            enableGui();
        }
    }

    private void joinServer(ServerJoinInfo joinInfo) {
        if (joinInfo == null) {
            throw new NullPointerException("joinInfo");
        }
        if (joinInfo.playerName == null || "".equals(joinInfo.playerName)) {
            joinInfo.playerName = session.getAccount().playerName;
        }
        dispose();
        ClientUpdateScreen.createAndShow(joinInfo);
    }

    // =============================================================================================
    //                                                                           GUI EVENT LISTENERS
    // =============================================================================================
    private void setHandlers() {
        // allow double-clicking servers on the list, to join them
        serverTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    joinSelectedServer();
                }
            }
        });

        // allow pressing <Enter> on the server table to join
        serverTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
        serverTable.getActionMap().put("Enter", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                joinSelectedServer();
            }
        });

        // Fill in the "Server URL" field when a server is selected in the table
        serverTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent lse) {
                if (!lse.getValueIsAdjusting()) {
                    selectedServer = getSelectedServer();
                    if (selectedServer != null) {
                        final String playUrl = session.getPlayUrl(selectedServer.hash);
                        tServerURL.setText(playUrl);
                    }
                }
            }
        });

        // Prevent the server list JTable from permanently stealing keyboard focus
        ActionMap tableActions = serverTable.getActionMap();
        tableActions.put("selectPreviousColumnCell", new TableFocusPreviousComponentAction());
        tableActions.put("selectNextColumnCell", new TableFocusNextComponentAction());

        // Select contents of ServerURL field on-focus
        tServerURL.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                tServerURL.selectAll();
            }

            @Override
            public void focusLost(final FocusEvent e) {
            }
        });
    }

    private static class TableFocusPreviousComponentAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent evt) {
            KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            manager.focusPreviousComponent();
        }
    }

    private static class TableFocusNextComponentAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent evt) {
            KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            manager.focusNextComponent();
        }
    }

    private static class UptimeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 3) {
                final int ticks = (int) value;
                this.setText(ServerListEntry.formatUptime(ticks));
            } else {
                this.setText("");
            }
            return this;
        }
    }

    private void bChangeUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bChangeUserActionPerformed
        LogUtil.getLogger().log(Level.INFO, "[Change User]");
        dispose();
        new SignInScreen().setVisible(true);
    }//GEN-LAST:event_bChangeUserActionPerformed

    private void tSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tSearchKeyReleased
        fillServerTable();
    }//GEN-LAST:event_tSearchKeyReleased

    private void tSearchFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tSearchFocusGained
        tSearch.selectAll();
    }//GEN-LAST:event_tSearchFocusGained

    private void tSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tSearchActionPerformed
        if (serverTable.getSelectedRows().length == 1) {
            joinSelectedServer();
        }
    }//GEN-LAST:event_tSearchActionPerformed

    private void bPreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bPreferencesActionPerformed
        new PreferencesScreen(this).setVisible(true);
    }//GEN-LAST:event_bPreferencesActionPerformed

    private void tServerURLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tServerURLActionPerformed
        joinSelectedServer();
    }//GEN-LAST:event_tServerURLActionPerformed

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

        bChangeUser = new net.classicube.launcher.gui.JNiceLookingButton();
        bPreferences = new net.classicube.launcher.gui.JNiceLookingButton();
        javax.swing.JSeparator separator1 = new javax.swing.JSeparator();
        tSearch = new net.classicube.launcher.gui.PlaceholderTextField();
        serverTableContainer = new javax.swing.JScrollPane();
        serverTable = new javax.swing.JTable();
        javax.swing.JSeparator separator2 = new javax.swing.JSeparator();
        tServerURL = new javax.swing.JTextField();
        bConnect = new net.classicube.launcher.gui.JNiceLookingButton();
        progress = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWidths = new int[] {0, 5, 0, 5, 0};
        layout.rowHeights = new int[] {0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0};
        getContentPane().setLayout(layout);

        bChangeUser.setText("< Change User");
        bChangeUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bChangeUserActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        getContentPane().add(bChangeUser, gridBagConstraints);

        bPreferences.setText("Preferences");
        bPreferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bPreferencesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        getContentPane().add(bPreferences, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(separator1, gridBagConstraints);

        tSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tSearchActionPerformed(evt);
            }
        });
        tSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tSearchFocusGained(evt);
            }
        });
        tSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tSearchKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(tSearch, gridBagConstraints);

        serverTableContainer.setBackground(new java.awt.Color(255, 255, 255));
        serverTableContainer.setMinimumSize(new java.awt.Dimension(300, 150));
        serverTableContainer.setPreferredSize(new java.awt.Dimension(550, 400));

        serverTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Players", "Max", "Uptime", "Location"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        serverTable.setColumnSelectionAllowed(true);
        serverTable.getTableHeader().setReorderingAllowed(false);
        serverTableContainer.setViewportView(serverTable);
        serverTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        serverTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        serverTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        serverTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        serverTable.getColumnModel().getColumn(3).setCellRenderer(new UptimeCellRenderer());
        serverTable.getColumnModel().getColumn(4).setPreferredWidth(60);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        getContentPane().add(serverTableContainer, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(separator2, gridBagConstraints);

        tServerURL.setText("Server URL");
        tServerURL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tServerURLActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(tServerURL, gridBagConstraints);

        bConnect.setText("Connect >");
        bConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bConnectActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        getContentPane().add(bConnect, gridBagConstraints);

        progress.setIndeterminate(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(progress, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private net.classicube.launcher.gui.JNiceLookingButton bChangeUser;
    private net.classicube.launcher.gui.JNiceLookingButton bConnect;
    private net.classicube.launcher.gui.JNiceLookingButton bPreferences;
    private javax.swing.JProgressBar progress;
    private javax.swing.JTable serverTable;
    private javax.swing.JScrollPane serverTableContainer;
    private net.classicube.launcher.gui.PlaceholderTextField tSearch;
    private javax.swing.JTextField tServerURL;
    // End of variables declaration//GEN-END:variables
}
