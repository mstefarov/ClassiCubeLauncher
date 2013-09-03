package net.classicube.launcher;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

class Resources {

    static Image classiCubeBackground = null,
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
