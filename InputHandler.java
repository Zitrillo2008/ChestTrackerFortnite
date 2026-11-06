package com.mycompany.chest_counter_aplication;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javax.swing.SwingUtilities;

public class InputHandler implements NativeKeyListener {
    private final MainWindow window;

    public InputHandler(MainWindow window) {
        this.window = window;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int code = e.getKeyCode();
        
        // Si estamos configurando una tecla en los Ajustes
        if (window.isCapturingKey()) {
            window.assignCapturedKey(code);
            return;
        }

        // Atajos de juego
        SwingUtilities.invokeLater(() -> {
            if (code == window.getKeyAdd()) window.changeCount(1);
            else if (code == window.getKeyAdd5()) window.changeCount(5);
            else if (code == window.getKeySub()) window.changeCount(-1);
            else if (code == window.getKeyNewMatch()) window.resetMatchCount();
            else if (code == window.getKeyReset()) window.resetCount();
            else if (code == window.getKeyToggle()) window.toggleVisibility();
        });
    }
}