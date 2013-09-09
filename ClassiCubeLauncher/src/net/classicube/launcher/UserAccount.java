package net.classicube.launcher;

import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.prefs.Preferences;

// Stores metadata about a user account.
// Handled by AccountManager.
class UserAccount {

    public UserAccount(String username, String password) {
        if (username == null) {
            throw new NullPointerException("username");
        }
        if (password == null) {
            throw new NullPointerException("password");
        }
        SignInUsername = username;
        PlayerName = username;
        Password = password;
        SignInDate = new Date(0);
    }

    // Loads all information from a given Preferences node
    public UserAccount(Preferences prefs) {
        if (prefs == null) {
            throw new NullPointerException("prefs");
        }
        SignInUsername = prefs.get("SignInUsername", null);
        PlayerName = prefs.get("PlayerName", null);
        Password = prefs.get("Password", null);
        final long dateTicks = prefs.getLong("SignInDate", 0);
        SignInDate = new Date(dateTicks);
        if (SignInUsername == null || PlayerName == null || Password == null) {
            LogUtil.getLogger().log(Level.WARNING, "Could not parse pref as a sign-in account.");
            throw new IllegalArgumentException("Pref could not be parsed");
        }
    }

    // Stores all information into a given Preferences node
    public void Store(Preferences prefs) {
        if (prefs == null) {
            throw new NullPointerException("prefs");
        }
        prefs.put("SignInUsername", SignInUsername);
        prefs.put("PlayerName", PlayerName);
        prefs.put("Password", Password);
        prefs.putLong("SignInDate", SignInDate.getTime());
    }
    public String SignInUsername;
    public String PlayerName;
    public String Password;
    public Date SignInDate;

    
    private static class UserAccountDateComparator implements Comparator<UserAccount> {

        private UserAccountDateComparator() {
        }

        @Override
        public int compare(UserAccount o1, UserAccount o2) {
            final Long delta = o2.SignInDate.getTime() - o1.SignInDate.getTime();
            return delta.intValue();
        }
    }
    private static final UserAccountDateComparator comparatorInstance = new UserAccountDateComparator();

    public static Comparator<UserAccount> getComparator() {
        return comparatorInstance;
    }
}
