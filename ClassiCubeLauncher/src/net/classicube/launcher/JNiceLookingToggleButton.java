package net.classicube.launcher;

import java.awt.Graphics;
import javax.swing.JToggleButton;

public class JNiceLookingToggleButton extends JToggleButton {

    @Override
    protected void paintComponent(Graphics g) {
        JNiceLookingRenderer.paintComponent(this, g);
    }
}
