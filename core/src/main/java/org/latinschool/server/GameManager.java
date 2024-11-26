
package org.latinschool.server;

import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.Connection;
import org.latinschool.shared.Card;
import org.latinschool.server.HandEvaluator;
import org.latinschool.server.HandEvaluator.HandRank;
import org.latinschool.shared.messages.*;

import java.util.*;

public class GameManager {
    private static final int MAX_PLAYERS = 6;
    private Server server;
    private Map<Connection, Player> players;
    private Deck deck;
    private List<Card> communityCards;
    private int pot;
    private int dealerIndex;
    private int smallBlind;
    private int bigBlind;
    private int currentPlayerTurn;
    private boolean gameStarted;

    private Player lastraiser;

    private HandEvaluator handEvaluator;

    private Map<String, Player> nameToPlayerMap;

    private Queue<Player> playersToAct;


    public int minbet;
    private List<Player> activePlayers;
    public GameManager(Server server) {
        this.server = server;
        this.players = new LinkedHashMap<>();
        playersToAct = new LinkedList<>();
        this.deck = new Deck();
        this.communityCards = new ArrayList<>();
        this.pot = 0;
        this.dealerIndex = -1;
        this.smallBlind = 10;
        this.bigBlind = 20;
        this.currentPlayerTurn = 0;
        this.gameStarted = false;
        this.handEvaluator = new HandEvaluator();
        this.nameToPlayerMap = new HashMap<>();
        this.minbet = 20;
        this.lastraiser = null;
        activePlayers = new ArrayList<>();
    }

    public void handleJoinGame(Connection connection, String desiredName) {
        if (players.size() >= MAX_PLAYERS) {

            connection.sendTCP(new ServerFullMessage("Server is full. Cannot join the game."));
            return;
        }


        if (nameToPlayerMap.containsKey(desiredName)) {

            connection.sendTCP(new NameAlreadyTakenMessage("The name '" + desiredName + "' is already taken. Please choose another name."));
            return;
        }


        Player newPlayer = new Player(connection, desiredName);
        players.put(connection, newPlayer);
        nameToPlayerMap.put(desiredName, newPlayer);


        broadcastPlayerList();
    }

    public void handleDisconnect(Connection connection) {
        Player disconnectedPlayer = players.remove(connection);
        if (disconnectedPlayer != null) {
            String name = disconnectedPlayer.getName();
            nameToPlayerMap.remove(name);


            broadcastPlayerList();


        }
    }
    public void updateGameState(UpdateGameState state) {
        for (Connection conn : players.keySet()) {
            conn.sendTCP(state);
        }
    }
    public void addPlayer(Connection connection, String playerName) {
        if (players.size() >= 6) {
            connection.sendTCP(new ServerFullMessage("Server is full. Cannot join the game."));
            return;
        }
        Player player = new Player(connection, playerName);
        players.put(connection, player);
        broadcastPlayerList();

        if (players.size() >= 2 && !gameStarted) {
            server.sendToAllTCP(new GameReady(getPlayerNames()));
        } else {
            connection.sendTCP(new WaitingMessage("Waiting for another player to join..."));
        }
    }

    public void removePlayer(Connection connection) {
        Player player = players.remove(connection);
        if (player != null) {
            broadcastPlayerList();
            if (gameStarted) {
                endGameDueToDisconnection();
            }
        }
    }

    private String[] getPlayerNames() {
        return players.values().stream()
            .map(Player::getName)
            .toArray(String[]::new);
    }

    private void broadcastPlayerList() {

        String[] playerNames = players.values().stream()
            .map(Player::getName)
            .toArray(String[]::new);


        GameReady gameReady = new GameReady(playerNames);


        for (Connection conn : players.keySet()) {
            conn.sendTCP(gameReady);
        }
    }

