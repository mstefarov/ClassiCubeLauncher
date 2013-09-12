package net.classicube.launcher;

import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
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
// A new single-use GameService object is created for every session.
abstract class GameSession {

    // constructor used by implementations
    protected GameSession(String serviceName) {
        if (serviceName == null) {
            throw new NullPointerException("serviceName");
        }
        store = Preferences.userNodeForPackage(getClass())
                .node("GameServices")
                .node(serviceName);
        cookieJar.removeAll();
    }

    // Asynchronously sign a user in.
    // If "remember" is true, service should attempt to reuse stored credentials (if possible),
    // and store working credentials for next time after signing in.
    public abstract SignInTask signInAsync(UserAccount account, boolean remember);

    // Asynchronously fetches the server list.
    public abstract GetServerListTask getServerListAsync();

    // Attempts to extract as much information as possible about a server by URL.
    // Could be a play-link with a hash, or ip/port, or a direct-connect URL.
    public abstract ServerJoinInfo getDetailsFromUrl(String url);
    private static final String directUrlPattern = "^mc://" // scheme 
            + "(localhost|(\\d{1,3}\\.){3}\\d{1,3}|([a-zA-Z0-9\\-]+\\.)+([a-zA-Z0-9\\-]+))" // host/IP
            + "(:(\\d{1,5}))?/" // port
            + "([^/]+)" // username
            + "(/(.*))?$"; // mppass
    private static final Pattern directUrlRegex = Pattern.compile(directUrlPattern);

    protected ServerJoinInfo getDetailsFromDirectUrl(String url) {
        ServerJoinInfo result = new ServerJoinInfo();
        Matcher directUrlMatch = directUrlRegex.matcher(url);
        if (directUrlMatch.matches()) {
            try {
                result.address = InetAddress.getByName(directUrlMatch.group(1));
            } catch (UnknownHostException ex) {
                return null;
            }
            String portNum = directUrlMatch.group(6);
            if (portNum != null && portNum.length() > 0) {
                try {
                    result.port = Integer.parseInt(portNum);
                } catch (NumberFormatException ex) {
                    return null;
                }
            } else {
                result.port = 25565;
            }
            result.playerName = directUrlMatch.group(7);
            String mppass = directUrlMatch.group(9);
            if (mppass != null) {
                result.mppass = mppass;
            } else {
                result.mppass = "";
            }
            return result;
        }
        return null;
    }

    // Asynchronously gets mppass for given server
    public GetServerDetailsTask getServerDetailsAsync(String url) {
        return new GetServerDetailsWorker(url);
    }
    private static final String appletParamPattern = "<param name=\"(\\w+)\" value=\"(.+)\">";
    protected static final Pattern appletParamRegex = Pattern.compile(appletParamPattern);

    protected class GetServerDetailsWorker extends GetServerDetailsTask {

        public GetServerDetailsWorker(String url) {
            super(url);
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
                        joinInfo.playerName = value;
                        account.PlayerName = value;
                        break;
                    case "server":
                        joinInfo.address = InetAddress.getByName(value);
                        break;
                    case "port":
                        joinInfo.port = Integer.parseInt(value);
                        break;
                    case "mppass":
                        joinInfo.mppass = value;
                        break;
                }
            }

            // Verify that we got everything
            if (joinInfo.playerName == null || joinInfo.address == null
                    || joinInfo.port == 0 || joinInfo.mppass == null) {
                LogUtil.getLogger().log(Level.WARNING, "Incomplete information returned from Minecraft.net");
                return false;
            }
            return true;
        }
    }

    // Gets service site's root URL (for cookie filtering).
    public abstract URI getSiteUri();

    // Gets base skin URL (to pass to the client).
    public abstract String getSkinUrl();

    public abstract String getPlayUrl(String hash);

    // Clears all stored cookies
    protected void clearCookies() {
        cookieJar.removeAll();
    }

    // Stores all cookies to Preferences
    protected void storeCookies() {
        try {
            store.clear();
        } catch (BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error storing session", ex);
        }
        for (HttpCookie cookie : cookieJar.getCookies()) {
            store.put(cookie.getName(), cookie.toString());
        }
    }

    // Loads all cookies from Preferences
    protected void loadCookies() {
        try {
            for (String cookieName : store.keys()) {
                final HttpCookie newCookie = new HttpCookie(cookieName, store.get(cookieName, null));
                cookieJar.add(getSiteUri(), newCookie);
            }
        } catch (BackingStoreException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading session", ex);
        }
    }

    // Tries to find a cookie by name. Returns null if not found.
    protected HttpCookie getCookie(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        final List<HttpCookie> cookies = cookieJar.get(getSiteUri());
        for (HttpCookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    // Checks whether a cookie with the given name is stored.
    protected boolean hasCookie(String name) {
        return (getCookie(name) != null);
    }

    // Encodes a string in a URL-friendly format, for GET or POST
    protected String urlEncode(String rawString) {
        if (rawString == null) {
            throw new NullPointerException("rawString");
        }
        final String enc = StandardCharsets.UTF_8.name();
        try {
            return URLEncoder.encode(rawString, enc);
        } catch (UnsupportedEncodingException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Encoding error", ex);
            return null;
        }
    }

    // Decodes an HTML-escaped string
    protected String htmlDecode(String encodedString) {
        if (encodedString == null) {
            throw new NullPointerException("encodedString");
        }
        return StringEscapeUtils.UNESCAPE_HTML4.translate(encodedString);
    }

    // Initializes the cookie manager
    public static void Init() {
        final CookieManager cm = new CookieManager();
        cookieJar = cm.getCookieStore();
        CookieManager.setDefault(cm);
    }

    public static abstract class SignInTask extends SwingWorker<SignInResult, String> {

        public SignInTask(boolean remember) {
            this.remember = remember;
        }
        protected boolean remember;
    }

    public static abstract class GetServerListTask extends SwingWorker<ServerListEntry[], ServerListEntry> {
    }

    public static abstract class GetServerDetailsTask extends SwingWorker<Boolean, Boolean> {

        public GetServerDetailsTask(String url) {
            if (url == null) {
                throw new NullPointerException("url");
            }
            this.url = url;
            joinInfo = new ServerJoinInfo();
        }

        public ServerJoinInfo getJoinInfo() {
            return joinInfo;
        }
        protected ServerJoinInfo joinInfo;
        protected String url;
    }

    public UserAccount getAccount() {
        return account;
    }
    private static CookieStore cookieJar;
    protected UserAccount account;
    protected Preferences store;
}