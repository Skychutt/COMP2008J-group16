package com.monopolydeal.gui.theme;

import javafx.stage.Window;

/**
 * Themed confirmation dialog — delegates to {@link ThemedDialog}.
 */
public final class ThemedConfirmDialog {

    private ThemedConfirmDialog() {
    }

    public static boolean show(Window owner,
                               String title,
                               String message,
                               String confirmLabel,
                               String cancelLabel) {
        return ThemedDialog.showConfirm(owner, title, message, confirmLabel, cancelLabel);
    }
}
