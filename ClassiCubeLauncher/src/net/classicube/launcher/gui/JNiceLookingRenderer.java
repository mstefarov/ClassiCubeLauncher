package net.classicube.launcher.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;

class JNiceLookingRenderer {

    private static final int BORDER_RADIUS = 2;

    public static void paintComponent(AbstractButton button, Graphics g, int widthAdjust) {
        // Prepare
        final Graphics2D g2 = (Graphics2D) g.create();
        final Dimension size = button.getSize();
        int textOffset = 0;

        int startX = 0;
        if (widthAdjust > 0) {
            size.width += widthAdjust;
        } else if (widthAdjust < 0) {
            startX = widthAdjust;
        }

        final ButtonModel model = button.getModel();

        // Define colors
        final Color ccGradientTop, ccGradientBottom, ccHighlight, ccBorder;
        if (button.isEnabled()) {
            if (model.isArmed() || model.isPressed() || model.isSelected()) {
                // Pressed
                ccGradientTop = new Color(146, 123, 166);
                ccGradientBottom = new Color(168, 141, 191);
                ccHighlight = ccGradientTop;
                textOffset = 1;
            } else if (model.isRollover() || button.isFocusOwner()) {
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
        final RoundRectangle2D roundBorder = new RoundRectangle2D.Float(
                startX + 1, 1, // starting coordinate
                size.width - 3 - startX, size.height - 3, // size
                BORDER_RADIUS, BORDER_RADIUS); // border radii
        final GradientPaint gp = new GradientPaint(
                0, 0, ccGradientTop,
                0, button.getHeight(), ccGradientBottom);
        g2.setPaint(gp);
        g2.fill(roundBorder);

        // Paint background highlight
        g2.setPaint(ccHighlight);
        g2.drawLine(startX + 3, 2, size.width - 4 - startX, 2);

        // Paint highlight glow
        if (button.isFocusOwner() || (button instanceof JButton) && ((JButton) button).isDefaultButton()) {
            int glowSize = 1;
            if (button.isFocusOwner()) {
                glowSize = 3;
            }
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(glowSize));
            RoundRectangle2D innerBorder = new RoundRectangle2D.Float(
                    startX + 2, 2, // starting coordinate
                    size.width - 5 - startX, size.height - 5, // size
                    BORDER_RADIUS + 2, BORDER_RADIUS + 2); // border radii
            g2.draw(innerBorder);
            g2.setStroke(oldStroke);
        }

        // Paint border
        g2.setPaint(ccBorder);
        g2.draw(roundBorder);

        if ("v".equals(button.getText())) {
            // Paint a downwards arrow
            paintTriangle(g, button.getWidth() / 2 - 3 + textOffset,
                    button.getHeight() / 2 - 1 + textOffset,
                    4, Color.WHITE, ccBorder, button.isEnabled());

        } else {
            // Measure the label
            final FontMetrics fm = button.getFontMetrics(button.getFont());
            final Rectangle2D rect = fm.getStringBounds(button.getText(), g);

            final int textHeight = (int) (rect.getHeight());
            final int textWidth = (int) (rect.getWidth());
            final int panelHeight = button.getHeight();
            final int panelWidth = button.getWidth();

            // Center text horizontally and vertically
            final int x = (panelWidth - textWidth) / 2;
            final int y = (panelHeight - textHeight) / 2 + fm.getAscent() - 1;

            // Paint text shadow
            if (button.isEnabled()) {
                g2.setPaint(ccBorder);
                g2.drawString(button.getText(), x + 1 + textOffset, y + 1 + textOffset);
            }

            // Paint text proper
            g2.setPaint(Color.WHITE);
            g2.drawString(button.getText(), x + textOffset, y + textOffset);
        }
        // Clean up
        g2.dispose();
    }

    // Based on javax.swing.plaf.basic.BasicArrowButton.paintTriangle(...)
    public static void paintTriangle(Graphics g, int x, int y, int size, Color highlight, Color shadow, boolean isEnabled) {
        int mid, i, j;

        j = 0;
        size = Math.max(size, 2);
        mid = (size / 2) - 1;

        g.translate(x, y);

        if (isEnabled) {
            g.translate(1, 1);
            g.setColor(shadow);
            for (i = size - 1; i >= 0; i--) {
                g.drawLine(mid - i, j, mid + i, j);
                j++;
            }
            g.translate(-1, -1);
            g.setColor(highlight);
        }

        j = 0;
        for (i = size - 1; i >= 0; i--) {
            g.drawLine(mid - i, j, mid + i, j);
            j++;
        }
        g.translate(-x, -y);
    }

    private JNiceLookingRenderer() {
    }
}
