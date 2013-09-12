package net.classicube.launcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// This class handles persistence/"remembering" for user accounts.
// SignInScreen keeps separate copies of AccountManagers for each GameServiceType.
final class AccountManager {

    // Creates a new account manager for specified service name
    // serviceName is used to separate storage of data from different services
    public AccountManager(String serviceName) {
        if (serviceName == null) {
            throw new NullPointerException("serviceName");
        }
        final Preferences baseNode = Preferences.userNodeForPackage(getClass());
        store = baseNode.node("Accounts").node(serviceName);
    }

    // Loads 
    public void load() {
        try {
            for (String accountName : store.childrenNames()) {
                final UserAccount acct = new UserAccount(store.node(accountName));
                accounts.put(acct.signInUsername.toLowerCase(), acct);
            }
            LogUtil.getLogger().log(Level.FINE, "Loaded {0} accounts", accounts.size());
        } catch (BackingStoreException | IllegalArgumentException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading accounts", ex);
        }
    }

    // Stores all 
    public void store() {
        LogUtil.getLogger().log(Level.FINE, "store");
        clearStore();
        for (UserAccount acct : accounts.values()) {
            acct.store(store.node(acct.signInUsername.toLowerCase()));
        }
    }

    // Removes all stored accounts
    public void clear() {
        LogUtil.getLogger().log(Level.FINE, "clear");
        accounts.clear();
        clearStore();
    }

    public void clearPasswords() {
        LogUtil.getLogger().log(Level.FINE, "clearPasswords");
        for (UserAccount account : accounts.values()) {
            account.password = "";
        }
        store();
    }

    // Tries to find stored UserAccount data for given sign-in name
    public UserAccount findAccount(String signInName) {
        if (signInName == null) {
            throw new NullPointerException("signInName");
        }
        return accounts.get(signInName.toLowerCase());
    }

    // Gets a list of all accounts, ordered by sign-in date, most recent first
    public UserAccount[] getAccountsBySignInDate() {
        final UserAccount[] accountArray = accounts.values().toArray(new UserAccount[0]);
        Arrays.sort(accountArray, UserAccount.getComparator());
        return accountArray;
    }

    private void clearStore() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.ClearStore");
        try {
            for (String accountName : store.childrenNames()) {
                store.node(accountName.toLowerCase()).removeNode();
            }
        } catch (BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error clearing accounts", ex);
        }
    }

    public UserAccount onSignInBegin(String username, String password) {
        if (username == null) {
            throw new NullPointerException("username");
        }
        if (password == null) {
            throw new NullPointerException("password");
        }
        final UserAccount existingAccount = findAccount(username);
        if (existingAccount == null) {
            // new account!
            final UserAccount newAccount = new UserAccount(username, password);
            accounts.put(newAccount.signInUsername.toLowerCase(), newAccount);
            return newAccount;
        } else {
            existingAccount.signInUsername = username;
            existingAccount.password = password;
            return existingAccount;
        }
    }
    private final Preferences store;
    private final HashMap<String, UserAccount> accounts = new HashMap<>();
}
