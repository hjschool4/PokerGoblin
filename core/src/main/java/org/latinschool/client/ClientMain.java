
package org.latinschool.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import org.latinschool.client.gui.GameScreen;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.io.IOException;

public class ClientMain extends ApplicationAdapter {
    private ClientNetwork clientNetwork;
    private GameScreen gameScreen;

    @Override
    public void create () {

        gameScreen = new GameScreen();


        clientNetwork = new ClientNetwork(gameScreen);
        clientNetwork.start();
        try {
            clientNetwork.connect("localhost", 54555, 54777);
        } catch (IOException e) {
            e.printStackTrace();

            Gdx.app.postRunnable(() -> {
                Dialog dialog = new Dialog("Connection Failed", new Skin(Gdx.files.internal("ui/uiskin.json")));
                dialog.text("Could not connect to the server.");
                dialog.button("OK");
                dialog.show(gameScreen.getStage());
            });
            return;
        }


        Gdx.input.setInputProcessor(gameScreen.getStage());


        gameScreen.setNameSubmitListener(playerName -> {
            clientNetwork.sendJoinGame(playerName);
            clientNetwork.setLocalPlayerName(playerName);
        });

        gameScreen.setStartGameListener(() -> {
            clientNetwork.sendStartGame();
        });
        gameScreen.setNextGameListener(() -> {
            clientNetwork.sendNextRound();
        });
        gameScreen.setActionListener((actionType, amount) -> {
            clientNetwork.sendPlayerAction(actionType, amount);
        });
    }

    @Override
    public void render () {
        Gdx.gl.glClearColor(0, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        gameScreen.render();
    }

    @Override
    public void dispose () {
        clientNetwork.close();
        gameScreen.dispose();
    }
}
