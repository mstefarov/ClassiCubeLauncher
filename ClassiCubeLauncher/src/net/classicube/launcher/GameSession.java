package net.classicube.launcher;

import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.StringEscapeUtils;

// Base class for service-specific handlers.
public abstract class GameSession {
    private static final String COOKIES_NODE_NAME = "Cookies";
    protected Preferences store, cookieStore;

    // constructor used by implementations
    protected GameSession(final GameServiceType service) {
        this.store = Preferences.userNodeForPackage(this.getClass()).node(service.name());
        this.cookieStore = this.store.node(COOKIES_NODE_NAME);
    }

    // =============================================================================================
    //                                                                              ABSTRACT METHODS
    // =============================================================================================
    // Asynchronously sign a user in.
    // If "remember" is true, service should attempt to reuse stored credentials (if possible),
    // and store working credentials for next time after signing in.
    public abstract SignInTask signInAsync(final UserAccount account, final boolean remember);

    // Asynchronously fetches the server list.
    public abstract GetServerListTask getServerListAsync();

    // Attempts to extract as much information as possible about a server by URL.
    // Could be a play-link with a hash, or ip/port, or a direct-connect URL.
    public abstract ServerJoinInfo getDetailsFromUrl(final String url);

    // Gets service site's root URL (for cookie filtering).
    public abstract URI getSiteUri();

    // Gets base skin URL (to pass to the client).
    public abstract String getSkinUrl();

    // Creates a complete play URL from given server hash
    public abstract String getPlayUrl(final String hash);

    // Returns service type of this session
    public abstract GameServiceType getServiceType();
    

    public static abstract class SignInTask
            extends SwingWorker<SignInResult, String> {

        protected boolean remember;

        public SignInTask(final boolean remember) {
            this.remember = remember;
        }
    }

    public static abstract class GetServerListTask
            extends SwingWorker<ServerListEntry[], ServerListEntry> {
    }
    // =============================================================================================
    //                                                                       GETTING SERVICE DETAILS
    // =============================================================================================
    private static final String directUrlPattern = "^mc://" // scheme 
            + "(localhost|(\\d{1,3}\\.){3}\\d{1,3}|([a-zA-Z0-9\\-]+\\.)+([a-zA-Z0-9\\-]+))" // host/IP
            + "(:(\\d{1,5}))?/" // port
            + "([^/]+)" // username
            + "(/(.*))?$"; // mppass
    private static final Pattern directUrlRegex = Pattern.compile(directUrlPattern);
    private static final String appletParamPattern = "<param name=\"(\\w+)\" value=\"(.+)\">";
    protected static final Pattern appletParamRegex = Pattern.compile(appletParamPattern);

    protected ServerJoinInfo getDetailsFromDirectUrl(final String url) {
        final ServerJoinInfo result = new ServerJoinInfo();
        final Matcher directUrlMatch = directUrlRegex.matcher(url);
        if (directUrlMatch.matches()) {
            try {
                result.address = InetAddress.getByName(directUrlMatch.group(1));
            } catch (final UnknownHostException ex) {
                return null;
            }
            final String portNum = directUrlMatch.group(6);
            if (portNum != null && portNum.length() > 0) {
                try {
                    result.port = Integer.parseInt(portNum);
                } catch (final NumberFormatException ex) {
                    return null;
                }
            } else {
                result.port = 25565;
            }
            result.playerName = directUrlMatch.group(7);
            final String mppass = directUrlMatch.group(9);
            if (mppass != null) {
                result.pass = mppass;
            } else {
                result.pass = "";
            }
            return result;
        }
        return null;
    }

    // Asynchronously gets mppass for given server
    public GetServerDetailsTask getServerDetailsAsync(final String url) {
        return new GetServerDetailsTask(url);
    }

    public class GetServerDetailsTask
            extends SwingWorker<Boolean, Boolean> {

        private ServerJoinInfo joinInfo;
        private String url;

        public GetServerDetailsTask(final String url) {
            if (url == null) {
                throw new NullPointerException("url");
            }
            this.url = url;
            this.joinInfo = new ServerJoinInfo();
        }

        @Override
        protected Boolean doInBackground()
                throws Exception {
            LogUtil.getLogger().log(Level.FINE, "GetServerDetailsWorker");

            // Fetch the play page
            final String playPage = HttpUtil.downloadString(url);
            if (playPage == null) {
                return false;
            }

            // Parse information on the play page
            final Matcher appletParamMatch = appletParamRegex.matcher(playPage);
            while (appletParamMatch.find()) {
                final String name = appletParamMatch.group(1);
                final String value = appletParamMatch.group(2);
                switch (name) {
                    case "username":
                        this.joinInfo.playerName = value;
                        account.playerName = value;
                        break;
                    case "server":
                        this.joinInfo.address = InetAddress.getByName(value);
                        break;
                    case "port":
                        this.joinInfo.port = Integer.parseInt(value);
                        break;
                    case "mppass":
                        this.joinInfo.pass = value;
                        break;
                }
            }

            // Verify that we got everything
            if (this.joinInfo.playerName == null || this.joinInfo.address == null
                    || this.joinInfo.port == 0 || this.joinInfo.pass == null) {
                LogUtil.getLogger().log(Level.WARNING, "Incomplete information returned from Minecraft.net");
                return false;
            }
            return true;
        }

        public ServerJoinInfo getJoinInfo() {
            return this.joinInfo;
        }
    }
    // =============================================================================================
    //                                                                                        RESUME
    // =============================================================================================
    protected static final String RESUME_NODE_NAME = "ResumeInfo";

