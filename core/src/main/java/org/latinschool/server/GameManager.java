
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

    private Integer raiserIndex;

    private HandEvaluator handEvaluator;

    private Map<String, Player> nameToPlayerMap;

    private Queue<Player> playersToAct;
    private List<Player> playersInOrder;

    public int currentBet;
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
        this.currentBet = 0;
        activePlayers = new ArrayList<>();
        this.playersInOrder = new ArrayList<>();
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
        playersInOrder.add(newPlayer);
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
                if (player.getCurrentBet() < currentBet) {
                    player.getConnection().sendTCP(new ErrorMessage("Cannot check. You need to call or raise."));
                    return;
                }
                moveToNextPlayer();
                break;
            case FOLD:
                player.setFolded(true);
                activePlayers.remove(player);
                moveToNextPlayer();
                break;
            case CALL:
                int callAmount = currentBet - player.getCurrentBet();
                if (player.getChips() < callAmount) {
                    callAmount = player.getChips();
                }

                player.bet(callAmount);
                player.setCurrentBet(player.getCurrentBet() + callAmount);
                pot += callAmount;
                moveToNextPlayer();

                break;
            case RAISE:
                int raiseAmount = action.getAmount();
                int newBet = currentBet + raiseAmount;
                if (player.getChips() < raiseAmount) {
                    newBet = player.getCurrentBet() + player.getChips();
                    raiseAmount = player.getChips();
                }
                player.bet(raiseAmount);
                pot += raiseAmount;
                currentBet = newBet;
                player.setCurrentBet(newBet);
                raiserIndex = currentPlayerTurn;
                moveToNextPlayer();
                //resetPlayersToAct(player);
                break;
        }




        promptPlayerAction();
        if(checkRoundOver()) {
            proceedToNextRound();
            resetBets();

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
    private void moveToNextPlayer() {
        currentPlayerTurn = (currentPlayerTurn + 1) % playersInOrder.size();
    }

    public void startPreflop() {
        currentBet = 20;
        List<Player> playerList = new ArrayList<>(players.values());

        Player smallBlindPlayer = playerList.get((dealerIndex + 1) % players.size());
        Player bigBlindPlayer = playerList.get((dealerIndex + 2) % players.size());
        smallBlindPlayer.bet(smallBlind);
        smallBlindPlayer.setCurrentBet(10);
        bigBlindPlayer.bet(bigBlind);
        bigBlindPlayer.setCurrentBet(20);
        pot += smallBlind + bigBlind;

        raiserIndex = 1;



        for (Player player : players.values()) {
            Card[] playerCards = {deck.deal(), deck.deal()};
            player.setCards(playerCards);
            player.getConnection().sendTCP(new DealCards(playerCards));
            player.setLastAction("none");
        }

        server.sendToAllTCP(new StartGame());
        currentPlayerTurn = (raiserIndex + 1) % playersInOrder.size();
        promptPlayerAction();
        smallBlindPlayer.setLastAction("SMALL_BLIND " + smallBlind);
        bigBlindPlayer.setLastAction("BIG_BLIND " + bigBlind);
        initializePlayersToAct(playerList);
    }
    private boolean checkRoundOver() {
        /*if (playersToAct.isEmpty()) {

            return true;
        }*/
        if (currentPlayerTurn == raiserIndex) {
            return true;
        }
        return false;
    }
    private void resetBets() {
        for (Player p : playersInOrder) {
            p.setCurrentBet(0);
            p.setLastAction("None");
        }
        currentBet = 0;
        currentPlayerTurn = 0;
        raiserIndex = -1;
    }

    private void promptPlayerAction() {

        Player currentPlayer = playersInOrder.get(currentPlayerTurn);

        if (currentPlayer.hasFolded()) {
            currentPlayerTurn = (raiserIndex + 1) % playersInOrder.size();
            promptPlayerAction();
            return;
        }

        currentPlayer.setTurn(true);

        /*
        UpdateGameState.PlayerStatus[] statuses = new UpdateGameState.PlayerStatus[players.size()];
        for (int i = 0; i < players.size(); i++) {
            Player p = playerList.get(i);
            String lastAction = p.getLastAction() != null ? p.getLastAction() : "none";
            statuses[i] = new UpdateGameState.PlayerStatus(p.getName(), p.getChips(), p.hasFolded(), lastAction);
        }
        */
        UpdateGameState.PlayerStatus[] statuses = getPlayerStatuses();

        UpdateGameState state = new UpdateGameState(
            communityCards.toArray(new Card[0]),
            currentPlayer.getName(),
            statuses,
            pot
        );
        System.out.println(currentPlayer.getName());
        server.sendToAllTCP(state);
    }
    private UpdateGameState.PlayerStatus[] getPlayerStatuses() {
        UpdateGameState.PlayerStatus[] statuses = new UpdateGameState.PlayerStatus[playersInOrder.size()];
        for (int i = 0; i < playersInOrder.size(); i++) {
            Player p = playersInOrder.get(i);
            statuses[i] = new UpdateGameState.PlayerStatus(
                p.getName(),
                p.getChips(),
                p.hasFolded(),
                p.getLastAction()
            );
        }
        return statuses;
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
