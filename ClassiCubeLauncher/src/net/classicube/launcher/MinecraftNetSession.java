package net.classicube.launcher;

import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MinecraftNetSession extends GameSession {

    private static final String LoginSecureUri = "https://minecraft.net/login",
            LogoutUri = "http://minecraft.net/logout",
            HomepageUri = "http://minecraft.net",
            SkinUri = "http://s3.amazonaws.com/MinecraftSkins/",
            ServerListUri = "http://minecraft.net/classic/list",
            PlayUri = "http://minecraft.net/classic/play/",
            MigratedAccountMessage = "Your account has been migrated",
            WrongUsernameOrPasswordMessage = "Oops, unknown username or password.",
            authTokenPattern = "<input type=\"hidden\" name=\"authenticityToken\" value=\"([0-9a-f]+)\">",
            loggedInAsPattern = "<span class=\"logged-in\">\\s*Logged in as ([a-zA-Z0-9_\\.]{2,16})",
            serverNamePattern = "<a href=\"/classic/play/([0-9a-f]+)\">([^<]+)</a>",
            otherServerDataPattern = "<td>(\\d+)</td>[^<]+<td>(\\d+)</td>[^<]+<td>([^<]+)</td>[^<]+.+url\\(/images/flags/([a-z]+).png\\)",
            appletParamPattern = "<param name=\"(\\w+)\" value=\"(.+)\">",
            CookieName = "PLAY_SESSION";
    private static final Pattern authTokenRegex = Pattern.compile(authTokenPattern),
            loggedInAsRegex = Pattern.compile(loggedInAsPattern),
            serverNameRegex = Pattern.compile(serverNamePattern),
            otherServerDataRegex = Pattern.compile(otherServerDataPattern),
            appletParamRegex = Pattern.compile(appletParamPattern);

    public MinecraftNetSession(UserAccount account) {
        super("MinecraftNetSession", account);
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
            LogUtil.getLogger().log(Level.FINE, "MinecraftNetSignInWorker");
            boolean restoredSession = false;
            try {
                restoredSession = loadSessionCookie(remember);
            } catch (BackingStoreException ex) {
                LogUtil.getLogger().log(Level.WARNING, "Error restoring session", ex);
            }

            // "this.publish" can be used to send text status updates to the GUI (not hooked up)
            this.publish("Connecting to Minecraft.net");

            // download the login page
            String loginPage = HttpUtil.downloadString(LoginSecureUri);
            if(loginPage == null){
                return SignInResult.CONNECTION_ERROR;
            }

            // See if we're already logged in
            final Matcher loginMatch = loggedInAsRegex.matcher(loginPage);
            if (loginMatch.find()) {
                String actualPlayerName = loginMatch.group(1);
                if (remember && hasCookie(CookieName)
                        && actualPlayerName.equalsIgnoreCase(account.PlayerName)) {
                    // If player is already logged in with the right account: reuse a previous session
                    account.PlayerName = actualPlayerName;
                    LogUtil.getLogger().log(Level.INFO, "Restored session for {0}", account.PlayerName);
                    storeCookies();
                    return SignInResult.SUCCESS;

                } else {
                    // If we're not supposed to reuse session, if old username is different,
                    // or if there is no play session cookie set - relog
                    LogUtil.getLogger().log(Level.INFO, "Switching accounts from {0} to {1}",
                            new Object[]{actualPlayerName, account.PlayerName});
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
                            "Unrecognized login form served by minecraft.net; retrying.");

                } else {
                    // something unexpected happened, panic!
                    LogUtil.getLogger().log(Level.INFO, loginPage);
                    throw new SignInException("Login failed: Unrecognized login form served by minecraft.net");
                }
            }

            // Built up a login request
            final String authToken = authTokenMatch.group(1);
            final StringBuilder requestStr = new StringBuilder();
            requestStr.append("username=");
            requestStr.append(urlEncode(account.SignInUsername));
            requestStr.append("&password=");
            requestStr.append(urlEncode(account.Password));
            requestStr.append("&authenticityToken=");
            requestStr.append(urlEncode(authToken));
            if (remember) {
                requestStr.append("&remember=true");
            }
            requestStr.append("&redirect=");
            requestStr.append(urlEncode(HomepageUri));

            // POST our data to the login handler
            this.publish("Signing in...");
            String loginResponse = HttpUtil.uploadString(LoginSecureUri, requestStr.toString());
            if(loginResponse == null){
                return SignInResult.CONNECTION_ERROR;
            }
            
            // Check for common failure scenarios
            if (loginResponse.contains(WrongUsernameOrPasswordMessage)) {
                return SignInResult.WRONG_USER_OR_PASS;
            } else if (loginResponse.contains(MigratedAccountMessage)) {
                return SignInResult.MIGRATED_ACCOUNT;
            }

            // Confirm tha we are now logged in
            final Matcher responseMatch = loggedInAsRegex.matcher(loginResponse);
            if (responseMatch.find()) {
                account.PlayerName = responseMatch.group(1);
                return SignInResult.SUCCESS;
            } else {
                LogUtil.getLogger().log(Level.INFO, loginResponse);
                throw new SignInException("Login failed: Unrecognized response served by minecraft.net");
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
            LogUtil.getLogger().log(Level.FINE, "MinecraftNetGetServerListWorker");
            String serverListString = HttpUtil.downloadString(ServerListUri);
            final Matcher serverListMatch = serverNameRegex.matcher(serverListString);
            final Matcher otherServerDataMatch = otherServerDataRegex.matcher(serverListString);
            final ArrayList<ServerInfo> servers = new ArrayList<>();
            // Go through server table, one at a time!
            while (serverListMatch.find()) {
                // Fetch server's basic info
                final ServerInfo server = new ServerInfo();
                server.hash = serverListMatch.group(1);
                server.name = htmlDecode(serverListMatch.group(2));
                final int rowStart = serverListMatch.end();

                // Try getting the rest using another regex
                if (otherServerDataMatch.find(rowStart)) {
                    // this bit doesn't actually work yet (gotta fix my regex)
                    server.players = Integer.parseInt(otherServerDataMatch.group(1));
                    server.maxPlayers = Integer.parseInt(otherServerDataMatch.group(2));
                    final String uptimeString = otherServerDataMatch.group(3);
                    try {
                        // "servers.size" is added to preserve ordering for servers
                        // that have otherwise-identical uptime. It makes sure that every
                        // server has higher uptime (by 1 second) than the preceding one.
                        server.uptime = parseUptime(uptimeString) + servers.size();
                    } catch (IllegalArgumentException ex) {
                        String logMsg = String.format("Error parsing server uptime (\"{0}\") for {1}",
                                uptimeString, server.name);
                        LogUtil.getLogger().log(Level.WARNING, logMsg, ex);
                    }
                    server.flag = otherServerDataMatch.group(4);
                } else {
                    LogUtil.getLogger().log(Level.WARNING,
                            "Error passing extended server info for {0}", server.name);
                }
                servers.add(server);
            }
            // This list is heading off to ServerListScreen (not implemented yet)
            return servers.toArray(new ServerInfo[0]);
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
            LogUtil.getLogger().log(Level.FINE, "GetServerPassWorker");
            String serverLink = PlayUri + serverInfo.hash;

            String playPage = HttpUtil.downloadString(serverLink);
            if (playPage == null) {
                LogUtil.getLogger().log(Level.SEVERE,
                        "Error downloading play page for \"{0}\"", serverInfo.name);
                return false;
            }

            Matcher appletParamMatch = appletParamRegex.matcher(playPage);
            while (appletParamMatch.find()) {
                String name = appletParamMatch.group(1);
                String value = appletParamMatch.group(2);
                switch (name) {
                    case "username":
                        account.PlayerName = value;
                        break;
                    case "server":
                        serverInfo.address = InetAddress.getByName(value);
                        break;
                    case "port":
                        serverInfo.port = Integer.parseInt(value);
                        break;
                    case "mppass":
                        serverInfo.pass = value;
                        break;
                }
            }
            return true;
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

    // Tries to restore previous session (if possible)
    private boolean loadSessionCookie(boolean remember) throws BackingStoreException {
        LogUtil.getLogger().log(Level.FINE, "MinecraftNetSession.loadSessionCookie");
        clearCookies();
        if (store.childrenNames().length > 0) {
            if (remember) {
                loadCookies();
                final HttpCookie cookie = super.getCookie(CookieName);
                final String userToken = "username%3A" + account.SignInUsername + "%00";
                if (cookie != null && cookie.getValue().contains(userToken)) {
                    LogUtil.getLogger().log(Level.FINE,
                            "Loaded saved session for {0}", account.SignInUsername);
                    return true;
                } else {
                    LogUtil.getLogger().log(Level.FINE,
                            "Discarded saved session (username mismatch).");
                }
            } else {
                LogUtil.getLogger().log(Level.FINE, "Discarded a saved session.");
            }
        } else {
            LogUtil.getLogger().log(Level.FINE, "No session saved.");
        }
        return false;
    }

    // Parses Minecraft.net server list's way of showing uptime (e.g. 1s, 1m, 1h, 1d)
    // Returns the number of seconds
    private static int parseUptime(String uptime)
            throws IllegalArgumentException {
        if (uptime == null) {
            throw new NullPointerException("uptime");
        }
        final String numPart = uptime.substring(0, uptime.length() - 1);
        final char unitPart = uptime.charAt(uptime.length() - 1);
        final int number = Integer.parseInt(numPart);
        switch (unitPart) {
            case 's':
                return number;
            case 'm':
                return number * 60;
            case 'h':
                return number * 60 * 60;
            case 'd':
                return number * 60 * 60 * 24;
            default:
                throw new IllegalArgumentException("Invalid date/time parameter.");
        }
    }

    private URI siteUri;
}
