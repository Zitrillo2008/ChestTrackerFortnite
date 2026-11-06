package com.mycompany.chest_counter_aplication;

import com.github.kwhat.jnativehook.GlobalScreen;
import javax.swing.SwingUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        try {
            // Desactivar logs ruidosos de JNativeHook
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();

            SwingUtilities.invokeLater(() -> {
                MainWindow app = new MainWindow();
                GlobalScreen.addNativeKeyListener(new InputHandler(app));
                app.setVisible(true);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}