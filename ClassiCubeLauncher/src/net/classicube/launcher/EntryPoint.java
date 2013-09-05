package net.classicube.launcher;

import java.util.logging.Level;
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
            LogUtil.Log(Level.WARNING, "Error configuring GUI style", ex);
        }
        
        // display the form
        ShowSignInScreen();
    }
    
    public static void ShowSignInScreen(){
        if(serverListScreen != null){
            serverListScreen.setVisible(false);
        }
        if(signInScreen == null){
            signInScreen = new SignInScreen();
        }
        signInScreen.setVisible(true);
    }
    
    public static void ShowServerListScreen(){
        if(signInScreen != null){
            signInScreen.setVisible(false);
        }
        if(serverListScreen == null){
            serverListScreen = new ServerListScreen();
        }
        serverListScreen.setVisible(true);
    }
    
    static SignInScreen signInScreen;
    static ServerListScreen serverListScreen;
}
