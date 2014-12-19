package net.classicube.launcher;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.classicube.launcher.gui.PromptScreen;

// Provides all functionality specific to Minecraft.net:
// Signing in, parsing play links, getting server list, getting server details.
final class MinecraftNetSession extends GameSession {

    MinecraftNetSession() {
        super(GameServiceType.MinecraftNetService);
        try {
            siteUri = new URI(HOMEPAGE_URL);
        } catch (URISyntaxException ex) {
            // never happens
        }
    }
    // =============================================================================================
    //                                                                                       SIGN-IN
    // =============================================================================================
    private static final String PLAY_PAGE_URL = "https://minecraft.net/classic/play",
            LOGIN_URL = "https://minecraft.net/login",
            LOGOUT_URL = "https://minecraft.net/logout",
            CHALLENGE_URL = "https://minecraft.net/challenge",
            MIGRATED_ACCOUNT_MESSAGE = "Your account has been migrated",
            WRONG_USER_OR_PASS_MESSAGE = "Oops, unknown username or password.",
            CHALLENGE_FAILED_MESSAGE = "Could not confirm your identity",
            CHALLENGE_PASSED_MESSAGE = "Security challenge passed",
            AUTH_TOKEN_PATTERN = "name=\"authenticityToken\" value=\"([0-9a-f]+)\">",
            LOGGED_IN_AS_PATTERN = "<param name=\"username\" value=\"(\\S+)\">",
            COOKIE_NAME = "PLAY_SESSION",
            CHALLENGE_MESSAGE = "To confirm your identity, please answer the question below",
            CHALLENGE_QUESTION_PATTERN = "<label for=\"answer\">([^<]+)</label>",
            CHALLENGE_QUESTION_ID_PATTERN = "name=\"questionId\" value=\"(\\d+)\" />";
    private static final Pattern authTokenRegex = Pattern.compile(AUTH_TOKEN_PATTERN),
            usernameRegex = Pattern.compile(LOGGED_IN_AS_PATTERN),
            challengeQuestionRegex = Pattern.compile(CHALLENGE_QUESTION_PATTERN),
            challengeQuestionIdRegex = Pattern.compile(CHALLENGE_QUESTION_ID_PATTERN);

    @Override
    public SignInTask signInAsync(final UserAccount account, final boolean remember) {
        if (account == null) {
            throw new NullPointerException("account");
        }
        this.account = account;
        return new SignInWorker(remember);
    }

    // Asynchronously try signing in our user
    private class SignInWorker extends SignInTask {

        SignInWorker(final boolean remember) {
            super(remember);
        }

