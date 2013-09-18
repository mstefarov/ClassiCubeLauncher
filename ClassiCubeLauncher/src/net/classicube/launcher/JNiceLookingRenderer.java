package net.classicube.launcher;

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
        public static void paintComponent(AbstractButton button, Graphics g) {
        // Prepare
        Graphics2D g2 = (Graphics2D) g.create();
        Dimension size = button.getSize();
        int offset = 0;
        
        ButtonModel model = button.getModel();

        // Define colors
        Color ccGradientTop, ccGradientBottom, ccHighlight, ccBorder;
        if (button.isEnabled()) {
            if (model.isArmed() || model.isPressed() || model.isSelected()) {
                // Pressed
                ccGradientTop = new Color(146, 123, 166);
                ccGradientBottom = new Color(168, 141, 191);
                ccHighlight = ccGradientTop;
                offset = 1;
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
        RoundRectangle2D roundBorder = new RoundRectangle2D.Float(1, 1, size.width - 3, size.height - 3, 4, 4);
        GradientPaint gp = new GradientPaint(
                0, 0, ccGradientTop,
                0, button.getHeight(), ccGradientBottom);
        g2.setPaint(gp);
        g2.fill(roundBorder);

        // Paint background highlight
        g2.setPaint(ccHighlight);
        g2.drawLine(3, 2, size.width - 4, 2);

        // Paint highlight glow
        if (button.isFocusOwner() || (button instanceof JButton) && ((JButton)button).isDefaultButton()) {
            int glowSize = 1;
            if (button.isFocusOwner()) {
                glowSize = 3;
            }
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(glowSize));
            RoundRectangle2D innerBorder = new RoundRectangle2D.Float(2, 2, size.width - 5, size.height - 5, 4, 4);
            g2.draw(innerBorder);
            g2.setStroke(oldStroke);
        }
        
        // Paint border
        g2.setPaint(ccBorder);
        g2.draw(roundBorder);

        // Measure the label
        FontMetrics fm = button.getFontMetrics(button.getFont());
        Rectangle2D rect = fm.getStringBounds(button.getText(), g);

        int textHeight = (int) (rect.getHeight());
        int textWidth = (int) (rect.getWidth());
        int panelHeight = button.getHeight();
        int panelWidth = button.getWidth();

        // Center text horizontally and vertically
        int x = (panelWidth - textWidth) / 2;
        int y = (panelHeight - textHeight) / 2 + fm.getAscent() - 1;

        // Paint text shadow
        if (button.isEnabled()) {
            g2.setPaint(ccBorder);
            g2.drawString(button.getText(), x + 1 + offset, y + 1 + offset);
        }

        // Paint text proper
        g2.setPaint(Color.WHITE);
        g2.drawString(button.getText(), x + offset, y + offset);

        // Clean up
        g2.dispose();
    }
}