    public ServerJoinInfo loadResumeInfo() {
        if (!Prefs.getRememberServer()) {
            return null;
        }
        final Preferences node = store.node(RESUME_NODE_NAME);
        final ServerJoinInfo info = new ServerJoinInfo();
        info.playerName = node.get("PlayerName", null);
        info.hash = node.get("Hash", null);
        try {
            info.address = InetAddress.getByName(node.get("Address", null));
        } catch (final Exception ex) {
            return null;
        }
        info.port = node.getInt("Port", 0);
        info.pass = node.get("Pass", null);
        info.override = node.getBoolean("Override", false);
        info.signInNeeded = node.getBoolean("SignInNeeded", true);
        if (info.playerName == null || info.port == 0) {
            return null;
        }
        return info;
    }

    public void storeResumeInfo(final ServerJoinInfo info) {
        if (info == null) {
            throw new NullPointerException("info");
        }
        if (!Prefs.getRememberServer()) {
            return;
        }
        final Preferences node = this.store.node(RESUME_NODE_NAME);
        node.put("PlayerName", info.playerName);
        node.put("Hash", (info.hash != null ? info.hash : ""));
        node.put("Address", info.address.getHostAddress());
        node.putInt("Port", info.port);
        node.put("Pass", (info.pass != null ? info.pass : ""));
        node.putBoolean("Override", info.override);
        node.putBoolean("SignInNeeded", info.signInNeeded);
    }
    // =============================================================================================
    //                                                                                       COOKIES
    // =============================================================================================
    private static CookieStore cookieJar;

    // Initializes the cookie manager
    public static void init() {
        final CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        cookieJar = cm.getCookieStore();
        CookieManager.setDefault(cm);
    }

    // Clears all stored cookies
    protected void clearCookies() {
        cookieJar.removeAll();
    }

    // Stores all cookies to Preferences
    protected void storeCookies() {
        try {
            this.cookieStore.clear();
        } catch (final BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error storing session", ex);
        }
        for (final HttpCookie cookie : cookieJar.getCookies()) {
            this.cookieStore.put(cookie.getName(), cookie.getValue());
        }
    }

    // Loads all cookies from Preferences
    protected void loadCookies() {
        try {
            for (final String cookieName : this.cookieStore.keys()) {
                final HttpCookie newCookie = new HttpCookie(cookieName, cookieStore.get(cookieName, null));
                newCookie.setPath("/");
                cookieJar.add(getSiteUri(), newCookie);
            }
        } catch (final BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading session", ex);
        }
    }

    // Checks whether a cookie with the given name is stored.
    protected boolean hasCookie(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        final List<HttpCookie> cookies = cookieJar.get(getSiteUri());
        for (final HttpCookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    

    // Tries to restore previous session (if possible)
    protected final boolean loadSessionCookies(final boolean remember, String cookieName) {
        clearCookies();
        if (remember) {
            this.loadCookies();
            if (hasCookie(cookieName)) {
                LogUtil.getLogger().log(Level.FINE, "Loaded saved session.");
                return true;
            } else {
                LogUtil.getLogger().log(Level.FINE, "No session saved.");
            }
        } else {
            LogUtil.getLogger().log(Level.FINE, "Discarded a saved session.");
        }
        return false;
    }
    
    // =============================================================================================
    //                                                                                         UTILS
    // =============================================================================================
    protected UserAccount account;

    public UserAccount getAccount() {
        return this.account;
    }

    // Encodes a string in a URL-friendly format, for GET or POST
    protected String urlEncode(final String rawString) {
        if (rawString == null) {
            throw new NullPointerException("rawString");
        }
        final String encName = StandardCharsets.UTF_8.name();
        try {
            return URLEncoder.encode(rawString, encName);
        } catch (final UnsupportedEncodingException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Encoding error", ex);
            return null;
        }
    }

    // Decodes an HTML-escaped string
    protected String htmlDecode(final String encodedString) {
        if (encodedString == null) {
            throw new NullPointerException("encodedString");
        }
        return StringEscapeUtils.UNESCAPE_HTML4.translate(encodedString);
    }
}