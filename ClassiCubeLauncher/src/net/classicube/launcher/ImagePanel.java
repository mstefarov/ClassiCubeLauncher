package net.classicube.launcher;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JComponent;

// A little custom panel that has a tiled background texture
public final class ImagePanel extends JComponent {
    private Image image;
    private boolean isTiled;
    private static final long serialVersionUID = 1L;

    public ImagePanel() {
    }

    public ImagePanel(final Image image, final boolean isTiled) {
        this.image = image;
        this.isTiled = isTiled;
    }

    public void setImage(final Image image) {
        this.image = image;
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
    }
}