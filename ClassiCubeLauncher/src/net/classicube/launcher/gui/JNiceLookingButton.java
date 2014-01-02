package net.classicube.launcher.gui;

import java.awt.Graphics;
import javax.swing.JButton;

public class JNiceLookingButton extends JButton {
    
    @Override
    protected void paintComponent(Graphics g) {
        JNiceLookingRenderer.paintComponent(this, g, widthAdjust);
    }
    
    private int widthAdjust=0;
    public int getWidthAdjust(){
        return widthAdjust;
    }
    
    public void setWidthAdjust(int value){
        widthAdjust = value;
    }
}
