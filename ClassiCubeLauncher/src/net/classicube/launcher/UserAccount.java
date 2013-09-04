package net.classicube.launcher;

import java.util.Date;
import java.util.logging.Level;
import java.util.prefs.Preferences;

class UserAccount {

    public UserAccount(Preferences pref) {
        SignInUsername = pref.get("SignInUsername", null);
        PlayerName = pref.get("PlayerName", null);
        Password = pref.get("Password", null);
        long dateTicks = pref.getLong("SignInDate", 0);
        SignInDate = new Date(dateTicks);
        if (SignInUsername == null || PlayerName == null || Password == null) {
            LogUtil.Log(Level.WARNING, "Could not parse pref as a sign-in account.");
            throw new IllegalArgumentException();
        }
    }

    public void Store(Preferences pref) {
        pref.put("SignInUsername", SignInUsername);
        pref.put("PlayerName", PlayerName);
        pref.put("Password", Password);
        pref.putLong("SignInDate", SignInDate.getTime());
    }
    public String SignInUsername;
    public String PlayerName;
    public String Password;
    public Date SignInDate;
}
