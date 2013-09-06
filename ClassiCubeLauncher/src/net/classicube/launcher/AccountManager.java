package net.classicube.launcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// This class handles persistence/"remembering" for user accounts.
// SignInScreen keeps separate copies of AccountManagers for each GameServiceType.
class AccountManager {

    // Creates a new account manager for specified service name
    // serviceName is used to separate storage of data from different services
    public AccountManager(String serviceName) {
        if (serviceName == null) {
            throw new NullPointerException("serviceName");
        }
        final Preferences baseNode = Preferences.userNodeForPackage(this.getClass());
        this.store = baseNode.node("Accounts").node(serviceName);
    }

    // Loads 
    public void Load() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.Load");
        try {
            for (String accountName : store.childrenNames()) {
                final UserAccount acct = new UserAccount(store.node(accountName));
                accounts.put(acct.SignInUsername.toLowerCase(), acct);
            }
        } catch (BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading accounts", ex);
        }
    }

    // Stores all 
    public void Store() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.Store");
        ClearStore();
        for (UserAccount acct : accounts.values()) {
            acct.Store(store.node(acct.SignInUsername.toLowerCase()));
        }
    }

    // Removes all stored accounts
    public void Clear() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.Clear");
        accounts.clear();
        ClearStore();
    }

    // Tries to find stored UserAccount data for given sign-in name
    public UserAccount FindAccount(String signInName) {
        if (signInName == null) {
            throw new NullPointerException("signInName");
        }
        return accounts.get(signInName.toLowerCase());
    }

    // Gets a list of all accounts, ordered by sign-in date, most recent first
    public UserAccount[] GetAccountsBySignInDate() {
        final UserAccount[] accountArray = accounts.values().toArray(new UserAccount[0]);
        Arrays.sort(accountArray, UserAccountDateComparator.instance);
        return accountArray;
    }

    private void ClearStore() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.ClearStore");
        try {
            for (String accountName : store.childrenNames()) {
                store.node(accountName.toLowerCase()).removeNode();
            }
        } catch (BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error clearing accounts", ex);
        }
    }
    private Preferences store;
    private HashMap<String, UserAccount> accounts = new HashMap<>();
}
