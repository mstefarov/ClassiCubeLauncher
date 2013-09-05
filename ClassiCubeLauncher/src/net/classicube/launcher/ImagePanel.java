package net.classicube.launcher;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JComponent;

// A little custom panel that has a tiled background texture
public class ImagePanel extends JComponent {

    public ImagePanel() {
    }

    public ImagePanel(Image image, boolean isTiled) {
        this.image = image;
        this.isTiled = isTiled;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) {
            return;
        }
        int iw = image.getWidth(this);
        int ih = image.getHeight(this);
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
    private Image image;
    private boolean isTiled;
    private static final long serialVersionUID = 1L;
}