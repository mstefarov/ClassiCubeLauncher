package net.classicube.launcher;

import java.awt.Graphics;
import javax.swing.JButton;

public class JNiceLookingButton extends JButton {

    @Override
    protected void paintComponent(Graphics g) {
        JNiceLookingRenderer.paintComponent(this, g);
    }
}
