package net.classicube.launcher;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JComponent;

public class TiledImagePanel extends JComponent {

    private Image image;

    public TiledImagePanel(Image image) {
        this.image = image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int iw = image.getWidth(this);
        int ih = image.getHeight(this);
        if (iw > 0 && ih > 0) {
            for (int x = 0; x < getWidth(); x += iw) {
                for (int y = 0; y < getHeight(); y += ih) {
                    g.drawImage(image, x, y, iw, ih, this);
                }
            }
        }
    }
}