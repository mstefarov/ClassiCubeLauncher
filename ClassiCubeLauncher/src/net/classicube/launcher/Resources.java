package net.classicube.launcher;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

// Static class that keeps track of loading (lazily) our resource files.
// Currently just handles the 4 texture images for SignInScreen.
class Resources {

    private static Image classiCubeBackground = null,
            minecraftNetBackground = null,
            classiCubeLogo = null,
            minecraftNetLogo = null;

    public static Image getClassiCubeBackground() {
        if (classiCubeBackground == null) {
            classiCubeBackground = loadImage("/images/ClassiCubeBG.png");
        }
        return classiCubeBackground;
    }

    public static Image getMinecraftNetBackground() {
        if (minecraftNetBackground == null) {
            minecraftNetBackground = loadImage("/images/MinecraftNetBG.png");
        }
        return minecraftNetBackground;
    }

    public static Image getClassiCubeLogo() {
        if (classiCubeLogo == null) {
            classiCubeLogo = loadImage("/images/ClassiCubeLogo.png");
        }
        return classiCubeLogo;
    }

    public static Image getMinecraftNetLogo() {
        if (minecraftNetLogo == null) {
            minecraftNetLogo = loadImage("/images/MinecraftNetLogo.png");
        }
        return minecraftNetLogo;
    }

    // Loads an image from inside the ClassiCubeLauncher JAR
    private static Image loadImage(String fileName) {
        if (fileName == null) {
            throw new NullPointerException("fileName");
        }
        final URL imageUrl = Resources.class.getResource(fileName);
        try {
            return ImageIO.read(imageUrl);
        } catch (IOException ex) {
            LogUtil.die("Error loading GUI resource " + fileName, ex);
            return null;
        }
    }
}
