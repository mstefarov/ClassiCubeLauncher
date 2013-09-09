package net.classicube.launcher;

import java.awt.Component;
import java.awt.event.ActionEvent;
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
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SwingWorker.StateValue;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class ServerListScreen extends javax.swing.JFrame {

    public ServerListScreen() {
        LogUtil.getLogger().log(Level.FINE, "ServerListScreen");
        initComponents();

        // set window title
        if (SessionManager.getServiceType() == GameServiceType.ClassiCubeNetService) {
            setTitle("ClassiCube.net servers");
        } else {
            setTitle("Minecraft.net servers");
        }

        // prepare to auto-adjust table columns (when the data arrives)
        serverTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableColumnAdjuster = new TableColumnAdjuster(serverTable);

        // configure table sorting and selection
        serverTable.setAutoCreateRowSorter(true);
        serverTable.setCellSelectionEnabled(false);
        serverTable.setRowSelectionAllowed(true);

        // set table shortcuts
        setTableHandlers();

        // center the form on screen (initially)
        setLocationRelativeTo(null);

        // start fetching the server list
        tSearch.setPlaceholder("Loading server list...");
        tSearch.setEnabled(false);
        getServerListTask = SessionManager.getSession().getServerListAsync();
        getServerListTask.addPropertyChangeListener(
                new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("state".equals(evt.getPropertyName())) {
                    if (evt.getNewValue().equals(StateValue.DONE)) {
                        onServerListDone();
                    }
                }
            }
        });
        getServerListTask.execute();

    }

    private void setTableHandlers() {
        // allow double-clicking servers on the list, to join them
        serverTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
            public void actionPerformed(ActionEvent ae) {
                joinSelectedServer();
            }
        });
    }

    private void disableGui() {
        this.bChangeUser.setEnabled(false);
        this.bPreferences.setEnabled(false);
        this.tSearch.setEnabled(false);
        this.serverTable.setEnabled(false);
        this.tServerURL.setEnabled(false);
        this.bConnect.setEnabled(false);
    }

    class UptimeCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 3) {
                final int ticks = (int) value;
                this.setText(ServerInfo.formatUptime(ticks));
            } else {
                this.setText("");

            }
            return this;
        }
    }

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
            LogUtil.showWarning(ex.toString(), "Problem loading server list");
            tSearch.setText("Could not load server list.");
        }
    }

    private void fillServerTable() {
        final DefaultTableModel model = (DefaultTableModel) serverTable.getModel();

        // reset sort order
        RowSorter rs = serverTable.getRowSorter();
        rs.setSortKeys(null);

        // remove all rows
        model.setNumRows(0);
        displayedServerList.clear();

        // add new rows
        String searchTerm = tSearch.getText().toLowerCase();
        for (ServerInfo server : serverList) {
            if (server.name.toLowerCase().contains(searchTerm)) {
                displayedServerList.add(server);
                model.addRow(new Object[]{
                    server.name,
                    server.players,
                    server.maxPlayers,
                    server.uptime,
                    ServerInfo.toCountryName(server.flag)
                });
            }
        }

        // select first server
        if (model.getRowCount() > 0) {
            serverTable.setRowSelectionInterval(0, 0);
        }
    }

    private ServerInfo getSelectedServer() {
        int[] rowIndex = serverTable.getSelectedRows();
        if (rowIndex.length == 1) {
            int trueIndex = serverTable.convertRowIndexToModel(rowIndex[0]);
            System.out.println("row=" + rowIndex[0] + "  true=" + trueIndex);
            System.out.println("displayedServerList[" + trueIndex + "] = " + displayedServerList.get(trueIndex).name);
            return displayedServerList.get(trueIndex);
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        bChangeUser = new javax.swing.JButton();
        javax.swing.JSeparator separator1 = new javax.swing.JSeparator();
        javax.swing.JSeparator separator2 = new javax.swing.JSeparator();
        bPreferences = new javax.swing.JButton();
        tServerURL = new javax.swing.JTextField();
        bConnect = new javax.swing.JButton();
        serverTableContainer = new javax.swing.JScrollPane();
        serverTable = new javax.swing.JTable();
        progress = new javax.swing.JProgressBar();
        tSearch = new net.classicube.launcher.PlaceholderTextField();

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
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(separator1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(separator2, gridBagConstraints);

        bPreferences.setText("Preferences");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        getContentPane().add(bPreferences, gridBagConstraints);

        tServerURL.setText("Server URL");
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

        progress.setIndeterminate(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(progress, gridBagConstraints);

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

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bChangeUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bChangeUserActionPerformed
        LogUtil.getLogger().log(Level.INFO, "[Change User]");
        EntryPoint.ShowSignInScreen();
    }//GEN-LAST:event_bChangeUserActionPerformed

    private void bConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bConnectActionPerformed
        joinSelectedServer();
    }//GEN-LAST:event_bConnectActionPerformed

    private void tSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tSearchKeyReleased
        System.out.println(tSearch.getText());
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

    void joinSelectedServer() {
        final ServerInfo selectedServer = getSelectedServer();

        LogUtil.getLogger().log(Level.INFO, "Fetching details for server: {0}", selectedServer.name);
        getServerDetailsTask = SessionManager.getSession().getServerDetailsAsync(selectedServer);
        getServerDetailsTask.addPropertyChangeListener(
                new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
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
    }

    private void onServerDetailsDone() {
        LogUtil.getLogger().log(Level.FINE, "ServerListScreen.onServerDetailsDone");
        try {
            final boolean result = getServerDetailsTask.get();
            if (result) {
                SessionManager.serverDetails = getServerDetailsTask.getServerInfo();
                EntryPoint.ShowClientUpdateScreen();
            } else {
                LogUtil.showError("Could not fetch server details.", "Error");
            }
        } catch (InterruptedException | ExecutionException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading server details", ex);
            LogUtil.showWarning(ex.toString(), "Problem loading server details");
            tSearch.setText("Could not load server list.");
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bChangeUser;
    private javax.swing.JButton bConnect;
    private javax.swing.JButton bPreferences;
    private javax.swing.JProgressBar progress;
    private javax.swing.JTable serverTable;
    private javax.swing.JScrollPane serverTableContainer;
    private net.classicube.launcher.PlaceholderTextField tSearch;
    private javax.swing.JTextField tServerURL;
    // End of variables declaration//GEN-END:variables
    private List<ServerInfo> displayedServerList = new ArrayList<>();
    private GameSession.GetServerListTask getServerListTask;
    private GameSession.GetServerDetailsTask getServerDetailsTask;
    private ServerInfo[] serverList;
    private TableColumnAdjuster tableColumnAdjuster;
}