        @Override
        protected SignInResult doInBackground() throws Exception {
            LogUtil.getLogger().log(Level.FINE, "MinecraftNetSession.SignInWorker");
            final boolean restoredSession = loadSessionCookies(this.remember, COOKIE_NAME);
            boolean relogRequired = false;

            // download classic singleplayer page to check for logged-in username
            String playPage = HttpUtil.downloadString(PLAY_PAGE_URL);
            if (playPage == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // See if we're already logged in
            final Matcher loginMatch = usernameRegex.matcher(playPage);
            if (loginMatch.find()) {
                // We ARE signed in! Check the username...
                final String actualPlayerName = loginMatch.group(1);
                final boolean nameEquals = actualPlayerName.equalsIgnoreCase(account.playerName);
                if (remember && nameEquals) {
                    // If we are already signed into the right account,
                    // and we are allowed to reuse sessions...

                    // Check for presence of a challenge question
                    if (playPage.contains(CHALLENGE_MESSAGE)) {
                        SignInResult result = handleChallengeQuestions(playPage);
                        if (result != SignInResult.SUCCESS) {
                            // if challenge was not completed successfully, abort
                            return result;
                        }
                    }

                    // Session restored successfully, we are done.
                    account.playerName = actualPlayerName; // correct stored-name capitalization (if needed)
                    LogUtil.getLogger().log(Level.INFO, "Restored session for {0}", account.playerName);
                    storeCookies();
                    return SignInResult.SUCCESS;

                } else {
                    // Either the restored session was for a different user...
                    if (!nameEquals) {
                        LogUtil.getLogger().log(Level.INFO,
                                "Switching accounts from {0} to {1}",
                                new Object[]{actualPlayerName, account.playerName});
                    } else {
                        // ...or we are not allowed to restore sessions at all...
                        LogUtil.getLogger().log(Level.INFO,
                                "Unable to reuse old session; signing in anew.");
                    }
                    relogRequired = true;
                }

            } else if (restoredSession) {
                // ...or the saved session did not work (perhaps it expired).
                LogUtil.getLogger().log(Level.WARNING,
                        "Failed to restore session at Minecraft.net; retrying.");
                relogRequired = true;
            }

            // If needed: log out and clear cookies.
            if (relogRequired) {
                HttpUtil.downloadString(LOGOUT_URL);
                clearStoredSession();
            }

            // download the login page
            String loginPage = HttpUtil.downloadString(LOGIN_URL);
            if (loginPage == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // Extract authenticityToken from the login page
            final Matcher loginAuthTokenMatch = authTokenRegex.matcher(loginPage);
            if (!loginAuthTokenMatch.find()) {
                // We asked for a login form, but we received something different. Panic!
                LogUtil.getLogger().log(Level.INFO, loginPage);
                throw new SignInException("Unrecognized login form served by Minecraft.net");
            }

            // Build up the login request
            final String authToken = loginAuthTokenMatch.group(1);
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
            String loginResponse = HttpUtil.uploadString(LOGIN_URL, requestStr.toString(), HttpUtil.FORM_DATA);
            if (loginResponse == null) {
                return SignInResult.CONNECTION_ERROR;
            }

            // Check for common failure scenarios
            if (loginResponse.contains(WRONG_USER_OR_PASS_MESSAGE)) {
                return SignInResult.WRONG_USER_OR_PASS;
            } else if (loginResponse.contains(MIGRATED_ACCOUNT_MESSAGE)) {
                return SignInResult.MIGRATED_ACCOUNT;
            }

            // Confirm that we are now logged in
            String playPage2 = HttpUtil.downloadString(PLAY_PAGE_URL);
            if (playPage2 == null) {
                return SignInResult.CONNECTION_ERROR;
            }
            
            final Matcher responseMatch = usernameRegex.matcher(playPage2);
            if (responseMatch.find()) {
                // ...yes, we are signed in!
                LogUtil.getLogger().log(Level.INFO,
                        "Successfully signed in as {0} ({1})",
                        new Object[]{account.signInUsername, account.playerName});

                // Check for presence of a challenge question
                if (loginResponse.contains(CHALLENGE_MESSAGE)) {
                    SignInResult result = handleChallengeQuestions(loginResponse);
                    if (result != SignInResult.SUCCESS) {
                        return result;
                    }
                }

                // For Mojang accounts, the sign-in name (email) is different from the in-game name (player name).
                // Minecraft.net pages will display the *player name* after signing in.
                account.playerName = responseMatch.group(1);

                // If allowed, remember session (cookies and account info) until next time.
                if (remember) {
                    storeSession();
                }
                return SignInResult.SUCCESS;

            } else {
                // ...no, we did not sign in. And we don't know why. Panic!
                clearStoredSession();
                LogUtil.getLogger().log(Level.INFO, loginResponse);
                throw new SignInException("Unrecognized response served by minecraft.net");
            }
        }
    }

    SignInResult handleChallengeQuestions(final String page)
            throws SignInException {
        if (page == null) {
            throw new NullPointerException("page");
        }
        LogUtil.getLogger().log(Level.FINE, "Minecraft.net asked a challenge question.");

        // Locate the question text, and other form data, on the page
        final Matcher challengeMatch = challengeQuestionRegex.matcher(page);
        final Matcher challengeAuthTokenMatch = authTokenRegex.matcher(page);
        final Matcher challengeQuestionIdMatch = challengeQuestionIdRegex.matcher(page);
        if (!challengeMatch.find() || !challengeAuthTokenMatch.find() || !challengeQuestionIdMatch.find()) {
            LogUtil.getLogger().log(Level.INFO, page);
            throw new SignInException("Could not parse challenge question.");
        }
        final String authToken = challengeAuthTokenMatch.group(1);
        final String question = challengeMatch.group(1);
        final int questionId = Integer.parseInt(challengeQuestionIdMatch.group(1));

        // Ask user to answer the question
        String answer = PromptScreen.show("Minecraft.net asks",
                "<html>Since you are logging in from this computer for the first time,<br>"
                + "Minecraft.net needs you to confirm your identity before you can continue.<br>"
                + "This is to make sure that your account isn't used without your authorization."
                + "<br><br><b>" + question, "", true);
        if (answer == null) {
            // If player gave no answer, or closed the window, abort signing in.
            return SignInResult.CHALLENGE_FAILED;
        }

        // POST player's answer, auth token, and question ID to Minecraft.net
        final StringBuilder challengeRequestStr = new StringBuilder();
        challengeRequestStr.append("answer=");
        challengeRequestStr.append(urlEncode(answer));
        challengeRequestStr.append("&authenticityToken=");
        challengeRequestStr.append(urlEncode(authToken));
        challengeRequestStr.append("&questionId=");
        challengeRequestStr.append(questionId);
        final String response = HttpUtil.uploadString(CHALLENGE_URL, challengeRequestStr.toString(), HttpUtil.FORM_DATA);

        // Parse the response
        if (response == null) {
            return SignInResult.CONNECTION_ERROR;

        } else if (response.contains(CHALLENGE_FAILED_MESSAGE)) {
            // Player answered the question incorrectly. Abort.
            return SignInResult.CHALLENGE_FAILED;

        } else if (response.contains(CHALLENGE_PASSED_MESSAGE)) {
            // Question was answered correctly. Success!
            return SignInResult.SUCCESS;

        } else {
            // We failed, and we don't know why. Panic!
            LogUtil.getLogger().log(Level.INFO, response);
            throw new SignInException("Could not pass security question: "
                    + "Unrecognized response served by minecraft.net");
        }
    }
    // =============================================================================================
    //                                                                                   SERVER LIST
    // =============================================================================================
    private static final String SERVER_LIST_URL = "https://minecraft.net/classic/list",
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

            if (serverListString == null) {
                throw new RuntimeException("Could not fetch a list of servers from Minecraft.net");
            }

            final Matcher serverListMatch = serverNameRegex.matcher(serverListString);
            final Matcher otherServerDataMatch = otherServerDataRegex.matcher(serverListString);
            final ArrayList<ServerListEntry> servers = new ArrayList<>();
            // Go through server table, one at a time!
            while (serverListMatch.find()) {
                // Fetch server's basic info
                final ServerListEntry server = new ServerListEntry();
                server.hash = serverListMatch.group(1);
                server.name = htmlDecode(serverListMatch.group(2));
                server.name = server.name.replaceAll("&hellip;", "...");
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
            return servers.toArray(new ServerListEntry[servers.size()]);
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
            result.passNeeded = true;
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
            PLAY_URL = "http://minecraft.net/classic/play/",
            HOMEPAGE_URL = "http://minecraft.net/";

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
