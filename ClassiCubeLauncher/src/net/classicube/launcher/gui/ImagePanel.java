package net.classicube.launcher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import javax.swing.JComponent;

// A little custom panel that has a tiled background texture
public final class ImagePanel extends JComponent {
    private Image image;
    private boolean isTiled;
    private boolean drawGradient;
    private Color gradientColor = Color.BLACK;
    private static final long serialVersionUID = 1L;

    public ImagePanel() {
    }

    public ImagePanel(final Image image, final boolean isTiled) {
        this.isTiled = isTiled;
        setImage(image);
    }

    public void setImage(final Image image) {
        this.image = image;
        if (!isTiled && image != null) {
            this.setPreferredSize(new Dimension(image.getWidth(this), image.getHeight(this)));
            this.setMinimumSize(this.getPreferredSize());
        }
    }

    public void setGradient(final boolean drawGradient) {
        this.drawGradient = drawGradient;
    }

    public void setGradientColor(final Color gradientColor) {
        this.gradientColor = gradientColor;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (image == null) {
            return;
        }
        final int iw = image.getWidth(this);
        final int ih = image.getHeight(this);
        if (isTiled) {
            if (iw > 0 && ih > 0) {
                for (int x = 0; x < getWidth(); x += iw) {
                    for (int y = 0; y < getHeight(); y += ih) {
                        g.drawImage(image, x, y, iw, ih, this);
                    }
                }
            }
        } else {
            g.drawImage(image, 0, 0, iw, ih, this);
        }
        if (drawGradient) {
            Color secondaryColor = new Color(gradientColor.getRed(),
                    gradientColor.getGreen(),
                    gradientColor.getBlue(),
                    0);
            Graphics2D g2 = (Graphics2D) g.create();
            LinearGradientPaint grad = new LinearGradientPaint(
                    0, 0,
                    0, getHeight(),
                    new float[]{.5f, 1},
                    new Color[]{secondaryColor, gradientColor});
            g2.setPaint(grad);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
