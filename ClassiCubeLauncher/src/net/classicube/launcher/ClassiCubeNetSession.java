package net.classicube.launcher;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Provides all functionality specific to ClassiCube.net:
// Signing in, parsing play links, getting server list, getting server details.
final class ClassiCubeNetSession extends GameSession {

    public ClassiCubeNetSession() {
        super(GameServiceType.ClassiCubeNetService);
        try {
            this.siteUri = new URI(HOMEPAGE_URL);
        } catch (final URISyntaxException ex) {
            // this never happens
        }
    }
    // =============================================================================================
    //                                                                                       SIGN-IN
    // =============================================================================================
    private static final String LOGIN_URL = "http://www.classicube.net/acc/login",
            LOGOUT_URL = "http://www.classicube.net/acc/logout",
            COOKIE_NAME = "session",
            WRONG_USERNAME_OR_PASS_MESSAGE = "Login failed (Username or password may be incorrect)",
            AUTH_TOKEN_PATTERN = "<input id=\"csrf_token\" name=\"csrf_token\" type=\"hidden\" value=\"(.+?)\">",
            USERNAME_PATTERN = "^[a-zA-Z0-9_\\.]{2,16}$",
            LOGGED_IN_AS_PATTERN = "<a href=\"/acc\" class=\"button\">([a-zA-Z0-9_\\.]{2,16})</a>";
    private static final Pattern authTokenRegex = Pattern.compile(AUTH_TOKEN_PATTERN),
            loggedInAsRegex = Pattern.compile(LOGGED_IN_AS_PATTERN);

    // Asynchronously try signing in our user
    @Override
    public SignInTask signInAsync(final UserAccount account, final boolean remember) {
        if (account == null) {
            throw new NullPointerException("account");
        }
        this.account = account;
        return new SignInWorker(remember);
    }

    @Override
    public GameServiceType getServiceType() {
        return GameServiceType.ClassiCubeNetService;
    }

    private class SignInWorker extends SignInTask {

        public SignInWorker(final boolean remember) {
            super(remember);
        }

