/*Java class Copyright (C) HeyMan7 <2013>*/
package net.classicube.launcher;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ClassiCubeNetSession extends GameSession {

    private static final String HomepageUri = "http://classicube.net";

    public ClassiCubeNetSession() {
        super("ClassiCubeNetSession");
        try {
            siteUri = new URI(HomepageUri);
        } catch (URISyntaxException ex) {
            LogUtil.die("Cannot set siteUri", ex);
        }
    }
    // =============================================================================================
    //                                                                                       SIGN-IN
    // =============================================================================================
    private static final String LoginSecureUri = "http://www.classicube.net/acc/login",
            LogoutUri = "http://www.classicube.net/acc/logout",
            CookieName = "session",
            WrongUsernameOrPasswordMessage = "Login failed (Username or password may be incorrect)",
            authTokenPattern = "<input id=\"csrf_token\" name=\"csrf_token\" type=\"hidden\" value=\"(.+?)\">",
            loggedInAsPattern = "<a href=\"/acc\" class=\"button\">([a-zA-Z0-9_\\.]{2,16})";
    private static final Pattern authTokenRegex = Pattern.compile(authTokenPattern),
            loggedInAsRegex = Pattern.compile(loggedInAsPattern);

    @Override
    public SignInTask signInAsync(UserAccount account, boolean remember) {
        this.account = account;
        return new SignInWorker(remember);
    }

    @Override
    public GameServiceType getServiceType() {
        return GameServiceType.ClassiCubeNetService;
    }

    // Asynchronously try signing in our user
    private class SignInWorker extends SignInTask {

        public SignInWorker(boolean remember) {
            super(remember);
        }

        @Override
        protected SignInResult doInBackground() throws Exception {
            LogUtil.getLogger().log(Level.FINE, "ClassiCubeNetSignInWorker");
            boolean restoredSession = false;
            try {
                restoredSession = loadSessionCookie(remember);
            } catch (BackingStoreException ex) {
                LogUtil.getLogger().log(Level.WARNING,
                        "Error restoring session", ex);
            }

            // "this.publish" can be used to send text status updates to the GUI
            // (not hooked up)
            publish("Connecting to ClassiCube.net");

            // download the login page
            String loginPage = HttpUtil.downloadString(LoginSecureUri);
            if (loginPage == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // See if we're already logged in
            final Matcher loginMatch = loggedInAsRegex.matcher(loginPage);
            if (loginMatch.find()) {
                final String actualPlayerName = loginMatch.group(1);
                if (remember
                        && hasCookie(CookieName)
                        && actualPlayerName.equalsIgnoreCase(account.playerName)) {
                    // If player is already logged in with the right account:
                    // reuse a previous session
                    account.playerName = actualPlayerName;
                    LogUtil.getLogger().log(Level.INFO,
                            "Restored session for {0}", account.playerName);
                    storeCookies();
                    return SignInResult.SUCCESS;

                } else {
                    // If we're not supposed to reuse session, if old username
                    // is different,
                    // or if there is no play session cookie set - relog
                    LogUtil.getLogger().log(Level.INFO,
                            "Switching accounts from {0} to {1}",
                            new Object[]{actualPlayerName, account.playerName});
                    HttpUtil.downloadString(LogoutUri);
                    clearCookies();
                    loginPage = HttpUtil.downloadString(LoginSecureUri);
                }
            }

            // Extract authenticityToken from the login page
            final Matcher authTokenMatch = authTokenRegex.matcher(loginPage);
            if (!authTokenMatch.find()) {
                if (restoredSession) {
                    // restoring session failed; log out and retry
                    HttpUtil.downloadString(LogoutUri);
                    clearCookies();
                    LogUtil.getLogger().log(Level.WARNING,
                            "Unrecognized login form served by ClassiCube.net; retrying.");

                } else {
                    // something unexpected happened, panic!
                    LogUtil.getLogger().log(Level.INFO, loginPage);
                    throw new SignInException(
                            "Login failed: Unrecognized login form served by ClassiCube");
                }
            }

            // Built up a login request
            final String authToken = authTokenMatch.group(1);
            final StringBuilder requestStr = new StringBuilder();
            requestStr.append("username=");
            requestStr.append(urlEncode(account.signInUsername));
            requestStr.append("&password=");
            requestStr.append(urlEncode(account.password));
            requestStr.append("&csrf_token=");
            requestStr.append(urlEncode(authToken));
            if (remember) {
                requestStr.append("&remember_me=true");
            }
            requestStr.append("&redirect=");
            requestStr.append(urlEncode(HomepageUri));

            // POST our data to the login handler
            publish("Signing in...");
            final String loginResponse = HttpUtil.uploadString(LoginSecureUri, requestStr.toString());
            if (loginResponse == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // Check for common failure scenarios
            if (loginResponse.contains(WrongUsernameOrPasswordMessage)) {
                return SignInResult.WRONG_USER_OR_PASS;
            }

            // Confirm that we are now logged in
            final Matcher responseMatch = loggedInAsRegex.matcher(loginResponse);
            if (responseMatch.find()) {
                account.playerName = responseMatch.group(1);
                return SignInResult.SUCCESS;
            } else {
                LogUtil.getLogger().log(Level.INFO, loginResponse);
                throw new SignInException(
                        "Login failed: Unrecognized response served by ClassiCube.net");
            }
        }
    }

    //have not checked loadSessionCookie, presume it works fine (It remembers me)
    // Tries to restore previous session (if possible)
    private boolean loadSessionCookie(boolean remember)
            throws BackingStoreException {
        LogUtil.getLogger().log(Level.FINE, "ClassiCubeNetSession.loadSessionCookie");
        clearCookies();
        if (store.childrenNames().length > 0) {
            if (remember) {
                loadCookies();
                final HttpCookie cookie = super.getCookie(CookieName);
                final String userToken = "username%3A" + account.signInUsername + "%00";
                if (cookie != null && cookie.getValue().contains(userToken)) {
                    LogUtil.getLogger().log(Level.FINE,
                            "Loaded saved session for {0}", account.signInUsername);
                    return true;
                } else {
                    LogUtil.getLogger().log(Level.FINE,
                            "Discarded saved session (username mismatch).");
                }
            } else {
                LogUtil.getLogger().log(Level.FINE,
                        "Discarded a saved session.");
            }
        } else {
            LogUtil.getLogger().log(Level.FINE, "No session saved.");
        }
        return false;
    }
    // =============================================================================================
    //                                                                                   SERVER LIST
    // =============================================================================================
    private static final String ServerListUri = "http://www.classicube.net/api/serverlist";

    @Override
    public GetServerListTask getServerListAsync() {
        return new GetServerListWorker();
    }

    private class GetServerListWorker extends GetServerListTask {

        @Override
        protected ServerListEntry[] doInBackground() throws Exception {
            LogUtil.getLogger().log(Level.FINE, "ClassiCubeNetGetServerListWorker");
            final String serverListString = HttpUtil.downloadString(ServerListUri);

            final ArrayList<ServerListEntry> servers = new ArrayList<>();

            final JsonArray array = JsonParser.array().from(serverListString);

            for (Object rawRow : array) { //iterate through and add servers to the list
                final JsonObject row = (JsonObject) rawRow;
                final ServerListEntry info = new ServerListEntry();

                info.flag = "";
                info.hash = row.getString("hash");
                info.maxPlayers = row.getInt("maxplayers");
                info.name = row.getString("name");
                info.players = row.getInt("players");
                info.uptime = row.getInt("players");
                servers.add(info); //add it
            }
            return servers.toArray(new ServerListEntry[0]); //return
        }
    }
    // =============================================================================================
    //                                                                              DETAILS-FROM-URL
    // =============================================================================================
    // http://www.classicube.net/server/play/583c911d2f9f437af451a144b493d0cf
    private static final String playHashUrlPattern = "^http://" // scheme
            + "www.classicube.net/server/play/" // host+path
            + "([0-9a-fA-F]{28,32})/?" + // hash
            "(\\?override=(true|1))?$"; // override
    private static final String ipPortUrlPattern = "^https?://" // scheme
            + "www.classicube.net/server/play/?" // host+path
            + "\\?ip=(localhost|(\\d{1,3}\\.){3}\\d{1,3}|([a-zA-Z0-9\\-]+\\.)+([a-zA-Z0-9\\-]+))" // host/IP
            + "&port=(\\d{1,5})$"; // port
    private static final Pattern playHashUrlRegex = Pattern.compile(playHashUrlPattern),
            ipPortUrlRegex = Pattern.compile(ipPortUrlPattern);

    @Override
    public ServerJoinInfo getDetailsFromUrl(String url) {
        ServerJoinInfo result = super.getDetailsFromDirectUrl(url);
        if (result != null) {
            return result;
        }

        Matcher playHashUrlMatch = playHashUrlRegex.matcher(url);
        if (playHashUrlMatch.matches()) {
            result = new ServerJoinInfo();
            result.signInNeeded = true;
            result.hash = playHashUrlMatch.group(1);
            String overrideString = playHashUrlMatch.group(3);
            if ("1".equals(overrideString) || "true".equals(overrideString)) {
                result.override = true;
            }
            return result;
        }

        Matcher ipPortUrlMatch = ipPortUrlRegex.matcher(url);
        if (ipPortUrlMatch.matches()) {
            result = new ServerJoinInfo();
            result.signInNeeded = true;
            try {
                result.address = InetAddress.getByName(ipPortUrlMatch.group(1));
            } catch (UnknownHostException ex) {
                return null;
            }
            String portNum = ipPortUrlMatch.group(5);
            if (portNum != null && portNum.length() > 0) {
                try {
                    result.port = Integer.parseInt(portNum);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return result;
        }
        return null;
    }
    // =============================================================================================
    //                                                                                           ETC
    // =============================================================================================
    private static final String SkinUrl = "http://www.classicube.net/skins/",
            PlayUrl = "http://www.classicube.net/server/play/";

    @Override
    public String getSkinUrl() {
        return SkinUrl;
    }

    @Override
    public URI getSiteUri() {
        return siteUri;
    }

    @Override
    public String getPlayUrl(String hash) {
        return PlayUrl + hash;
    }
    private URI siteUri;
}
