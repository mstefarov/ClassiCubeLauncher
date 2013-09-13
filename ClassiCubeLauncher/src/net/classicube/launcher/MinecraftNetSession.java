package net.classicube.launcher;

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

final class MinecraftNetSession extends GameSession {

    private static final String HOMEPAGE_URL = "http://minecraft.net";

    public MinecraftNetSession() {
        super("MinecraftNetSession");
        try {
            siteUri = new URI(HOMEPAGE_URL);
        } catch (URISyntaxException ex) {
            LogUtil.die("Cannot set siteUri", ex);
        }
    }
    // =============================================================================================
    //                                                                                       SIGN-IN
    // =============================================================================================
    private static final String LOGIN_URL = "https://minecraft.net/login",
            LOGOUT_URL = "http://minecraft.net/logout",
            MIGRATED_ACCOUNT_MESSAGE = "Your account has been migrated",
            WRONG_USER_OR_PASS_MESSAGE = "Oops, unknown username or password.",
            AUTH_TOKEN_PATTERN = "<input type=\"hidden\" name=\"authenticityToken\" value=\"([0-9a-f]+)\">",
            LOGGED_IN_AS_PATTERN = "<span class=\"logged-in\">\\s*Logged in as ([a-zA-Z0-9_\\.]{2,16})",
            COOKIE_NAME = "PLAY_SESSION";
    private static final Pattern authTokenRegex = Pattern.compile(AUTH_TOKEN_PATTERN),
            loggedInAsRegex = Pattern.compile(LOGGED_IN_AS_PATTERN);

    @Override
    public SignInTask signInAsync(final UserAccount account, final boolean remember) {
        this.account = account;
        return new SignInWorker(remember);
    }

    // Asynchronously try signing in our user
    private class SignInWorker extends SignInTask {

        public SignInWorker(final boolean remember) {
            super(remember);
        }