        @Override
        protected SignInResult doInBackground()
                throws Exception {
            final Logger logger = LogUtil.getLogger();
            logger.log(Level.FINE, "ClassiCubeNetSession.SignInWorker");
            final boolean restoredSession = loadSessionCookies(this.remember, COOKIE_NAME);

            // check if given username is valid at all
            if(!account.signInUsername.matches(USERNAME_PATTERN)){
                return SignInResult.EMAIL_UNACCEPTABLE;
            }
            
            // download the login page
            String loginPage = HttpUtil.downloadString(LOGIN_URL);
            if (loginPage == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // See if we're already logged in
            final Matcher loginMatch = loggedInAsRegex.matcher(loginPage);
            if (loginMatch.find()) {
                final String actualPlayerName = loginMatch.group(1);
                if (this.remember && actualPlayerName.equalsIgnoreCase(account.playerName)) {
                    // If player is already logged in with the right account:
                    // reuse a previous session
                    account.playerName = actualPlayerName;
                    logger.log(Level.INFO,
                            "Restored session for {0}", account.playerName);
                    storeCookies();
                    return SignInResult.SUCCESS;

                } else {
                    // If we're not supposed to reuse session, if old username
                    // is different, or if there is no play session cookie set - relog
                    logger.log(Level.INFO,
                            "Switching accounts from {0} to {1}",
                            new Object[]{actualPlayerName, account.playerName});
                    HttpUtil.downloadString(LOGOUT_URL);
                    clearStoredSession();
                    loginPage = HttpUtil.downloadString(LOGIN_URL);
                }

            } else if (restoredSession) {
                // Failed to restore session
                logger.log(Level.WARNING,
                        "Failed to restore session at ClassiCube.net; retrying.");
                HttpUtil.downloadString(LOGOUT_URL);
                clearStoredSession();
                loginPage = HttpUtil.downloadString(LOGIN_URL);
            }

            // Extract authenticityToken from the login page
            final Matcher authTokenMatch = authTokenRegex.matcher(loginPage);
            if (!authTokenMatch.find()) {
                // We asked for a login form, got something different back. Panic.
                logger.log(Level.INFO, loginPage);
                throw new SignInException("Unrecognized login form served by ClassiCube.net");
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
            if (this.remember) {
                requestStr.append("&remember_me=true");
            }
            requestStr.append("&redirect=");
            requestStr.append(urlEncode(HOMEPAGE_URL));

            // POST our data to the login handler
            final String loginResponse = HttpUtil.uploadString(LOGIN_URL, requestStr.toString());
            if (loginResponse == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // Check for common failure scenarios
            if (loginResponse.contains(WRONG_USERNAME_OR_PASS_MESSAGE)) {
                return SignInResult.WRONG_USER_OR_PASS;
            }

            // Confirm that we are now logged in
            final Matcher responseMatch = loggedInAsRegex.matcher(loginResponse);
            if (responseMatch.find()) {
                // Signed in successfully
                account.playerName = responseMatch.group(1);
                if (this.remember) {
                    storeSession();
                }
                logger.log(Level.INFO,
                        "Successfully signed in as {0} ({1})",
                        new Object[]{account.signInUsername, account.playerName});
                return SignInResult.SUCCESS;

            } else {
                // Still not signed in. Something is wrong.
                clearStoredSession();
                logger.log(Level.INFO, loginResponse);
                throw new SignInException("Unrecognized response served by ClassiCube.net");
            }
        }
    }
    // =============================================================================================
    //                                                                                   SERVER LIST
    // =============================================================================================
    private static final String SERVER_LIST_URL = "http://www.classicube.net/api/serverlist";

    @Override
    public GetServerListTask getServerListAsync() {
        return new GetServerListWorker();
    }

    private class GetServerListWorker extends GetServerListTask {

        @Override
        protected ServerListEntry[] doInBackground()
                throws Exception {
            LogUtil.getLogger().log(Level.FINE, "ClassiCubeNetGetServerListWorker");
            final String serverListString = HttpUtil.downloadString(SERVER_LIST_URL);

            final ArrayList<ServerListEntry> servers = new ArrayList<>();

            final JsonArray array = JsonParser.array().from(serverListString);

            for (final Object rawRow : array) { //iterate through and add servers to the list
                final JsonObject row = (JsonObject) rawRow;
                final ServerListEntry info = new ServerListEntry();

                info.flag = "";
                info.hash = row.getString("hash");
                info.maxPlayers = row.getInt("maxplayers");
                info.name = row.getString("name");
                info.players = row.getInt("players");
                info.uptime = row.getInt("uptime");
                servers.add(info); //add it
            }
            return servers.toArray(new ServerListEntry[servers.size()]); //return
        }
    }
    // =============================================================================================
    //                                                                              DETAILS-FROM-URL
    // =============================================================================================
    private static final String PLAY_HASH_URL_PATTERN = "^http://" // scheme
            + "www.classicube.net/server/play/" // host+path
            + "([0-9a-fA-F]{28,32})/?" + // hash
            "(\\?override=(true|1))?$"; // override
    private static final String IP_PORT_URL_PATTERN = "^http://" // scheme
            + "www.classicube.net/server/play/?" // host+path
            + "\\?ip=(localhost|(\\d{1,3}\\.){3}\\d{1,3}|([a-zA-Z0-9\\-]+\\.)+([a-zA-Z0-9\\-]+))" // host/IP
            + "&port=(\\d{1,5})" // port
            + "(&mppass=(.+))?$"; // optional mppass
    private static final Pattern playHashUrlRegex = Pattern.compile(PLAY_HASH_URL_PATTERN),
            ipPortUrlRegex = Pattern.compile(IP_PORT_URL_PATTERN);

    @Override
    public ServerJoinInfo getDetailsFromUrl(final String url) {
        final ServerJoinInfo directResult = super.getDetailsFromDirectUrl(url);
        if (directResult != null) {
            return directResult;
        }

        final Matcher playHashUrlMatch = playHashUrlRegex.matcher(url);
        if (playHashUrlMatch.matches()) {
            final ServerJoinInfo result = new ServerJoinInfo();
            result.signInNeeded = true;
            result.passNeeded = true;
            result.hash = playHashUrlMatch.group(1);
            final String overrideString = playHashUrlMatch.group(3);
            if ("1".equals(overrideString) || "true".equals(overrideString)) {
                result.override = true;
            }
            return result;
        }

        final Matcher ipPortUrlMatch = ipPortUrlRegex.matcher(url);
        if (ipPortUrlMatch.matches()) {
            final ServerJoinInfo result = new ServerJoinInfo();
            result.pass = ipPortUrlMatch.group(7);
            result.signInNeeded = (result.pass != null);
            try {
                result.address = InetAddress.getByName(ipPortUrlMatch.group(1));
            } catch (final UnknownHostException ex) {
                return null;
            }
            final String portNum = ipPortUrlMatch.group(5);
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
    private static final String SKIN_URL = "http://www.classicube.net/skins/",
            PLAY_URL = "http://www.classicube.net/server/play/",
            HOMEPAGE_URL = "http://www.classicube.net/";

    @Override
    public String getSkinUrl() {
        return SKIN_URL;
    }

    @Override
    public URI getSiteUri() {
        return this.siteUri;
    }

    @Override
    public String getPlayUrl(final String hash) {
        if (hash == null) {
            throw new NullPointerException("hash");
        }
        return PLAY_URL + hash;
    }
    private URI siteUri;
}
