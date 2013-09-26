package net.classicube.launcher;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.prefs.Preferences;

// Stores metadata about a user account.
// Handled by AccountManager.
public final class UserAccount {

    public String signInUsername;
    public String playerName;
    public String password;
    public Date signInDate;

    public UserAccount(final String username, final String password) {
        if (username == null) {
            throw new NullPointerException("username");
        }
        if (password == null) {
            throw new NullPointerException("password");
        }
        this.signInUsername = username;
        this.playerName = username;
        this.password = password;
        this.signInDate = new Date(0);
    }

    // Loads all information from a given Preferences node
    public UserAccount(final Preferences prefs) {
        if (prefs == null) {
            throw new NullPointerException("prefs");
        }
        this.signInUsername = prefs.get("SignInUsername", null);
        this.playerName = prefs.get("PlayerName", null);
        if (Prefs.getRememberPasswords()) {
            this.password = prefs.get("Password", "");
        } else {
            this.password = "";
        }
        final long dateTicks = prefs.getLong("SignInDate", 0);
        this.signInDate = new Date(dateTicks);
        if (this.signInUsername == null || this.playerName == null || this.password == null) {
            LogUtil.getLogger().log(Level.WARNING, "Could not parse pref as a sign-in account.");
            throw new IllegalArgumentException("Pref could not be parsed");
        }
    }

    // Stores all information into a given Preferences node
    public void store(final Preferences prefs) {
        if (prefs == null) {
            throw new NullPointerException("prefs");
        }
        prefs.put("SignInUsername", this.signInUsername);
        prefs.put("PlayerName", this.playerName);
        if (Prefs.getRememberPasswords()) {
            prefs.put("Password", this.password);
        } else {
            prefs.put("Password", "");
        }
        prefs.putLong("SignInDate", this.signInDate.getTime());
    }

    // Gets a comparator that sorts servers by signInDate (most recent first)
    public static Comparator<UserAccount> getUptimeComparator() {
        return comparatorInstance;
    }
    private static final UserAccountDateComparator comparatorInstance = new UserAccountDateComparator();

    private static class UserAccountDateComparator
            implements Comparator<UserAccount>, Serializable {
        @Override
        public int compare(final UserAccount o1, final UserAccount o2) {
            final Long delta = o2.signInDate.getTime() - o1.signInDate.getTime();
            return delta.intValue();
        }
    }
}
