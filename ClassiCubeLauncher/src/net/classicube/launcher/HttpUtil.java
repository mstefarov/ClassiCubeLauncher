package net.classicube.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

final class HttpUtil {

    private static final int MaxRedirects = 3;
    private static final String UserAgent = "ClassiCube Launcher";

    public static HttpURLConnection makeHttpConnection(final String urlString, final byte[] postData)
            throws MalformedURLException, IOException {
        if (urlString == null) {
            throw new NullPointerException("urlString");
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
    public static String downloadString(final String urlString) {
        return uploadString(urlString, null, MaxRedirects);
    }

    // Uploads a string using POST, then downloads the response.
    // Returns null and logs an error on failure.
    public static String uploadString(final String urlString, final String dataString) {
        return uploadString(urlString, dataString, MaxRedirects);
    }

    private static String uploadString(final String urlString, final String dataString, final int followRedirects) {
        LogUtil.getLogger().log(Level.FINE, "{0} {1}",
                new Object[]{dataString == null ? "GET" : "POST", urlString});
        HttpURLConnection connection = null;
        final byte[] data;
        if (dataString != null) {
            data = dataString.getBytes();
        } else {
            data = null;
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
                if (followRedirects > 0) {
                    final String redirectUrl = connection.getHeaderField("location");
                    return uploadString(redirectUrl, null, followRedirects - 1);
                } else {
                    LogUtil.getLogger().log(Level.FINE, "Redirected ({0}) to {1} (not following)",
                            new Object[]{responseCode, urlString});
                }
            }

            // Read response
            final StringBuilder response = new StringBuilder();
            final boolean badRequest = (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST);
            try (final InputStream is = (badRequest ? connection.getErrorStream() : connection.getInputStream())) {
                try (final InputStreamReader isr = new InputStreamReader(is)) {
                    try (final BufferedReader rd = new BufferedReader(isr)) {
                        String line;
                        while ((line = rd.readLine()) != null) {
                            response.append(line);
                            response.append(System.lineSeparator());
                        }
                    }
                }
            }
            if (badRequest) {
                String errMsg = String.format("Server returned HTTP response code: %d for URL: %s with message:%n%s",
                        responseCode, urlString, response);
                throw new IOException(errMsg);
            }

            return response.toString();

        } catch (final IOException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error while sending request to " + urlString, ex);
            return null;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpUtil() {
    }
}
