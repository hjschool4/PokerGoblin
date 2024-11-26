
package org.latinschool.client.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.latinschool.shared.Card;
import org.latinschool.server.HandEvaluator.HandRank;
import org.latinschool.shared.messages.PlayerAction;
import org.latinschool.shared.messages.UpdateGameState;
import org.latinschool.shared.messages.UpdateGameState.PlayerStatus;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.Arrays;
import java.util.Collections;

public class GameScreen {
    private Stage stage;
    private FitViewport viewport;
    private TextureLoader textureLoader;


    private Table rootTable;
    private Table nameTable;
    private TextField nameField;
    private Table waitingTable;
    private Label waitingLabel;
    private Table readyTable;
    private Label readyLabel;
    private TextField startGameTextBox;
    private Label waitingForStartLabel;
    private Table gameTable;
    private TextButton checkButton, foldButton, callButton, raiseButton;

    private TextButton nextGame;
    private Label potLabel, currentPlayerLabel;
    private Image[] communityCardImages;
    private Label gameResultLabel;
    private Label playerChipsLabel;

    private BitmapFont font;
    private PlayerUI[] playerUIs;

    private boolean isGameStarted = false;


    private int playerChips = 1000;


    private String localPlayerName;

    public GameScreen() {
        viewport = new FitViewport(1920, 1080);
        stage = new Stage(viewport);

        textureLoader = new TextureLoader();


        initializeUI();
    }

    private void initializeUI() {
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);


        nameTable = new Table();
        nameTable.setName("nameTable");
        nameTable.setFillParent(true);
        nameTable.setVisible(true);
        rootTable.addActor(nameTable);

        Label nameLabel = new Label("Enter Your Name:", skin);
        nameField = new TextField("", skin);

        nameTable.add(nameLabel).padBottom(10).center();
        nameTable.row();
        nameTable.add(nameField).width(300).padBottom(10).center();


        nameField.setTextFieldListener((textField, c) -> {
            if (c == '\n' || c == '\r') {
                String playerName = textField.getText().trim();
                if (!playerName.isEmpty()) {

                    if (nameSubmitListener != null) {
                        nameSubmitListener.onNameSubmitted(playerName);
                        setLocalPlayerName(playerName);



                        nameField.setDisabled(true);
                    }
                }
            }
        });


        waitingTable = new Table();
        waitingTable.setName("waitingTable");
        waitingTable.setFillParent(true);
        waitingTable.setVisible(false);
        rootTable.addActor(waitingTable);

        waitingLabel = new Label("Waiting for another player to join...", skin);
        waitingTable.add(waitingLabel).center();


        readyTable = new Table();
        readyTable.setName("readyTable");
        readyTable.setFillParent(true);
        readyTable.setVisible(false);
        rootTable.addActor(readyTable);

        readyLabel = new Label("Game is ready to start!", skin);
        startGameTextBox = new TextField("", skin);
        startGameTextBox.setMessageText("Press Enter to start the game");
        startGameTextBox.setAlignment(Align.center);
        startGameTextBox.setDisabled(false);
        startGameTextBox.setText("");

        readyTable.add(readyLabel).padBottom(10).center();
        readyTable.row();
        readyTable.add(startGameTextBox).width(400).height(60).center();


        startGameTextBox.setTextFieldListener((textField, c) -> {
            if (c == '\n' || c == '\r') {

                if (startGameListener != null) {
                    startGameListener.onStartGame();

                    textField.setText("");
                    textField.setDisabled(true);
                    readyTable.setVisible(false);
                }
            }
        });


        waitingForStartLabel = new Label("Waiting for the host to start the game...", skin);
        waitingForStartLabel.setVisible(false);
        rootTable.addActor(waitingForStartLabel);


        gameTable = new Table();
        gameTable.setName("gameTable");
        gameTable.setFillParent(true);
        gameTable.setVisible(false);
        rootTable.addActor(gameTable);


        Table topSectionTable = new Table();
        topSectionTable.setFillParent(true);
        topSectionTable.top().padTop(20).padLeft(20).padRight(20);
        gameTable.addActor(topSectionTable);


        potLabel = new Label("Pot: 0", skin);
        potLabel.setAlignment(Align.left);
        topSectionTable.add(potLabel).left().expandX();
        topSectionTable.row();


        currentPlayerLabel = new Label("Current Player: None", skin);
        currentPlayerLabel.setAlignment(Align.center);
        topSectionTable.add(currentPlayerLabel).center().expandX();
        topSectionTable.row();


