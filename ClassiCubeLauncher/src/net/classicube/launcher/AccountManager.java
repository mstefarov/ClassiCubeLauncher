package net.classicube.launcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// This class handles persistence/"remembering" for user accounts.
// SignInScreen keeps separate copies of AccountManagers for each GameService.
class AccountManager {

    // Creates a new account manager for specified service name
    // serviceName is used to separate storage of data from different services
    public AccountManager(String serviceName) {
        this.store = Preferences.userNodeForPackage(this.getClass()).node("Accounts").node(serviceName);
    }

    // Loads 
    public void Load() {
        LogUtil.Log(Level.FINE, "AccountManager.Load");
        try {
            for (String accountName : store.childrenNames()) {
                UserAccount acct = new UserAccount(store.node(accountName));
                accounts.put(acct.SignInUsername.toLowerCase(), acct);
            }
        } catch (BackingStoreException ex) {
            LogUtil.Log(Level.SEVERE, "Error loading accounts", ex);
        }
    }

    // Stores all 
    public void Store() {
        LogUtil.Log(Level.FINE, "AccountManager.Store");
        ClearStore();
        for (UserAccount acct : accounts.values()) {
            acct.Store(store.node(acct.SignInUsername.toLowerCase()));
        }
    }

    // Removes all stored accounts
    public void Clear() {
        LogUtil.Log(Level.FINE, "AccountManager.Clear");
        accounts.clear();
        ClearStore();
    }

    // Tries to find stored UserAccount data for given sign-in name
    public UserAccount FindAccount(String signInName) {
        return accounts.get(signInName.toLowerCase());
    }

    // Gets a list of all accounts, ordered by sign-in date, most recent first
    public UserAccount[] GetAccountsBySignInDate() {
        UserAccount[] accountArray = accounts.values().toArray(new UserAccount[0]);
        Arrays.sort(accountArray, UserAccountDateComparator.instance);
        return accountArray;
    }

    private void ClearStore() {
        LogUtil.Log(Level.FINE, "AccountManager.ClearStore");
        try {
            for (String accountName : store.childrenNames()) {
                store.node(accountName.toLowerCase()).removeNode();
            }
        } catch (BackingStoreException ex) {
            LogUtil.Log(Level.SEVERE, "Error clearing accounts", ex);
        }
    }
    private Preferences store;
    private HashMap<String, UserAccount> accounts = new HashMap<>();
}
