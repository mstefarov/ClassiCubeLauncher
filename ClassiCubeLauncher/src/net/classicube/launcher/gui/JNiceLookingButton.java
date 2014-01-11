package net.classicube.launcher.gui;

import java.awt.Graphics;
import javax.swing.JButton;

public class JNiceLookingButton extends JButton {
    // used to adjust the appearance of borders.
    // Positive values shift and clip the left border by the specified number of pixels.
    // Negative values shift and clip the right border.
    // This tweak is used to produce the "split-button" appearance on the sign-in form.
    private int widthAdjust = 0;

    @Override
    protected void paintComponent(Graphics g) {
        JNiceLookingRenderer.paintComponent(this, g, widthAdjust);
    }

    public int getWidthAdjust() {
        return widthAdjust;
    }

    public void setWidthAdjust(int value) {
        widthAdjust = value;
    }
}
