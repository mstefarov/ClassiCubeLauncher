package net.classicube.launcher;

import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftNetService extends GameService {

    static final String LoginSecureUri = "https://minecraft.net/login";
    static final String LogoutUri = "http://minecraft.net/logout";
    static final String MigratedAccountMessage = "Your account has been migrated";
    static final String WrongUsernameOrPasswordMessage = "Oops, unknown username or password.";
    static final String authTokenPattern = "<input type=\"hidden\" name=\"authenticityToken\" value=\"([0-9a-f]+)\">";
    static final String loggedInAsPattern = "<span class=\"logged-in\">\\s*Logged in as ([a-zA-Z0-9_\\.]{2,16})";
    static final Pattern authTokenRegex = Pattern.compile(authTokenPattern);
    static final Pattern loggedInAsRegex = Pattern.compile(loggedInAsPattern);
    static final String CookieName = "PLAY_SESSION";

    public MinecraftNetService(UserAccount acct) {
        super(acct);
        Preferences usrPrefs = Preferences.systemNodeForPackage(MinecraftNetService.class);
        prefs = usrPrefs.node("MinecraftNetService");
    }

    @Override
    public SignInResult signIn(boolean remember) throws SignInException {
        final String logPrefix = "MinecraftNetService.signIn: ";
        boolean restoredSession = false;
        try {
            restoredSession = loadSessionCookie(remember);
        } catch (BackingStoreException ex) {
            LogUtil.Log(Level.WARNING, "Error restoring session", ex);
        }

        // download the login page
        String loginPage = downloadString(LoginSecureUri);
        LogUtil.Log(Level.FINE, loginPage);

        // See if we're already logged in
        Matcher loginMatch = loggedInAsRegex.matcher(loginPage);
        if (loginMatch.find()) {
            String actualPlayerName = loginMatch.group(1);
            if (remember && hasCookie(CookieName)
                    && actualPlayerName.equalsIgnoreCase(account.PlayerName)) {
                // If player is already logged in with the right account: reuse a previous session
                account.PlayerName = actualPlayerName;
                LogUtil.Log(Level.INFO, logPrefix + "Restored session for " + account.PlayerName);
                storeCookies(prefs.node("Cookies"));
                return SignInResult.SUCCESS;
            } else {
                // If we're not supposed to reuse session, if old username is different,
                // or if there is no play session cookie set - relog
                LogUtil.Log(Level.INFO, logPrefix + "Switching accounts from "
                        + actualPlayerName + " to " + account.PlayerName);
                downloadString(LogoutUri);
                clearCookies();
                loginPage = downloadString(LoginSecureUri);
            }
        }

        // Extract authenticityToken from the login page
        Matcher authTokenMatch = authTokenRegex.matcher(loginPage);
        if (!authTokenMatch.find()) {
            if (restoredSession) {
                // restoring session failed; log out and retry
                downloadString(LogoutUri);
                clearCookies();
                LogUtil.Log(Level.WARNING, logPrefix + "Unrecognized login form served by minecraft.net; retrying.");
            } else {
                // something unexpected happened, panic!
                throw new SignInException("Login failed: Unrecognized login form served by minecraft.net");
            }
        }

        // Built up a login request
        String authToken = authTokenMatch.group(1);
        StringBuilder requestStr = new StringBuilder();
        requestStr.append("username=");
        requestStr.append(UrlEncode(account.SignInUsername));
        requestStr.append("&password=");
        requestStr.append(UrlEncode(account.Password));
        requestStr.append("&authenticityToken=");
        requestStr.append(UrlEncode(authToken));
        if (remember) {
            requestStr.append("&remember=true");
        }

        // POST our data to the login handler
        String loginResponse = uploadString(LoginSecureUri, requestStr.toString());

        // Check for common failure scenarios
        if (loginResponse.contains(WrongUsernameOrPasswordMessage)) {
            return SignInResult.WRONG_USER_OR_PASS;
        } else if (loginResponse.contains(MigratedAccountMessage)) {
            return SignInResult.MIGRATED_ACCOUNT;
        }

        // Confirm tha we are now logged in
        loginMatch = loggedInAsRegex.matcher(loginPage);
        if (loginMatch.find()) {
            account.PlayerName = loginMatch.group(1);
            return SignInResult.SUCCESS;
        } else {
            throw new SignInException("Login failed: Unrecognized response served by minecraft.net");
        }
    }

    @Override
    public ServerInfo[] getServerList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getServerPass(ServerInfo server) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void storeSession(Preferences pref) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void loadSession(Preferences pref) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getSkinUrl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URI getSiteUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    boolean loadSessionCookie(boolean remember) throws BackingStoreException {
        final String logPrefix = "MinecraftNetService.loadSessionCookie: ";
        clearCookies();
        if (prefs.nodeExists("Cookies")) {
            if (remember) {
                loadCookies(prefs.node("Cookies"));
                HttpCookie cookie = super.getCookie(CookieName);
                String userToken = "username%3A" + account.SignInUsername + "%00";
                if (cookie != null && cookie.getValue().contains(userToken)) {
                    LogUtil.Log(Level.FINE,
                            logPrefix + "Loaded saved session for " + account.SignInUsername);
                    return true;
                } else {
                    LogUtil.Log(Level.FINE,
                            logPrefix + "Discarded saved session (username mismatch).");
                }
            } else {
                LogUtil.Log(Level.FINE, logPrefix + "Discarded a saved session.");
            }
        } else {
            LogUtil.Log(Level.FINE, logPrefix + "No session saved.");
        }
        return false;
    }
    Preferences prefs;
}