        playerChipsLabel = new Label("Chips: 1000", skin);
        playerChipsLabel.setAlignment(Align.right);
        topSectionTable.add(playerChipsLabel).right().expandX();
        topSectionTable.row();


        communityCardImages = new Image[5];
        for (int i = 0; i < 5; i++) {
            communityCardImages[i] = new Image();
            communityCardImages[i].setSize(88 * 0.8f, 124 * 0.8f);
            communityCardImages[i].setPosition(816 + i * (88 * 0.8f + 20), 500);
            gameTable.addActor(communityCardImages[i]);
        }

        checkButton = new TextButton("Check", skin);
        checkButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (actionListener != null) {
                    actionListener.onAction(PlayerAction.ActionType.CHECK, 0);
                }
            }
        });

        foldButton = new TextButton("Fold", skin);
        foldButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (actionListener != null) {
                    actionListener.onAction(PlayerAction.ActionType.FOLD, 0);
                }
            }
        });

        callButton = new TextButton("Call", skin);
        callButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (actionListener != null) {
                    actionListener.onAction(PlayerAction.ActionType.CALL, 0);
                }
            }
        });

        raiseButton = new TextButton("Raise", skin);
        raiseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showRaiseDialog();
            }
        });


        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.addActor(checkButton);
        buttonGroup.addActor(foldButton);
        buttonGroup.addActor(callButton);
        buttonGroup.addActor(raiseButton);
        buttonGroup.space(20);
        float totalWidth = checkButton.getWidth() + foldButton.getWidth() + callButton.getWidth() + raiseButton.getWidth() + 60;
        buttonGroup.setPosition(900 - totalWidth / 2, 240);
        gameTable.addActor(buttonGroup);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/clickbait.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 36;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();



        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.GREEN;
        gameResultLabel = new Label("", labelStyle);
        gameResultLabel.setPosition(800 - gameResultLabel.getWidth() / 2, 640);
        gameResultLabel.setVisible(false);

        gameTable.addActor(gameResultLabel);

        nextGame = new TextButton("Next Game", skin);
        nextGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (roundListener != null) {
                    nextGame.setDisabled(true);
                    nextGame.setVisible(false);
                    roundListener.onNextRound();
                }
            }
        });
        nextGame.setPosition(830 - nextGame.getWidth() /2, 420);
        nextGame.setVisible(false);
        nextGame.setDisabled(false);
        gameTable.addActor(nextGame);



        playerUIs = new PlayerUI[6];
        for (int i = 0; i < 6; i++) {
            playerUIs[i] = new PlayerUI(skin);

            playerUIs[i].setVisible(false);
            stage.addActor(playerUIs[i].nameLabel);
            stage.addActor(playerUIs[i].chipsLabel);
            stage.addActor(playerUIs[i].lastActionLabel);
        }
    }

    public interface NameSubmitListener {
        void onNameSubmitted(String playerName);
    }

    public interface StartGameListener {
        void onStartGame();
    }

    public interface ActionListener {
        void onAction(PlayerAction.ActionType actionType, int amount);
    }

    public interface NextRoundListener {
        void onNextRound();
    }

    private NameSubmitListener nameSubmitListener;
    private StartGameListener startGameListener;
    private ActionListener actionListener;

    private NextRoundListener roundListener;

    public void setNameSubmitListener(NameSubmitListener listener) {
        this.nameSubmitListener = listener;
    }

    public void setStartGameListener(StartGameListener listener) {
        this.startGameListener = listener;
    }

    public void setActionListener(ActionListener listener) {
        this.actionListener = listener;
    }

    public void setNextGameListener(NextRoundListener listener) {
        this.roundListener = listener;
    }

    public void setLocalPlayerName(String name) {
        this.localPlayerName = name;
    }

    public void updatePot(int pot) {
        potLabel.setText("Pot: " + pot);
    }

    public void updateCurrentPlayer(String playerName) {
        currentPlayerLabel.setText("Current Player: " + playerName);
    }

    public void updateCommunityCards(Card[] communityCards) {
        for (int i = 0; i < communityCardImages.length; i++) {
            if (i < communityCards.length && communityCards[i] != null) {
                TextureRegion cardRegion = textureLoader.getTextureRegion(communityCards[i]);
                if (cardRegion != null) {
                    communityCardImages[i].setDrawable(new TextureRegionDrawable(cardRegion));
                }
            } else {
                communityCardImages[i].setDrawable(null);
            }
        }
    }

    public void updatePlayerChips(int chips) {
        playerChipsLabel.setText("Chips: " + chips);
        this.playerChips = chips;
    }

    public void showWaitingScreen() {

        nameTable.setVisible(false);
        readyTable.setVisible(false);
        waitingForStartLabel.setVisible(false);
        gameTable.setVisible(false);


        waitingTable.setVisible(true);
    }
    public void showNextGame() {
        nextGame.setDisabled(false);
        nextGame.setVisible(true);
    }
    public void showStartGameTextBox() {

        nameTable.setVisible(false);
        waitingTable.setVisible(false);
        waitingForStartLabel.setVisible(false);
        gameTable.setVisible(false);


        readyTable.setVisible(true);
        startGameTextBox.setDisabled(false);
        startGameTextBox.setText("");
        startGameTextBox.setFocusTraversal(false);
        startGameTextBox.setMessageText("Press Enter to start the game");
    }

    public void showWaitingForStart() {

        nameTable.setVisible(false);
        readyTable.setVisible(false);
        waitingTable.setVisible(false);
        gameTable.setVisible(false);


        waitingForStartLabel.setVisible(true);
    }

    public void startGame() {

        nameTable.setVisible(false);
        readyTable.setVisible(false);
        waitingTable.setVisible(false);
        waitingForStartLabel.setVisible(false);


        gameTable.setVisible(true);
        isGameStarted = true;


        setActionButtonsEnabled(true);
    }

    public void receiveDealtCards(Card[] playerCards) {

        for (Actor actor : stage.getActors()) {
            if (actor instanceof Image) {
                Image img = (Image) actor;
                if (img.getX() == 800 && img.getY() == 100) {
                    img.remove();
                }
                if (img.getX() == 900 && img.getY() == 100) {
                    img.remove();
                }
            }
        }

        if (playerCards.length != 2) return;

        Image playerCard1 = new Image(new TextureRegionDrawable(textureLoader.getTextureRegion(playerCards[0])));
        playerCard1.setSize(88 * 0.8f, 124 * 0.8f);
        playerCard1.setPosition(800, 100);
        stage.addActor(playerCard1);

        Image playerCard2 = new Image(new TextureRegionDrawable(textureLoader.getTextureRegion(playerCards[1])));
        playerCard2.setSize(88 * 0.8f, 124 * 0.8f);
        playerCard2.setPosition(900, 100);
        stage.addActor(playerCard2);
    }

    public void updateGameState(UpdateGameState state) {

        updatePot(state.getPot());
        updateCurrentPlayer(state.getCurrentPlayer());
        updateCommunityCards(state.getCommunityCards());


        PlayerStatus[] players = state.getPlayers();
        int localPlayerIndex = -1;


        for (int i = 0; i < players.length; i++) {
            if (players[i].getName().equals(localPlayerName)) {
                localPlayerIndex = i;
                break;
            }
        }

        if (localPlayerIndex >= 0) {

            PlayerStatus[] reorderedPlayers = new PlayerStatus[players.length];
            int currentIndex = 0;


            for (int i = localPlayerIndex; i < players.length; i++) {
                reorderedPlayers[currentIndex++] = players[i];
            }


            for (int i = 0; i < localPlayerIndex; i++) {
                reorderedPlayers[currentIndex++] = players[i];
            }

            players = reorderedPlayers;
        }


        for (int i = 0; i < playerUIs.length; i++) {
            if (i < players.length) {
                PlayerStatus ps = players[i];
                playerUIs[i].update(ps.getName(), ps.getChips(), ps.hasFolded(), ps.getLastAction());


                boolean isLocal = ps.getName().equals(localPlayerName);
                playerUIs[i].setLocalPlayer(isLocal);
                playerUIs[i].setPositionBasedOnIndex(i);
                playerUIs[i].setVisible(true);
            } else {
                playerUIs[i].setVisible(false);
            }
        }

        boolean isLocalPlayerTurn = state.getCurrentPlayer().equals(localPlayerName);
        setActionButtonsEnabled(isLocalPlayerTurn);
    }

    public void showGameResult(String winnerName, HandRank winnerRank) {
        gameResultLabel.setText("Winner: " + winnerName + " with " + winnerRank);


        gameResultLabel.setVisible(true);
    }

    public void showDisconnectedMessage() {

        Dialog dialog = new Dialog("Disconnected", new Skin(Gdx.files.internal("ui/uiskin.json")));
        dialog.text("The server has disconnected.");
        dialog.button("OK");
        dialog.show(stage);
    }

    public void promptForName() {

        nameField.setDisabled(false);
        nameField.setText("");
        nameField.setFocusTraversal(false);
        nameField.setMessageText("Enter Your Name:");
        nameTable.setVisible(true);
    }

    private void setActionButtonsEnabled(boolean enabled) {
        checkButton.setVisible(enabled);
        foldButton.setVisible(enabled);
        callButton.setVisible(enabled);
        raiseButton.setVisible(enabled);

        checkButton.setDisabled(!enabled);
        foldButton.setDisabled(!enabled);
        callButton.setDisabled(!enabled);
        raiseButton.setDisabled(!enabled);
    }

    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public Stage getStage() { return stage; }

    public void dispose() {
        stage.dispose();
    }

    private class PlayerUI {
        private Label nameLabel;
        private Label chipsLabel;
        private Label lastActionLabel;
        private boolean isLocalPlayer;

        public PlayerUI(Skin skin) {
            nameLabel = new Label("", skin);
            chipsLabel = new Label("", skin);
            lastActionLabel = new Label("Last Action: none", skin);


            nameLabel.setColor(Color.WHITE);
            chipsLabel.setColor(Color.WHITE);
            lastActionLabel.setColor(Color.WHITE);
        }

        public void update(String name, int chips, boolean hasFolded, String lastAction) {
            if (isLocalPlayer) {
                nameLabel.setText(name + " (You)");
                nameLabel.setColor(Color.CYAN);
                updatePlayerChips(chips);
            } else {
                nameLabel.setText(name + (hasFolded ? " (Folded)" : ""));
                nameLabel.setColor(hasFolded ? Color.GRAY : Color.WHITE);
            }

            chipsLabel.setText("Chips: " + chips);
            lastActionLabel.setText("Last Action: " + (lastAction != null ? lastAction : "none"));
        }


        public void setLocalPlayer(boolean isLocal) {
            this.isLocalPlayer = isLocal;

            if (isLocalPlayer) {
                nameLabel.setColor(Color.CYAN);
            } else {
                nameLabel.setColor(Color.WHITE);
            }

        }

        public void setPositionBasedOnIndex(int index) {
            float baseX, baseY;


            switch (index) {
                case 0:
                    baseX = 800;
                    baseY = 300;
                    break;
                case 1:
                    baseX = 1500;
                    baseY = 500;
                    break;
                case 2:
                    baseX = 1500;
                    baseY = 900;
                    break;
                case 3:
                    baseX = 900;
                    baseY = 1050;
                    break;
                case 4:
                    baseX = 300;
                    baseY = 900;
                    break;
                case 5:
                    baseX = 300;
                    baseY = 500;
                    break;
                default:
                    return;
            }


            float xPosition = baseX - nameLabel.getWidth() / 2;

            nameLabel.setPosition(xPosition, baseY);
            chipsLabel.setPosition(xPosition, baseY - 20);
            lastActionLabel.setPosition(xPosition, baseY - 40);
        }


        public void setVisible(boolean visible) {
            nameLabel.setVisible(visible);
            chipsLabel.setVisible(visible);
            lastActionLabel.setVisible(visible);
        }
    }

    private void showRaiseDialog() {
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));


        final TextField raiseAmountField = new TextField("", skin);


        Dialog raiseDialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                if (object instanceof Boolean && (Boolean) object) {
                    String text = raiseAmountField.getText().trim();
                    try {
                        int raiseAmount = Integer.parseInt(text);
                        if (raiseAmount > 0 && actionListener != null) {
                            actionListener.onAction(PlayerAction.ActionType.RAISE, raiseAmount);
                            this.hide();
                        } else {

                            showErrorDialog("Invalid raise amount.");
                        }
                    } catch (NumberFormatException e) {
                        showErrorDialog("Please enter a valid number.");
                    }
                } else {

                    this.hide();
                }
            }
        };


        raiseDialog.text("Enter raise amount:");
        raiseDialog.getContentTable().row();
        raiseDialog.getContentTable().add(raiseAmountField).width(200);


        raiseDialog.button("Cancel", false);
        raiseDialog.button("Raise", true);


        raiseDialog.key(com.badlogic.gdx.Input.Keys.ENTER, true);
        raiseDialog.key(com.badlogic.gdx.Input.Keys.ESCAPE, false);


        raiseDialog.show(stage);
    }

    private void showErrorDialog(String message) {
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        Dialog errorDialog = new Dialog("Error", skin);
        errorDialog.text(message);
        errorDialog.button("OK");
        errorDialog.show(stage);
    }
}
