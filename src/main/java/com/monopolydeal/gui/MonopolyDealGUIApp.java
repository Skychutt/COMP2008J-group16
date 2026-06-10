package com.monopolydeal.gui;

import com.monopolydeal.gui.audio.BackgroundMusicManager;
import com.monopolydeal.gui.board.GameFrame;
import com.monopolydeal.gui.menu.MainMenuFrame;
import com.monopolydeal.gui.theme.ThemedDialog;

import com.monopolydeal.ai.BotPlayerController;
import com.monopolydeal.enums.PlayerType;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;

import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * JavaFX application entry point. Launches the home screen on the primary Stage.
 */
public class MonopolyDealGUIApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Prevent JavaFX from auto-exiting when the home stage is temporarily hidden
        // while the game stage is being created.
        Platform.setImplicitExit(false);
        BackgroundMusicManager.getInstance().start();
        MainMenuFrame.createAndShow(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Starts a local hot-seat game after setup is complete.
     *
     * @param homeFrame    the home-screen wrapper (its {@code showHomeAgain()} is called on exit)
     * @param playerCount  number of players (2–5)
     * @param playerNames  player display names
     */
    public static void startLocalGame(MainMenuFrame homeFrame,
                                      int playerCount,
                                      List<String> playerNames) {
        if (playerCount < 2 || playerCount > 5) {
            homeFrame.showHomeAgain();
            return;
        }

        Deck.reset();
        GameManager.reset();

        GameManager gameManager = GameManager.getInstance();
        gameManager.initGame(playerCount, playerNames);

        GameLogic logic = new GameLogic(gameManager);
        logic.getActionHandler().setUseDialogInput(true);

        Stage gameStage = new Stage();
        Runnable homeCallback = homeFrame::showHomeAgain;

        try {
            GameFrame gameFrame = new GameFrame(gameManager, logic, gameStage, homeCallback);
            // Show the game stage BEFORE hiding home — keeps window count > 0 at all times,
            // preventing JavaFX implicit-exit from firing when the home stage is hidden.
            gameFrame.show();
            homeFrame.getStage().hide();
            logic.startGame();
        } catch (Exception e) {
            e.printStackTrace();
            gameStage.close();
            homeFrame.getStage().show();
            homeFrame.getStage().toFront();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String details = sw.toString().length() > 800
                    ? sw.toString().substring(0, 800) + "..."
                    : sw.toString();
            ThemedDialog.showError(
                    homeFrame.getStage(),
                    "Failed to start game",
                    e.getClass().getSimpleName() + ": " + e.getMessage() + "\n\n" + details
            );
        }
    }

    /**
     * Starts a single-player game against one or more AI opponents.
     */
    public static void startAiGame(MainMenuFrame homeFrame,
                                   String humanName,
                                   int aiOpponents) {
        if (aiOpponents < 1 || aiOpponents > 4) {
            homeFrame.showHomeAgain();
            return;
        }

        Deck.reset();
        GameManager.reset();

        List<GameManager.PlayerSetup> setups = new ArrayList<>();
        setups.add(new GameManager.PlayerSetup(humanName, PlayerType.HUMAN));
        for (int i = 1; i <= aiOpponents; i++) {
            setups.add(new GameManager.PlayerSetup("AI " + i, PlayerType.AI));
        }

        GameManager gameManager = GameManager.getInstance();
        gameManager.initGameWithSetups(setups);

        BotPlayerController botController = new BotPlayerController();
        GameLogic logic = new GameLogic(gameManager);
        logic.getActionHandler().setUseDialogInput(true);
        logic.getActionHandler().setDecisionResolver(botController);

        Stage gameStage = new Stage();
        Runnable homeCallback = homeFrame::showHomeAgain;

        try {
            GameFrame gameFrame = new GameFrame(
                    gameManager, logic, gameStage, homeCallback, true, botController);
            gameFrame.show();
            homeFrame.getStage().hide();
            logic.startGame();
        } catch (Exception e) {
            e.printStackTrace();
            gameStage.close();
            homeFrame.getStage().show();
            homeFrame.getStage().toFront();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String details = sw.toString().length() > 800
                    ? sw.toString().substring(0, 800) + "..."
                    : sw.toString();
            ThemedDialog.showError(
                    homeFrame.getStage(),
                    "Failed to start AI game",
                    e.getClass().getSimpleName() + ": " + e.getMessage() + "\n\n" + details
            );
        }
    }
}
