package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Right-side recent event log.
 */
public class RecentLogPanel extends VBox {

    private final TextArea txtLog;

    public RecentLogPanel() {
        setSpacing(8);
        setPadding(new Insets(10, 10, 10, 10));
        setStyle(UITheme.panelBorderStyle());

        Label title = new Label("Recent Log");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setTextFill(UITheme.TEXT_MAIN);

        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setWrapText(true);
        txtLog.setFocusTraversable(false);
        txtLog.setPrefRowCount(18);
        txtLog.setMaxHeight(Double.MAX_VALUE);
        UITheme.styleLogArea(txtLog);

        VBox.setVgrow(txtLog, Priority.ALWAYS);
        getChildren().addAll(title, txtLog);
    }

    public void logEvent(String event) {
        if (event == null || event.trim().isEmpty()) {
            return;
        }
        txtLog.appendText(event.trim() + "\n");
        trimLogLines(10);
    }

    private void trimLogLines(int maxLines) {
        String[] lines = txtLog.getText().split("\n");
        if (lines.length <= maxLines) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = lines.length - maxLines; i < lines.length; i++) {
            sb.append(lines[i]).append('\n');
        }
        txtLog.setText(sb.toString());
        txtLog.positionCaret(txtLog.getText().length());
    }
}