    public void handlePlayerAction(Connection connection, PlayerAction action) {
        Player player = players.get(connection);
        if (player == null || !player.isTurn()) {
            connection.sendTCP(new WaitingMessage("It's not your turn."));
            return;
        }


        String actionStr = action.getAction().name();
        if (action.getAction() == PlayerAction.ActionType.RAISE) {
            actionStr += " " + action.getAmount();
        }
        player.setLastAction(actionStr);

        switch (action.getAction()) {
            case CHECK:
                if (minbet - player.getContributed() != 0) {
                    player.setFolded(true);
                    player.setLastAction(PlayerAction.ActionType.FOLD.name());
                }
                playersToAct.remove(player);
                break;
            case FOLD:
                player.setFolded(true);
                activePlayers.remove(player);
                playersToAct.remove(player);
                break;
            case CALL:
                int callAmount = minbet - player.getContributed();
                System.out.println("player: " + player.getName() + " minbet: " + minbet + " contibuted: " + player.getContributed() + " call ammount: " + callAmount);
                if (player.getChips() < callAmount) {
                    callAmount = player.getChips();
                }
                player.bet(callAmount);
                player.addContributed(callAmount);
                pot += callAmount;
                playersToAct.remove(player);

                break;
            case RAISE:
                int raiseAmount = action.getAmount();
                if (player.getChips() < (raiseAmount + (minbet - player.getContributed()))) {
                    raiseAmount = player.getChips();
                }
                player.bet(raiseAmount  + (minbet - player.getContributed()));
                pot += raiseAmount;
                minbet += action.getAmount();
                player.addContributed(raiseAmount);
                lastraiser = player;


                resetPlayersToAct(player);
                break;
        }




        currentPlayerTurn = (currentPlayerTurn + 1) % players.size();
        promptPlayerAction();
        if(checkRoundOver()) {
            proceedToNextRound();
            resetPlayersToAct(players.get((currentPlayerTurn -1) % players.size()));
            resetAllContributed();
            minbet = 0;

        }




    }

    public void handleStartGameButton(Connection connection) {
        if (gameStarted) return;
        gameStarted = true;
        deck.shuffle();
        dealerIndex = (dealerIndex + 1) % players.size();
        startPreflop();
    }
    private void initializePlayersToAct(List<Player> playerList) {
        playersToAct.clear();
        int totalPlayers = playerList.size();
        for (int i = 0; i < totalPlayers; i++) {
            int playerIndex = (dealerIndex + 2 + i) % totalPlayers;
            Player player = playerList.get(playerIndex);
            if (!player.hasFolded() && player.getChips() != 0) {
                playersToAct.add(player);
            }
        }
    }
    private void resetPlayersToAct(Player raiser) {
        playersToAct.clear();
        for (Player p : activePlayers) {
            if (!p.equals(raiser) && !p.hasFolded()){
                playersToAct.add(p);
            }
        }
    }
    private void resetAllContributed() {
        for (Player p : players.values()) {
            p.resetContributed();
        }
    }
    public void startPreflop() {

        List<Player> playerList = new ArrayList<>(players.values());

        Player smallBlindPlayer = playerList.get((dealerIndex + 1) % players.size());
        Player bigBlindPlayer = playerList.get((dealerIndex + 2) % players.size());
        smallBlindPlayer.bet(smallBlind);
        smallBlindPlayer.addContributed(10);
        bigBlindPlayer.bet(bigBlind);
        bigBlindPlayer.addContributed(20);
        pot += smallBlind + bigBlind;





        for (Player player : players.values()) {
            Card[] playerCards = {deck.deal(), deck.deal()};
            player.setCards(playerCards);
            player.getConnection().sendTCP(new DealCards(playerCards));
            player.setLastAction("none");
        }

        server.sendToAllTCP(new StartGame());
        currentPlayerTurn = (dealerIndex + 3) % players.size();
        promptPlayerAction();
        smallBlindPlayer.setLastAction("SMALL_BLIND " + smallBlind);
        bigBlindPlayer.setLastAction("BIG_BLIND " + bigBlind);
        initializePlayersToAct(playerList);
    }
    private boolean checkRoundOver() {
        if (playersToAct.isEmpty()) {

            return true;
        }
        return false;
    }

