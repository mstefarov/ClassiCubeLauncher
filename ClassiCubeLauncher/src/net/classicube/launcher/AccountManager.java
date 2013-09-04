package net.classicube.launcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class AccountManager {

    public AccountManager(String keyName) {
        this.store = Preferences.userNodeForPackage(this.getClass()).node(keyName);
    }
    
    public UserAccount Add(String username, String password){
        UserAccount account = new UserAccount(username, password);
        accounts.put(username.toLowerCase(), account);
        return account;
    }

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

    public void Store() {
        LogUtil.Log(Level.FINE, "AccountManager.Store");
        Clear();
        for (UserAccount acct : accounts.values()) {
            acct.Store(store.node(acct.SignInUsername.toLowerCase()));
        }
    }

    public void Clear() {
        LogUtil.Log(Level.FINE, "AccountManager.Clear");
        try {
            for (String accountName : store.childrenNames()) {
                store.node(accountName.toLowerCase()).removeNode();
            }
        } catch (BackingStoreException ex) {
            LogUtil.Log(Level.SEVERE, "Error clearing accounts", ex);
        }
    }

    public UserAccount FindAccount(String signInName) {
        return accounts.get(signInName.toLowerCase());
    }

    public UserAccount[] GetAccountsBySignInDate() {
        UserAccount[] accountArray = accounts.values().toArray(new UserAccount[0]);
        Arrays.sort(accountArray, UserAccountDateComparator.instance);
        return accountArray;
    }
    private Preferences store;
    private HashMap<String, UserAccount> accounts = new HashMap<>();
}
