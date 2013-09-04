package net.classicube.launcher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
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
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            connection.setDoOutput(true);
        } else {
            connection.setRequestMethod("GET");
        }
        return connection;
    }

    protected String DownloadString(String urlString) {
        return UploadString(urlString, null);
    }

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
}