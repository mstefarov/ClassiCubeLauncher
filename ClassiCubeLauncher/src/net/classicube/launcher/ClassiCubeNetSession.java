/*Java class Copyright (C) HeyMan7 <2013>*/
package net.classicube.launcher;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ClassiCubeNetSession extends GameSession {

    private static final String LoginSecureUri = "http://www.classicube.net/acc/login",
            LogoutUri = "http://www.classicube.net/acc/logout",
            HomepageUri = "http://classicube.net",
            SkinUri = "http://www.classicube.net/skins/",
            ServerListUri = "http://www.classicube.net/api/serverlist",
            WrongUsernameOrPasswordMessage = "Login failed (Username or password may be incorrect)",
            authTokenPattern = "<input id=\"csrf_token\" name=\"csrf_token\" type=\"hidden\" value=\"(.+?)\">",
            loggedInAsPattern = "<a href=\"/acc\" class=\"button\">([a-zA-Z0-9_\\.]{2,16})",
            CookieName = "session";
    private static final Pattern authTokenRegex = Pattern
            .compile(authTokenPattern), loggedInAsRegex = Pattern
            .compile(loggedInAsPattern);

    public ClassiCubeNetSession(UserAccount account) {
        super("ClassiCubeNetSession", account);
        try {
            siteUri = new URI(HomepageUri);
        } catch (URISyntaxException ex) {
            LogUtil.die("Cannot set siteUri", ex);
        }
    }

    @Override
    public SignInTask signInAsync(boolean remember) {
        return new SignInWorker(remember);
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
            this.publish("Connecting to ClassiCube.net");

            // download the login page
            String loginPage = HttpUtil.downloadString(LoginSecureUri);
            if (loginPage == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // See if we're already logged in
            final Matcher loginMatch = loggedInAsRegex.matcher(loginPage);
            if (loginMatch.find()) {
                String actualPlayerName = loginMatch.group(1);
                if (remember
                        && hasCookie(CookieName)
                        && actualPlayerName
                        .equalsIgnoreCase(account.PlayerName)) {
                    // If player is already logged in with the right account:
                    // reuse a previous session
                    account.PlayerName = actualPlayerName;
                    LogUtil.getLogger().log(Level.INFO,
                            "Restored session for {0}", account.PlayerName);
                    storeCookies();
                    return SignInResult.SUCCESS;

                } else {
                    // If we're not supposed to reuse session, if old username
                    // is different,
                    // or if there is no play session cookie set - relog
                    LogUtil.getLogger()
                            .log(Level.INFO,
                            "Switching accounts from {0} to {1}",
                            new Object[]{actualPlayerName,
                        account.PlayerName});
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
                    LogUtil.getLogger()
                            .log(Level.WARNING,
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
            requestStr.append(urlEncode(account.SignInUsername));
            requestStr.append("&password=");
            requestStr.append(urlEncode(account.Password));
            requestStr.append("&csrf_token=");
            requestStr.append(urlEncode(authToken));
            if (remember) {
                requestStr.append("&remember_me=true");
            }
            requestStr.append("&redirect=");
            requestStr.append(urlEncode(HomepageUri));

            // POST our data to the login handler
            this.publish("Signing in...");
            String loginResponse = HttpUtil.uploadString(LoginSecureUri,
                    requestStr.toString());
            if (loginResponse == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // Check for common failure scenarios
            if (loginResponse.contains(WrongUsernameOrPasswordMessage)) {
                return SignInResult.WRONG_USER_OR_PASS;
            }

            // Confirm that we are now logged in
            final Matcher responseMatch = loggedInAsRegex
                    .matcher(loginResponse);
            if (responseMatch.find()) {
                account.PlayerName = responseMatch.group(1);
                return SignInResult.SUCCESS;
            } else {
                LogUtil.getLogger().log(Level.INFO, loginResponse);
                throw new SignInException(
                        "Login failed: Unrecognized response served by ClassiCube.net");
            }
        }
    }

    @Override
    public GetServerListTask getServerListAsync() {
        return new GetServerListWorker();
    }

    private class GetServerListWorker extends GetServerListTask {

        @Override
        protected ServerInfo[] doInBackground() throws Exception {
            LogUtil.getLogger().log(Level.FINE,
                    "ClassiCubeNetGetServerListWorker");
            String serverListString = HttpUtil.downloadString(ServerListUri);

            final ArrayList<ServerInfo> servers = new ArrayList<>();

            JsonArray array = JsonParser.array().from(serverListString);

            for (Object rawRow : array) { //iterate through and add servers to the list
                JsonObject row = (JsonObject)rawRow;
                ServerInfo info = new ServerInfo();
                
                info.address = InetAddress.getByName(row.getString("ip"));
                info.flag = "";
                info.hash = row.getString("hash");
                info.maxPlayers = row.getInt("maxplayers");
                info.name = row.getString("name");
                info.pass = row.getString("mppass");
                info.players = row.getInt("players");
                info.uptime = row.getInt("players");
                info.port = row.getInt("port");
                servers.add(info); //add it
            }
            return servers.toArray(new ServerInfo[0]); //return
        }
    }

    @Override
    public GetServerDetailsTask getServerDetailsAsync(ServerInfo server) {
        return new GetServerDetailsWorker(server);
    }

    private class GetServerDetailsWorker extends GetServerDetailsTask {

        public GetServerDetailsWorker(ServerInfo server) {
            super(server);
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            return true; // just return true, .info is all set
        }
    }

    @Override
    public String getSkinUrl() {
        return SkinUri;
    }

    @Override
    public URI getSiteUri() {
        return siteUri;
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
                final String userToken = "username%3A" + account.SignInUsername + "%00";
                if (cookie != null && cookie.getValue().contains(userToken)) {
                    LogUtil.getLogger().log(Level.FINE,
                            "Loaded saved session for {0}",
                            account.SignInUsername);
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
    private URI siteUri;
}
