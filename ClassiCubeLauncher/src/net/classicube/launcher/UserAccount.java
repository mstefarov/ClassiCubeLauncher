package net.classicube.launcher;

import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.prefs.Preferences;

// Stores metadata about a user account.
// Handled by AccountManager.
final class UserAccount {
    public String signInUsername;
    public String playerName;
    public String password;
    public Date signInDate;

    public UserAccount(String username, String password) {
        if (username == null) {
            throw new NullPointerException("username");
        }
        if (password == null) {
            throw new NullPointerException("password");
        }
        signInUsername = username;
        playerName = username;
        this.password = password;
        signInDate = new Date(0);
    }

    // Loads all information from a given Preferences node
    public UserAccount(Preferences prefs) {
        if (prefs == null) {
            throw new NullPointerException("prefs");
        }
        signInUsername = prefs.get("SignInUsername", null);
        playerName = prefs.get("PlayerName", null);
        password = prefs.get("Password", "");
        final long dateTicks = prefs.getLong("SignInDate", 0);
        signInDate = new Date(dateTicks);
        if (signInUsername == null || playerName == null || password == null) {
            LogUtil.getLogger().log(Level.WARNING, "Could not parse pref as a sign-in account.");
            throw new IllegalArgumentException("Pref could not be parsed");
        }
    }

    // Stores all information into a given Preferences node
    public void store(Preferences prefs) {
        if (prefs == null) {
            throw new NullPointerException("prefs");
        }
        prefs.put("SignInUsername", signInUsername);
        prefs.put("PlayerName", playerName);
        prefs.put("Password", password);
        prefs.putLong("SignInDate", signInDate.getTime());
    }

    private static class UserAccountDateComparator implements Comparator<UserAccount> {

        private UserAccountDateComparator() {
        }

        @Override
        public int compare(UserAccount o1, UserAccount o2) {
            final Long delta = o2.signInDate.getTime() - o1.signInDate.getTime();
            return delta.intValue();
        }
    }
    private static final UserAccountDateComparator comparatorInstance = new UserAccountDateComparator();

    public static Comparator<UserAccount> getComparator() {
        return comparatorInstance;
    }
}
