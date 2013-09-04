package net.classicube.launcher;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

abstract class GameService {

    UserAccount account;

    protected GameService(UserAccount account) {
        this.account = account;
    }

    // Tries to start a play session
    public abstract SignInResult signIn();

    // Fetches the server list
    public abstract ServerInfo[] getServerList();

    // Gets mppass for given server
    public abstract String getServerPass(ServerInfo server);

    // Stores current session
    public abstract void storeSession(Preferences pref);

    // Loads a previously-saved session
    public abstract void loadSession(Preferences pref);

    // Gets base skin URL (to pass to the client)
    public abstract String getSkinUrl();
    
    // Gets site URL (for cookie filtering)
    public abstract URI getSiteUri();
    

    protected HttpURLConnection makeHttpConnection(String urlString, byte[] postData)
            throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        String referer = url.getProtocol() + url.getHost();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.addRequestProperty("REFERER", referer);
        if (postData != null) {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            connection.setDoOutput(true);
        } else {
            connection.setRequestMethod("GET");
        }
        return connection;
    }

    // download a string using GET
    protected String DownloadString(String urlString) {
        return UploadString(urlString, null);
    }

    // upload a string using POST, and then download the response
    protected String UploadString(String urlString, String dataString) {
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
                    os.flush();
                }
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
            Logger.getLogger(GameService.class.getName()).log(Level.SEVERE, null, ex);
            return null;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    
    protected void ClearCookies(){
        cookieJar.removeAll();
    }
    
    
    protected void StoreCookies(Preferences pref){
        try {
            pref.clear();
        } catch (BackingStoreException ex) {
            Logger.getLogger(GameService.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (HttpCookie cookie: cookieJar.getCookies()){
            pref.put(cookie.getName(), cookie.toString());
        }
    }
    
    
    protected void LoadCookies(Preferences pref){
        try {
            for(String cookieName : pref.keys()){
                HttpCookie newCookie = new HttpCookie(cookieName,pref.get(cookieName, null));
                cookieJar.add(getSiteUri(), newCookie);
            }
        } catch (BackingStoreException ex) {
            Logger.getLogger(GameService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public static void Init(){
        cm = new CookieManager();
        cookieJar = cm.getCookieStore();
        CookieManager.setDefault(cm);
    }
    
    static CookieStore cookieJar;
    static CookieManager cm;
}