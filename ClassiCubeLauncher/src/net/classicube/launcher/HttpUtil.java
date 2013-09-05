package net.classicube.launcher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class HttpUtil {

    private static final String UserAgent = "ClassiCube Launcher";

    public static HttpURLConnection makeHttpConnection(String urlString, byte[] postData)
            throws MalformedURLException, IOException {
        if (urlString == null) {
            throw new IllegalArgumentException("urlString may not be null");
        }
        final URL url = new URL(urlString);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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

    // Downloads a string using GET.
    // Returns null and logs an error on failure.
    public static String downloadString(String urlString) {
        return uploadString(urlString, null);
    }

    // Uploads a string using POST, then downloads the response.
    // Returns null and logs an error on failure.
    public static String uploadString(String urlString, String dataString) {
        HttpURLConnection connection = null;
        byte[] data = null;
        if (dataString != null) {
            data = dataString.getBytes();
        }

        try {
            connection = HttpUtil.makeHttpConnection(urlString, data);

            // Write POST (if needed)
            if (data != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(data);
                }
            }

            // Handle redirects
            final int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                final String redirectUrl = connection.getHeaderField("location");
                return downloadString(redirectUrl);
            }

            // Read response
            final StringBuilder response = new StringBuilder();
            String line;
            try (InputStream is = connection.getInputStream()) {
                final BufferedReader rd = new BufferedReader(new InputStreamReader(is));
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
}
