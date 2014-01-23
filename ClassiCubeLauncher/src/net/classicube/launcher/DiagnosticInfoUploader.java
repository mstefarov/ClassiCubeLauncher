package net.classicube.launcher;

import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import net.classicube.launcher.gui.ErrorScreen;
import org.apache.commons.lang3.StringUtils;

public class DiagnosticInfoUploader {

    public static final String GIST_API_URL = "https://api.github.com/gists";

    public static String UploadToGist() {
        // gather files
        String sysData = getSystemProperties();
        String dirData = gatherClientDirStructure();
        String clientLogData = readLogFile(PathUtil.getClientDir(), "client.log");
        String clientOldLogData = readLogFile(PathUtil.getClientDir(), "client.old.log");
        String optionsData = readLogFile(PathUtil.getClientDir(), "options.txt");
        String launcherLogData = null, launcherOldLogData = null;
        try {
            launcherLogData = readLogFile(SharedUpdaterCode.getLauncherDir(), "launcher.log");
            launcherOldLogData = readLogFile(SharedUpdaterCode.getLauncherDir(), "launcher.old.log");
        } catch (IOException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Could not find launcher log file", ex);
        }

        // construct a Gist API request (JSON)
        JsonStringWriter writer = JsonWriter.string()
                .object()
                .value("description", "ClassiCube debug information")
                .value("public", false)
                .object("files");

        // append system information
        if (sysData != null) {
            writer = writer.object("_system")
                    .value("content", sysData)
                    .end();
        }

        // append directory information
        if (dirData != null) {
            writer = writer.object("_dir")
                    .value("content", dirData)
                    .end();
        }

        // append log files
        if (clientLogData != null) {
            writer = writer.object("client.log")
                    .value("content", clientLogData)
                    .end();
        }
        if (clientOldLogData != null) {
            writer = writer.object("client.old.log")
                    .value("content", clientOldLogData)
                    .end();
        }
        if (launcherLogData != null) {
            writer = writer.object("launcher.log")
                    .value("content", launcherLogData)
                    .end();
        }
        if (launcherOldLogData != null) {
            writer = writer.object("launcher.old.log")
                    .value("content", launcherOldLogData)
                    .end();
        }
        if (optionsData != null) {
            writer = writer.object("options.txt")
                    .value("content", optionsData)
                    .end();
        }

        // finalize JSON
        String json = writer.end()
                .end()
                .done();

        // post data to Gist
        String gistResponse = HttpUtil.uploadString(GIST_API_URL, json);

        // get URL of newly-created Gist
        try {
            return JsonParser.object().from(gistResponse).getString("html_url");
        } catch (JsonParserException ex) {
            ErrorScreen.show("Error uploading debug information",
                    "Debug information was gathered, but could not be uploaded.",
                    ex);
            LogUtil.getLogger().log(Level.SEVERE, "Error parsing Gist response", ex);
            return null;
        }
    }

    // Prints all system properties to string, one per line
    // Based on HashTable.toString(), but different formatting.
    private static String getSystemProperties() {
        Properties props = System.getProperties();
        int max = props.size();
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<Object, Object>> it = props.entrySet().iterator();

        for (int i = 0; i < max; i++) {
            Map.Entry<Object, Object> e = it.next();
            Object key = e.getKey();
            Object value = e.getValue();
            sb.append(key == props ? "(this)" : key.toString());
            sb.append('=');
            sb.append(value == props ? "(this)" : value.toString());
            sb.append('\n');
        }

        return sb.toString();
    }

    private static String gatherClientDirStructure() {
        try {
            final StringBuilder sb = new StringBuilder();
            String absClientDir = PathUtil.getClientDir().getAbsolutePath();
            Files.walkFileTree(Paths.get(absClientDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    sb.append(file).append('\n');
                    return FileVisitResult.CONTINUE;
                }
            });
            return sb.toString();
        } catch (IOException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error gathering directory structure for client dir", ex);
            return null;
        }
    }

    // Reads contents of given file into a string, if the file exists. Returns null otherwise.
    private static String readLogFile(File dir, String fileName) {
        Path path = Paths.get(dir.getAbsolutePath(), fileName);
        if (path.toFile().exists()) {
            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                return StringUtils.join(lines, '\n');
            } catch (IOException ex) {
                LogUtil.getLogger().log(Level.SEVERE, "Could not read " + fileName, ex);
            }
        }
        return null;
    }
}
