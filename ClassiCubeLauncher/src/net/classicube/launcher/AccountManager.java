package net.classicube.launcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// Handles persistence/"remembering" of user account information for a given GameServiceType.
// SessionManager creates an instance of AccountManager in SessionManager.selectService().
public final class AccountManager {

    private final Preferences store;
    private final HashMap<String, UserAccount> accounts = new HashMap<>();
    private static final String ACCOUNTS_NODE_NAME = "Accounts";

    // Creates a new account manager for specified service name
    // serviceName is used to separate storage of data from different services
    public AccountManager(final GameServiceType service) {
        final Preferences baseNode = Preferences.userNodeForPackage(getClass());
        this.store = baseNode.node(service.name()).node(ACCOUNTS_NODE_NAME);
    }

    // Loads all accounts from preferences
    public void load() {
        if (!Prefs.getRememberUsers()) {
            return;
        }
        try {
            for (final String accountName : this.store.childrenNames()) {
                final Preferences accountNode = this.store.node(accountName);
                try {
                    final UserAccount acct = new UserAccount(accountNode);
                    if (acct.signInDate.getTime() == 0) {
                        // AccountManager used to store all accounts as soon as [Sign In] button was
                        // pressed. Now we only store after a successful sign-in. This avoids typos
                        // from being saved forever.
                        LogUtil.getLogger().log(Level.WARNING,
                                "Skipping user account {0} (never signed into).", acct.signInUsername);
                        continue;
                    }
                    this.accounts.put(acct.signInUsername.toLowerCase(), acct);
                } catch (final IllegalArgumentException ex) {
                    LogUtil.getLogger().log(Level.SEVERE, "Error loading an account", ex);
                    accountNode.removeNode();
                }
            }
            LogUtil.getLogger().log(Level.FINE, "Loaded {0} accounts", this.accounts.size());
        } catch (BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading accounts", ex);
        }
    }

    // Stores ALL remembered accounts
    public void storeAll() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.store");
        this.clearStore();
        if (!Prefs.getRememberUsers()) {
            return;
        }
        for (final UserAccount acct : this.accounts.values()) {
            acct.store(this.store.node(acct.signInUsername.toLowerCase()));
        }
    }

    // Stores given account
    public void store(final UserAccount acct) {
        if (!this.accounts.containsValue(acct)) {
            this.accounts.put(acct.signInUsername.toLowerCase(), acct);
        }
        acct.store(this.store.node(acct.signInUsername.toLowerCase()));
    }

    // Erases all accounts
    public void clear() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.clear");
        this.accounts.clear();
        this.clearStore();
    }

    // Erases all accounts from memory and preferences store
    private void clearStore() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.clearStore");
        try {
            for (final String accountName : this.store.childrenNames()) {
                this.store.node(accountName.toLowerCase()).removeNode();
            }
        } catch (final BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error clearing accounts", ex);
        }
    }

    // Clears passwords (sets them to empty string) for all accounts
    public void clearPasswords() {
        LogUtil.getLogger().log(Level.FINE, "AccountManager.clearPasswords");
        for (final UserAccount account : this.accounts.values()) {
            account.password = "";
        }
        this.storeAll();
    }

    // Returns true if there is at least one account stored.
    public boolean hasAccounts() {
        return !this.accounts.isEmpty();
    }

    // Returns true if at least one account has a password stored.
    public boolean hasPasswords() {
        for (final UserAccount account : this.accounts.values()) {
            if (!account.password.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Tries to find stored UserAccount data for given sign-in name (case-insensitive)
    public UserAccount findAccount(final String signInName) {
        if (signInName == null) {
            throw new NullPointerException("signInName");
        }
        return this.accounts.get(signInName.toLowerCase());
    }

    // Gets a list of all accounts, ordered by sign-in date, most recent first
    public UserAccount[] getAccountsBySignInDate() {
        final UserAccount[] accountArray;
        accountArray = this.accounts.values().toArray(new UserAccount[this.accounts.size()]);
        Arrays.sort(accountArray, UserAccount.getUptimeComparator());
        return accountArray;
    }

    // Either creates a new UserAccount or retrieves an existing UserAccount for given username.
    // Called from SignInScreen, right after [Sign In] button is pressed.
    public UserAccount onSignInBegin(final String username, final String password) {
        if (username == null) {
            throw new NullPointerException("username");
        }
        if (password == null) {
            throw new NullPointerException("password");
        }
        final UserAccount existingAccount = this.findAccount(username);
        if (existingAccount == null) {
            // new account!
            final UserAccount newAccount = new UserAccount(username, password);
            return newAccount;
        } else {
            existingAccount.signInUsername = username;
            existingAccount.password = password;
            return existingAccount;
        }
    }
}
