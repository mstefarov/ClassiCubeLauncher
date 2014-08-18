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
    public static final String FORM_DATA = "application/x-www-form-urlencoded";
    public static final String JSON = "application/json";

    public static HttpURLConnection makeHttpConnection(final String urlString, final byte[] postData, final String contentType)
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
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            connection.setDoOutput(true);
        } else {
            connection.setRequestMethod("GET");
        }
        connection.setRequestProperty("Referer", urlString);
        connection.setRequestProperty("User-Agent", LogUtil.VERSION_STRING);
        return connection;
    }

    // Downloads a string using GET.
    // Returns null and logs an error on failure.
    public static String downloadString(final String urlString) {
        return uploadString(urlString, null, null, MaxRedirects);
    }

    // Uploads a string using POST, then downloads the response.
    // Returns null and logs an error on failure.
    public static String uploadString(final String urlString, final String dataString, final String contentType) {
        return uploadString(urlString, dataString, contentType, MaxRedirects);
    }

    private static String uploadString(final String urlString, final String dataString,
            final String contentType, final int followRedirects) {
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
            // DEBUG: Log request headers
            //LogUtil.getLogger().log(Level.INFO,connection.getRequestProperties().toString());

            connection = HttpUtil.makeHttpConnection(urlString, data, contentType);

            // Write POST (if needed)
            if (data != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(data);
                }
            }

            // DEBUG: Log response headers
            //LogUtil.getLogger().log(Level.INFO,connection.getHeaderFields().toString());
            // Handle redirects
            final int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                if (followRedirects > 0) {
                    final String redirectUrl = connection.getHeaderField("location");
                    return uploadString(redirectUrl, null, contentType, followRedirects - 1);
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
