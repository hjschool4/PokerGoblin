
package org.latinschool.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import org.latinschool.shared.KryoRegistration;
import org.latinschool.shared.messages.*;
import org.latinschool.server.HandEvaluator.HandRank;
import org.latinschool.client.gui.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.io.IOException;

public class ClientNetwork {
    private Client client;
    private GameScreen gameScreen;
    private String playerName;

    public ClientNetwork(GameScreen gameScreen) {
        this.client = new Client();
        this.gameScreen = gameScreen;
        KryoRegistration.register(client.getKryo());
        client.addListener(new ClientListener());
    }

    public void start() {
        client.start();
        System.out.println("Client started.");
    }

    public void connect(String host, int tcpPort, int udpPort) throws IOException {
        System.out.println("Attempting to connect to server at " + host + ":" + tcpPort + "/" + udpPort);
        client.connect(5000, host, tcpPort, udpPort);
        System.out.println("Connected to server.");
    }

    public void sendJoinGame(String playerName) {
        this.playerName = playerName;
        System.out.println("Sending JoinGame message with name: " + playerName);
        client.sendTCP(new JoinGame(playerName));
    }

    public void sendPlayerAction(PlayerAction.ActionType actionType, int amount) {
        System.out.println("Sending PlayerAction: " + actionType + (actionType == PlayerAction.ActionType.RAISE ? " with amount " + amount : ""));
        client.sendTCP(new PlayerAction(actionType, amount));
    }

    public void sendStartGame() {
        System.out.println("Sending StartGameButton message.");
        client.sendTCP(new StartGameButton());
    }
    public void sendNextRound() {
        System.out.println("Sending next Round");
        client.sendTCP(new StartNextGame());
    }

    public void setLocalPlayerName(String name) {
        this.gameScreen.setLocalPlayerName(name);
    }

    public void close() {
        System.out.println("Closing client connection.");
        client.close();
    }

    private class ClientListener extends Listener {
        @Override
        public void connected(Connection connection) {
            System.out.println("Connected to server.");
        }

        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof GameReady) {
                GameReady gameReady = (GameReady) object;
                String[] playerNames = gameReady.getPlayerNames();
                boolean isHost = playerNames.length > 0 && playerName.equals(playerNames[0]);
                Gdx.app.postRunnable(() -> {
                    if (isHost) {
                        gameScreen.showStartGameTextBox();
                    } else {
                        gameScreen.showWaitingForStart();
                    }
                });
            } else if (object instanceof StartGame) {
                Gdx.app.postRunnable(() -> gameScreen.startGame());
            } else if (object instanceof DealCards) {
                DealCards deal = (DealCards) object;
                Gdx.app.postRunnable(() -> gameScreen.receiveDealtCards(deal.getPlayerCards()));
            } else if (object instanceof UpdateGameState) {
                UpdateGameState state = (UpdateGameState) object;
                Gdx.app.postRunnable(() -> gameScreen.updateGameState(state));
            } else if (object instanceof ErrorMessage){
                Gdx.app.postRunnable(() -> gameScreen.showErrorDialog(((ErrorMessage) object).getMessage()));
            }
            else if (object instanceof GameResult) {
                GameResult result = (GameResult) object;
                String[] playerNames = result.getPlayerNames();
                boolean isHost = playerNames.length > 0 && playerName.equals(playerNames[0]);
                if (isHost) {
                    Gdx.app.postRunnable(() -> gameScreen.showNextGame());
                }

                Gdx.app.postRunnable(() -> gameScreen.showGameResult(result.getWinnerName(), result.getWinnerRank()));
            } else if (object instanceof WaitingMessage) {

            } else if (object instanceof ServerFullMessage) {
                ServerFullMessage fullMessage = (ServerFullMessage) object;
                Gdx.app.postRunnable(() -> {
                    Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
                    Dialog dialog = new Dialog("Server Full", skin) {
                        @Override
                        protected void result(Object object) {

                            if (object instanceof String && ((String) object).equals("OK")) {
                                Gdx.app.exit();
                            }
                        }
                    };
                    dialog.text(fullMessage.getMessage());
                    dialog.button("OK", "OK");
                    dialog.show(gameScreen.getStage());
                });
            } else if (object instanceof NameAlreadyTakenMessage) {
                NameAlreadyTakenMessage nameTaken = (NameAlreadyTakenMessage) object;
                Gdx.app.postRunnable(() -> {
                    Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
                    Dialog dialog = new Dialog("Name Taken", skin) {
                        @Override
                        protected void result(Object object) {
                            if (object instanceof String && ((String) object).equals("Retry")) {

                                gameScreen.promptForName();
                            }
                        }
                    };
                    dialog.text(nameTaken.getMessage());
                    dialog.button("Retry", "Retry");
                    dialog.show(gameScreen.getStage());
                });
            }
        }

        @Override
        public void disconnected(Connection connection) {
            Gdx.app.postRunnable(() -> gameScreen.showDisconnectedMessage());
        }
    }
}