        @Override
        protected SignInResult doInBackground() throws Exception {
            LogUtil.getLogger().log(Level.FINE, "MinecraftNetSignInWorker");
            boolean restoredSession = false;
            try {
                restoredSession = loadSessionCookie(remember);
            } catch (final BackingStoreException ex) {
                LogUtil.getLogger().log(Level.WARNING, "Error restoring session", ex);
            }

            // "publish" can be used to send text status updates to the GUI (not hooked up)
            this.publish("Connecting to Minecraft.net");

            // download the login page
            String loginPage = HttpUtil.downloadString(LOGIN_URL);
            if (loginPage == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // See if we're already logged in
            final Matcher loginMatch = loggedInAsRegex.matcher(loginPage);
            if (loginMatch.find()) {
                final String actualPlayerName = loginMatch.group(1);
                if (remember && hasCookie(COOKIE_NAME)
                        && actualPlayerName.equalsIgnoreCase(account.playerName)) {
                    // If player is already logged in with the right account: reuse a previous session
                    account.playerName = actualPlayerName;
                    LogUtil.getLogger().log(Level.INFO, "Restored session for {0}", account.playerName);
                    storeCookies();
                    return SignInResult.SUCCESS;

                } else {
                    // If we're not supposed to reuse session, if old username is different,
                    // or if there is no play session cookie set - relog
                    LogUtil.getLogger().log(Level.INFO,
                            "Switching accounts from {0} to {1}",
                            new Object[]{actualPlayerName, account.playerName});
                    HttpUtil.downloadString(LOGOUT_URL);
                    clearCookies();
                    loginPage = HttpUtil.downloadString(LOGIN_URL);
                }
            }

            // Extract authenticityToken from the login page
            final Matcher authTokenMatch = authTokenRegex.matcher(loginPage);
            if (!authTokenMatch.find()) {
                if (restoredSession) {
                    // restoring session failed; log out and retry
                    HttpUtil.downloadString(LOGOUT_URL);
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
            requestStr.append(urlEncode(account.signInUsername));
            requestStr.append("&password=");
            requestStr.append(urlEncode(account.password));
            requestStr.append("&authenticityToken=");
            requestStr.append(urlEncode(authToken));
            if (remember) {
                requestStr.append("&remember=true");
            }
            requestStr.append("&redirect=");
            requestStr.append(urlEncode(HOMEPAGE_URL));

            // POST our data to the login handler
            this.publish("Signing in...");
            final String loginResponse = HttpUtil.uploadString(LOGIN_URL, requestStr.toString());
            if (loginResponse == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // Check for common failure scenarios
            if (loginResponse.contains(WRONG_USER_OR_PASS_MESSAGE)) {
                return SignInResult.WRONG_USER_OR_PASS;
            } else if (loginResponse.contains(MIGRATED_ACCOUNT_MESSAGE)) {
                return SignInResult.MIGRATED_ACCOUNT;
            }

            // Confirm tha we are now logged in
            final Matcher responseMatch = loggedInAsRegex.matcher(loginResponse);
            if (responseMatch.find()) {
                account.playerName = responseMatch.group(1);
                return SignInResult.SUCCESS;
            } else {
                LogUtil.getLogger().log(Level.INFO, loginResponse);
                throw new SignInException("Login failed: Unrecognized response served by minecraft.net");
            }
        }
    }

    // Tries to restore previous session (if possible)
    private boolean loadSessionCookie(final boolean remember)
            throws BackingStoreException {
        LogUtil.getLogger().log(Level.FINE, "MinecraftNetSession.loadSessionCookie");
        this.clearCookies();
        if (this.store.childrenNames().length > 0) {
            if (remember) {
                this.loadCookies();
                final HttpCookie cookie = super.getCookie(COOKIE_NAME);
                final String userToken = "username%3A" + this.account.signInUsername + "%00";
                if (cookie != null && cookie.getValue().contains(userToken)) {
                    LogUtil.getLogger().log(Level.FINE,
                            "Loaded saved session for {0}", this.account.signInUsername);
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
    // =============================================================================================
    //                                                                                   SERVER LIST
    // =============================================================================================
    private static final String SERVER_LIST_URL = "http://minecraft.net/classic/list",
            SERVER_NAME_PATTERN = "<a href=\"/classic/play/([0-9a-f]+)\">([^<]+)</a>",
            SERVER_DETAILS_PATTERN = "<td>(\\d+)</td>[^<]+<td>(\\d+)</td>[^<]+<td>([^<]+)</td>[^<]+.+url\\(/images/flags/([a-z]+).png\\)";
    private static final Pattern serverNameRegex = Pattern.compile(SERVER_NAME_PATTERN),
            otherServerDataRegex = Pattern.compile(SERVER_DETAILS_PATTERN);

    @Override
    public GetServerListTask getServerListAsync() {
        return new GetServerListWorker();
    }

    private class GetServerListWorker extends GetServerListTask {

        @Override
        protected ServerListEntry[] doInBackground() throws Exception {
            LogUtil.getLogger().log(Level.FINE, "MinecraftNetGetServerListWorker");
            final String serverListString = HttpUtil.downloadString(SERVER_LIST_URL);
            final Matcher serverListMatch = serverNameRegex.matcher(serverListString);
            final Matcher otherServerDataMatch = otherServerDataRegex.matcher(serverListString);
            final ArrayList<ServerListEntry> servers = new ArrayList<>();
            // Go through server table, one at a time!
            while (serverListMatch.find()) {
                // Fetch server's basic info
                final ServerListEntry server = new ServerListEntry();
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
                    } catch (final IllegalArgumentException ex) {
                        final String logMsg = String.format(
                                "Error parsing server uptime (\"%s\") for %s",
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
            return servers.toArray(new ServerListEntry[0]);
        }
    }

    // Parses Minecraft.net server list's way of showing uptime (e.g. 1s, 1m, 1h, 1d)
    // Returns the number of seconds
    private static int parseUptime(final String uptime)
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
    // =============================================================================================
    //                                                                              DETAILS FROM URL
    // =============================================================================================
    private static final String PLAY_HASH_URL_PATTERN = "^https?://" // scheme
            + "(www\\.)?minecraft.net/classic/play/" // host+path
            + "([0-9a-fA-F]{28,32})/?" + // hash
            "(\\?override=(true|1))?$"; // override
    private static final String IP_PORT_URL_PATTERN = "^https?://" // scheme
            + "(www\\.)?minecraft.net/classic/play/?" // host+path
            + "\\?ip=(localhost|(\\d{1,3}\\.){3}\\d{1,3}|([a-zA-Z0-9\\-]+\\.)+([a-zA-Z0-9\\-]+))" // host/IP
            + "&port=(\\d{1,5})$"; // port
    private static final Pattern playHashUrlRegex = Pattern.compile(PLAY_HASH_URL_PATTERN),
            ipPortUrlRegex = Pattern.compile(IP_PORT_URL_PATTERN);

    @Override
    public ServerJoinInfo getDetailsFromUrl(final String url) {
        ServerJoinInfo result = super.getDetailsFromDirectUrl(url);
        if (result != null) {
            return result;
        }

        final Matcher playHashUrlMatch = playHashUrlRegex.matcher(url);
        if (playHashUrlMatch.matches()) {
            result = new ServerJoinInfo();
            result.signInNeeded = true;
            result.hash = playHashUrlMatch.group(2);
            if ("1".equals(playHashUrlMatch.group(4)) || "true".equals(playHashUrlMatch.group(4))) {
                result.override = true;
            }
            return result;
        }

        final Matcher ipPortUrlMatch = ipPortUrlRegex.matcher(url);
        if (ipPortUrlMatch.matches()) {
            result = new ServerJoinInfo();
            result.signInNeeded = true;
            try {
                result.address = InetAddress.getByName(ipPortUrlMatch.group(2));
            } catch (final UnknownHostException ex) {
                return null;
            }
            final String portNum = ipPortUrlMatch.group(6);
            if (portNum != null && portNum.length() > 0) {
                try {
                    result.port = Integer.parseInt(portNum);
                } catch (final NumberFormatException ex) {
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
    private static final String SKIN_URL = "http://s3.amazonaws.com/MinecraftSkins/",
            PLAY_URL = "http://minecraft.net/classic/play/";

    @Override
    public String getSkinUrl() {
        return SKIN_URL;
    }

    @Override
    public URI getSiteUri() {
        return siteUri;
    }

    @Override
    public String getPlayUrl(final String hash) {
        return PLAY_URL + hash;
    }
    
    @Override
    public GameServiceType getServiceType() {
        return GameServiceType.MinecraftNetService;
    }
    private URI siteUri;
}
