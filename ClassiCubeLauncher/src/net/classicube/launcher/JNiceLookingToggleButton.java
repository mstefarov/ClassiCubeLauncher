package net.classicube.launcher;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JToggleButton;

public class JNiceLookingToggleButton extends JToggleButton {

    @Override
    protected void paintComponent(Graphics g) {
        // Prepare
        Graphics2D g2 = (Graphics2D) g.create();
        Dimension size = this.getSize();

        // Define colors
        Color ccGradientTop, ccGradientBottom, ccHighlight, ccBorder;
        if (isEnabled()) {
            if (getModel().isArmed() || getModel().isPressed()) {
                // Pressed
                ccGradientTop = new Color(146, 123, 166);
                ccGradientBottom = new Color(168, 141, 191);
                ccHighlight = ccGradientTop;
            } else if (getModel().isRollover()) {
                // Hover
                ccGradientTop = new Color(180, 153, 203);
                ccGradientBottom = new Color(158, 135, 178);
                ccHighlight = new Color(192, 168, 211);
            } else {
                // Normal
                ccGradientTop = new Color(170, 143, 193);
                ccGradientBottom = new Color(148, 125, 168);
                ccHighlight = new Color(182, 158, 201);
            }
            ccBorder = new Color(97, 81, 110);
        } else {
            // Disabled
            ccGradientTop = new Color(182, 169, 194);
            ccGradientBottom = new Color(158, 146, 168);
            ccHighlight = new Color(191, 179, 201);
            ccBorder = new Color(128, 128, 128);
        }

        // Paint background
        RoundRectangle2D roundBorder = new RoundRectangle2D.Float(1, 1, size.width - 3, size.height - 3, 2, 2);
        GradientPaint gp = new GradientPaint(
                0, 0, ccGradientTop,
                0, getHeight(), ccGradientBottom);
        g2.setPaint(gp);
        g2.fill(roundBorder);

        // Paint background highlight
        g2.setPaint(ccHighlight);
        g2.drawLine(3, 2, size.width - 4, 2);

        // Paint border
        g2.setPaint(ccBorder);
        g2.draw(roundBorder);

        // Measure the label
        FontMetrics fm = getFontMetrics(getFont());
        Rectangle2D rect = fm.getStringBounds(getText(), g);

        int textHeight = (int) (rect.getHeight());
        int textWidth = (int) (rect.getWidth());
        int panelHeight = this.getHeight();
        int panelWidth = this.getWidth();

        // Center text horizontally and vertically
        int x = (panelWidth - textWidth) / 2;
        int y = (panelHeight - textHeight) / 2 + fm.getAscent() - 1;

        // Paint text shadow
        if (isEnabled()) {
            g2.setPaint(ccBorder);
            g2.drawString(getText(), x + 1, y + 1);
        }

        // Paint text proper
        g2.setPaint(Color.WHITE);
        g2.drawString(getText(), x, y);

        // Clean up
        g2.dispose();
    }
}
