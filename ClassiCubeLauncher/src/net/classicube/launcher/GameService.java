package net.classicube.launcher;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

abstract class GameService {

    private static final String UserAgent = "ClassiCube Launcher";

    protected GameService(String serviceName, UserAccount account) {
        store = Preferences.userNodeForPackage(this.getClass())
                .node("GameServices")
                .node(serviceName);
        if (account == null) {
            throw new IllegalArgumentException("account may not be null");
        }
        this.account = account;
    }

    // Tries to start a play session
    public abstract SignInResult signIn(boolean remember) throws SignInException;

    // Fetches the server list
    public abstract ServerInfo[] getServerList();

    // Gets mppass for given server
    public abstract String getServerPass(ServerInfo server);

    // Gets site URL (for cookie filtering)
    public abstract URI getSiteUri();

    // Gets base skin URL (to pass to the client)
    public abstract String getSkinUrl();

    private HttpURLConnection makeHttpConnection(String urlString, byte[] postData)
            throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        if (postData != null) {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            connection.setDoOutput(true);
        } else {
            connection.setRequestMethod("GET");
        }
        connection.setRequestProperty("Referer", urlString);
        connection.setRequestProperty("User-Agent", UserAgent);
        return connection;
    }

    // download a string using GET
    protected String downloadString(String urlString) {
        return uploadString(urlString, null);
    }

    // upload a string using POST, and then download the response
    protected String uploadString(String urlString, String dataString) {
        HttpURLConnection connection = null;
        byte[] data = null;
        if (dataString != null) {
            data = dataString.getBytes();
        }

        try {
            connection = makeHttpConnection(urlString, data);

            // Write POST (if needed)
            if (data != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(data);
                }
            }

            // Handle redirects
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                String redirectUrl = connection.getHeaderField("location");
                return downloadString(redirectUrl);
            }

            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            try (InputStream is = connection.getInputStream()) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
            }
            return response.toString();

        } catch (IOException ex) {
            LogUtil.Log(Level.SEVERE, "Error while sending request to " + urlString, ex);
            return null;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected void clearCookies() {
        cookieJar.removeAll();
    }

    protected void storeCookies() {
        try {
            store.clear();
        } catch (BackingStoreException ex) {
            LogUtil.Log(Level.SEVERE, "Error storing session", ex);
        }
        for (HttpCookie cookie : cookieJar.getCookies()) {
            store.put(cookie.getName(), cookie.toString());
        }
    }

    protected void loadCookies() {
        try {
            for (String cookieName : store.keys()) {
                HttpCookie newCookie = new HttpCookie(cookieName, store.get(cookieName, null));
                cookieJar.add(getSiteUri(), newCookie);
            }
        } catch (BackingStoreException ex) {
            LogUtil.Log(Level.SEVERE, "Error loading session", ex);
        }
    }

    protected HttpCookie getCookie(String name) {
        List<HttpCookie> cookies = cookieJar.get(getSiteUri());
        for (HttpCookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    protected boolean hasCookie(String name) {
        return (getCookie(name) != null);
    }

    protected String UrlEncode(String str) {
        String enc = StandardCharsets.UTF_8.name();
        try {
            return URLEncoder.encode(str, enc);
        } catch (UnsupportedEncodingException ex) {
            LogUtil.Log(Level.SEVERE, "UrlEncode error: " + ex);
            return null;
        }
    }

    public static void Init() {
        CookieManager cm = new CookieManager();
        cookieJar = cm.getCookieStore();
        CookieManager.setDefault(cm);
    }
    private static CookieStore cookieJar;
    protected UserAccount account;
    protected Preferences store;
}