    private void promptPlayerAction() {
        List<Player> playerList = new ArrayList<>(players.values());
        Player currentPlayer = playerList.get(currentPlayerTurn);

        if (currentPlayer.hasFolded()) {
            currentPlayerTurn = (currentPlayerTurn + 1) % players.size();
            promptPlayerAction();
            return;
        }

        currentPlayer.setTurn(true);


        UpdateGameState.PlayerStatus[] statuses = new UpdateGameState.PlayerStatus[players.size()];
        for (int i = 0; i < players.size(); i++) {
            Player p = playerList.get(i);
            String lastAction = p.getLastAction() != null ? p.getLastAction() : "none";
            statuses[i] = new UpdateGameState.PlayerStatus(p.getName(), p.getChips(), p.hasFolded(), lastAction);
        }


        UpdateGameState state = new UpdateGameState(
            communityCards.toArray(new Card[0]),
            currentPlayer.getName(),
            statuses,
            pot
        );
        server.sendToAllTCP(state);
    }
    public void handleNextGame() {

    }
    private void proceedToNextRound() {
        if (communityCards.size() < 5) {
            if (communityCards.size() == 0) {
                communityCards.add(deck.deal());
                communityCards.add(deck.deal());
                communityCards.add(deck.deal());
            } else if (communityCards.size() == 3) {
                communityCards.add(deck.deal());
            } else if (communityCards.size() == 4) {
                communityCards.add(deck.deal());
            }


        } else {
            showdown();
        }
    }

    private void showdown() {
        String[] playerNames = players.values().stream()
            .map(Player::getName)
            .toArray(String[]::new);
        List<Player> activePlayers = new ArrayList<>();
        for (Player player : players.values()) {
            if (!player.hasFolded()) {
                activePlayers.add(player);
            }
        }

        if (activePlayers.isEmpty()) {
            gameStarted = false;
            return;
        }

        Map<Player, HandEvaluator.EvaluatedHand> playerHands = new HashMap<>();
        for (Player player : activePlayers) {
            List<Card> allCards = new ArrayList<>();
            allCards.addAll(Arrays.asList(player.getCards()));
            allCards.addAll(communityCards);
            HandEvaluator.EvaluatedHand evaluated = handEvaluator.evaluateHand(allCards);
            playerHands.put(player, evaluated);
        }

        Player winner = null;
        HandEvaluator.EvaluatedHand bestHand = null;
        for (Map.Entry<Player, HandEvaluator.EvaluatedHand> entry : playerHands.entrySet()) {
            if (bestHand == null || entry.getValue().compareTo(bestHand) > 0) {
                bestHand = entry.getValue();
                winner = entry.getKey();
            }
        }

        if (winner != null && bestHand != null) {
            winner.addChips(pot);
            server.sendToAllTCP(new GameResult(winner.getName(), bestHand.getRank(), playerNames));
        }


        gameStarted = false;
        communityCards.clear();
        deck = new Deck();
        deck.shuffle();
        pot = 0;
        dealerIndex = (dealerIndex + 1) % players.size();


        for (Player player : players.values()) {
            player.reset();
            player.setLastAction("none");
        }

    }

    private void endGameDueToDisconnection() {
        String[] playerNames = players.values().stream()
            .map(Player::getName)
            .toArray(String[]::new);
        server.sendToAllTCP(new GameResult("No Winner", HandRank.HIGH_CARD, playerNames));
        gameStarted = false;
        communityCards.clear();
        deck = new Deck();
        deck.shuffle();
        pot = 0;
        dealerIndex = -1;


        for (Player player : players.values()) {
            player.reset();
            player.setLastAction("none");
        }
    }
}
