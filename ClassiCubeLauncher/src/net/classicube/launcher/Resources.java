package net.classicube.launcher;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class Resources {

    static Image classiCubeBackground = null;

    public static Image getClassiCubeBackground() {
        if (classiCubeBackground == null) {
            classiCubeBackground = loadImage("/images/ClassiCubeBG.png");
        }
        return classiCubeBackground;
    }
    static Image minecraftNetBackground = null;

    public static Image getMinecraftNetBackground() {
        if (minecraftNetBackground == null) {
            minecraftNetBackground = loadImage("/images/MinecraftNetBG.png");
        }
        return minecraftNetBackground;
    }

    public static Image loadImage(String fileName) {
        try {
            URL imageUrl = Resources.class.getResource(fileName);
            return ImageIO.read(imageUrl);
        } catch (IOException ex) {
            // TODO: log ex
            return null;
        }
    }
}
