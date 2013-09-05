package net.classicube.launcher;

import java.util.Date;
import java.util.logging.Level;
import java.util.prefs.Preferences;

// Stores metadata about a user account.
// Handled by AccountManager.
class UserAccount {
    public UserAccount(String username, String password){
        if(username == null){
            throw new IllegalArgumentException("username may not be null");
        }
        if(password == null){
            throw new IllegalArgumentException("password may not be null");
        }
        SignInUsername = username;
        PlayerName = username;
        Password = password;
        SignInDate = new Date(0);
    }
    
    // Loads all information from a given Preferences node
    public UserAccount(Preferences prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException("prefs may not be null");
        }
        SignInUsername = prefs.get("SignInUsername", null);
        PlayerName = prefs.get("PlayerName", null);
        Password = prefs.get("Password", null);
        final long dateTicks = prefs.getLong("SignInDate", 0);
        SignInDate = new Date(dateTicks);
        if (SignInUsername == null || PlayerName == null || Password == null) {
            LogUtil.Log(Level.WARNING, "Could not parse pref as a sign-in account.");
            throw new IllegalArgumentException();
        }
    }

    // Stores all information into a given Preferences node
    public void Store(Preferences prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException("prefs may not be null");
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
}
