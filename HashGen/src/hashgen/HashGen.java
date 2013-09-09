package hashgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashGen {

    private static MessageDigest digest;
    private final static byte[] ioBuffer = new byte[64 * 1024];
    private final static String[] files = new String[]{
        "launcher.jar",
        "client.jar",
        "lwjgl.jar.pack.lzma",
        "lwjgl_util.jar.pack.lzma",
        "jinput.jar.pack.lzma",
        "windows_natives.jar.lzma",
        "macosx_natives.jar.lzma",
        "linux_natives.jar.lzma",
        "solaris_natives.jar.lzma"
    };

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        digest = MessageDigest.getInstance("MD5");
        for (String fileName : files) {
            File file = new File(fileName);
            String name = file.getName();
            long length = file.length();
            String hash = computeHash(file);
            System.out.println(name + " " + length + " " + hash);
        }
    }

    private static String computeHash(File clientJar)
            throws FileNotFoundException, IOException {
        if (clientJar == null) {
            throw new NullPointerException("clientJar");
        }
        try (FileInputStream is = new FileInputStream(clientJar)) {
            final DigestInputStream dis = new DigestInputStream(is, digest);
            while (dis.read(ioBuffer) != -1) {
                // DigestInputStream is doing its job, we just need to read through it.
            }
        }
        final byte[] localHashBytes = digest.digest();
        return new BigInteger(1, localHashBytes).toString(16);
    }
}
