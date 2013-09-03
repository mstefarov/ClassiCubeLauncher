package net.classicube.launcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class AccountManager {

    public AccountManager(Preferences store) {
        this.store = store;
    }

    public void Load() throws BackingStoreException {
        for (String accountName : store.childrenNames()) {
            UserAccount acct = new UserAccount(store.node(accountName));
            accounts.put(acct.PlayerName, acct);
        }
    }

    public void Store() throws BackingStoreException {
        Clear();
        for (UserAccount acct : accounts.values()) {
            acct.Store(store.node(acct.SignInUsername));
        }
    }

    public void Clear() throws BackingStoreException {
        for (String accountName : store.childrenNames()) {
            store.node(accountName).removeNode();
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
    Preferences store;
    HashMap<String, UserAccount> accounts = new HashMap<>();
}
