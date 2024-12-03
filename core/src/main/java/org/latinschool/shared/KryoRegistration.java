
package org.latinschool.shared;

import com.esotericsoftware.kryo.Kryo;
import org.latinschool.server.HandEvaluator;
import org.latinschool.shared.messages.*;
import org.latinschool.server.HandEvaluator.HandRank;

public class KryoRegistration {
    public static void register(Kryo kryo) {

        kryo.register(BaseMessage.class);


        kryo.register(JoinGame.class);
        kryo.register(StartGame.class);
        kryo.register(DealCards.class);
        kryo.register(PlayerAction.class);
        kryo.register(PlayerAction.ActionType.class);
        kryo.register(UpdateGameState.class);
        kryo.register(UpdateGameState.PlayerStatus.class);
        kryo.register(UpdateGameState.PlayerStatus[].class);
        kryo.register(StartGameButton.class);
        kryo.register(GameReady.class);
        kryo.register(GameResult.class);
        kryo.register(WaitingMessage.class);
        kryo.register(ServerFullMessage.class);
        kryo.register(NameAlreadyTakenMessage.class);
        kryo.register(StartNextGame.class);
        kryo.register(ErrorMessage.class);

        kryo.register(Card.class);
        kryo.register(Suit.class);
        kryo.register(Rank.class);


        kryo.register(HandEvaluator.class);
        kryo.register(HandRank.class);


        kryo.register(Card[].class);
        kryo.register(String[].class);
    }
}
