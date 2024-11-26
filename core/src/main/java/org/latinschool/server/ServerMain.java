package org.latinschool.server;

import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import org.latinschool.shared.KryoRegistration;
import org.latinschool.shared.messages.*;
import com.esotericsoftware.kryo.Kryo;

import java.io.IOException;

public class ServerMain {
    private Server server;
    private GameManager gameManager;

    public ServerMain() throws IOException {
        server = new Server();
        gameManager = new GameManager(server);
        KryoRegistration.register(server.getKryo());
        server.start();
        server.bind(54555, 54777);

        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("New client connected: " + connection.getRemoteAddressTCP());
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof JoinGame) {
                    JoinGame joinGame = (JoinGame) object;
                    String desiredName = joinGame.getPlayerName();
                    gameManager.handleJoinGame(connection, desiredName);
                    System.out.println("Received JoinGame from: " + joinGame.getPlayerName());

                } else if (object instanceof PlayerAction) {
                    PlayerAction action = (PlayerAction) object;
                    System.out.println("Received PlayerAction from: " + connection + " Action: " + action.getAction());
                    gameManager.handlePlayerAction(connection, action);
                } else if (object instanceof StartGameButton) {
                    System.out.println("Received StartGameButton from: " + connection);
                    gameManager.handleStartGameButton(connection);
                } else if (object instanceof StartNextGame) {
                    System.out.println("Recieved StartNextGame from: " + connection);
                    gameManager.startPreflop();
                }
                else {
                    System.out.println("Received unknown object: " + object.getClass().getName());
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("Client disconnected: " + connection.getRemoteAddressTCP());
                gameManager.handleDisconnect(connection);
            }
        });

        System.out.println("Server started on ports 54555 (TCP) and 54777 (UDP)");
    }

    public static void main(String[] args) {
        try {
            new ServerMain();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
