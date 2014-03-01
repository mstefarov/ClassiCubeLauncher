package net.classicube.launcher.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import net.classicube.launcher.LogUtil;

// Static class that keeps track of GUI look-and-feel,
// branding colors, and loading (lazily) images and icons.
public final class Resources {

    public static final Color ccGradient = new Color(124, 104, 141),
            mcGradient = new Color(36, 36, 36),
            ccLightColor = new Color(153, 128, 173),
            ccBorderColor = new Color(97, 81, 110),
            accentColor = new Color(101, 38, 143);

    private static Image classiCubeBackground = null,
            minecraftNetBackground = null,
            classiCubeLogo = null,
            minecraftNetLogo = null,
            errorIcon = null,
            warningIcon = null,
            infoIcon = null;
    private static ArrayList<Image> windowIcons = null;

    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel() {
                @Override
                public UIDefaults getDefaults() {
                    // Customize the colors to match Charged-Miners.com style
                    final UIDefaults defaults = super.getDefaults();
                    final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 13);
                    defaults.put("Button.font", font);
                    defaults.put("ToggleButton.font", font);
                    defaults.put("Button.textForeground", Color.WHITE);
                    defaults.put("ToggleButton.textForeground", Color.WHITE);
                    defaults.put("nimbusBase", ccLightColor);
                    defaults.put("nimbusBlueGrey", ccLightColor);
                    defaults.put("control", ccLightColor);
                    defaults.put("nimbusFocus", ccBorderColor);
                    defaults.put("nimbusBorder", ccBorderColor);
                    defaults.put("nimbusSelectionBackground", ccBorderColor);
                    defaults.put("Table.background", Color.WHITE);
                    defaults.put("nimbusOrange", accentColor);
                    return defaults;
                }
            });
        } catch (final UnsupportedLookAndFeelException ex) {
            LogUtil.getLogger().log(Level.WARNING, "Error configuring GUI style.", ex);
        }
    }

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

    public static Image getErrorIcon() {
        if (errorIcon == null) {
            errorIcon = loadImage("/images/errorIcon.png");
        }
        return errorIcon;
    }

    public static Image getWarningIcon() {
        if (warningIcon == null) {
            warningIcon = loadImage("/images/warningIcon.png");
        }
        return warningIcon;
    }

    public static Image getInfoIcon() {
        if (infoIcon == null) {
            infoIcon = loadImage("/images/infoIcon.png");
        }
        return infoIcon;
    }

    public static List<Image> getWindowIcons() {
        if (windowIcons == null) {
            windowIcons = new ArrayList<>(2);
            windowIcons.add(loadImage("/images/windowIcon16x16.png"));
            windowIcons.add(loadImage("/images/windowIcon32x32.png"));
        }
        return windowIcons;
    }

    // Loads an image from inside the ClassiCubeLauncher JAR
    private static Image loadImage(final String fileName) {
        if (fileName == null) {
            throw new NullPointerException("fileName");
        }
        final URL imageUrl = Resources.class.getResource(fileName);
        try {
            return ImageIO.read(imageUrl);
        } catch (final IOException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error loading resource: " + fileName, ex);
            return null;
        }
    }

    private Resources() {
    }
}
