package net.classicube.launcher;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;

public class EntryPoint {
    public static void main(String[] args) {
        // initialize shared code
        LogUtil.Init();
        GameService.Init();
        
        // set look-and-feel to Numbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException |
                InstantiationException |
                IllegalAccessException |
                UnsupportedLookAndFeelException ex) {
            Logger.getLogger(SignInScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // display the form
        new SignInScreen().setVisible(true);
    }
}
