package Player;

import AdministratorServer.beans.PlayerBean;
import Utils.Coordinate;
import com.greetings.grpc.GreetingServiceGrpc;
import com.greetings.grpc.GreetingServiceOuterClass;
import election.ElectionServiceGrpc;
import election.ElectionServiceOuterClass;
import exitGame.ExitGameServiceGrpc;
import exitGame.ExitGameServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import reachBase.AccessBaseService;
import reachBase.AccessServiceGrpc;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkTopologyModule {

    private static NetworkTopologyModule instance = null;
    private static HashMap<PlayerBean, Coordinate> playerCoordinateMap;
    public static List<String> playersListToGiveAccess = new ArrayList<>();
    private HashMap<PlayerBean, Coordinate> playersMapWithoutSeeker;

    private static Player currentPlayer;

    private String seeker;

    private static boolean allFartherThanMe = true;

    private boolean isUpdating = false;

    private long requestTimeStamp = 0;

    private static int grantCounter = 0;

    private NetworkTopologyModule() {
        playerCoordinateMap = new HashMap<>();
        playersMapWithoutSeeker = new HashMap<>();
    }

    public static final Object lock = new Object();
    public final Object updatingLock = new Object();

    public static NetworkTopologyModule getInstance() {
        if (instance == null) {
            instance = new NetworkTopologyModule();
        }
        return instance;
    }

    //region Getters and Setters

    public HashMap<PlayerBean, Coordinate> getPlayerCoordinateMap() {
        return playerCoordinateMap;
    }

    public void setPlayerCoordinateMap(HashMap<PlayerBean, Coordinate> playerCoordinateMap) {
        this.playerCoordinateMap = playerCoordinateMap;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public String getSeeker() {
        return seeker;
    }

    public void setSeeker(String seekerId) {
        this.seeker = seekerId;
    }

    public long getRequestTimeStamp() {
        return requestTimeStamp;
    }

    public void setRequestTimeStamp(long requestTimeStamp) {
        this.requestTimeStamp = requestTimeStamp;
    }

    //endregion

    //region Asynchronous calls methods

    public void sendPlayerCoordinates(PlayerBean currentPlayer, Coordinate currentCoordinate) throws InterruptedException {
        for (PlayerBean player : playerCoordinateMap.keySet()) {
            if (!player.equals(currentPlayer)) {
                // Send the current player's coordinates to each other player
                asynchronousGreetingsCall(player, currentCoordinate);
            }
        }
    }

    public void startElection(PlayerBean currentPlayer, Coordinate currentCoordinate) throws InterruptedException {
        System.out.println("Starting election");
        System.out.println("Size of player map: " + playerCoordinateMap.size());
        for (PlayerBean player : playerCoordinateMap.keySet()) {
            if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                // Send the current player's coordinates to each other player
                asynchronousStartElectionCall(player, currentCoordinate);
            }
        }

        if(allFartherThanMe){
            System.out.println("I am the leader");
            Player.setIsSeeker(true);
            setSeeker(NetworkTopologyModule.currentPlayer.getId());
            for (PlayerBean player : playerCoordinateMap.keySet()) {
                System.out.println(player.getId() + " " + player.getAddress() + " " + player.getPortNumber());
                if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                    // Send the current player's coordinates to each other player
                    declareVictoryCall(player, currentCoordinate);
                }
            }

//            synchronized (this.getCurrentPlayer().lock) {
//                this.getCurrentPlayer().lock.notify();
//            }
        }

    }

    public void askForAccessToBase(PlayerBean currentPlayer) throws InterruptedException {
        
        requestTimeStamp = System.currentTimeMillis();

        System.out.println("PlayerCoordinateMap size after election process: " + playersMapWithoutSeeker.size());

        for (PlayerBean player : playersMapWithoutSeeker.keySet()) {
            System.out.println(player.getId() + " " + player.getAddress() + " " + player.getPortNumber());
            if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                // Ask access to the base
                System.out.println("Player " + currentPlayer.getId() + " is asking for access to the base at player " + player.getId() + " at timestamp " + requestTimeStamp + " ");
                asynchronousAskAccessToBaseCall(player);
            }
        }

        synchronized (lock) {
            while(!checkEnterCondition()){
                try {
                    // Aspetta che grantCounter raggiunga il valore desiderato
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Ripristina lo stato interrotto
                    System.out.println("Thread was interrupted, failed to complete operation");
                }
            }

            System.out.println("Access granted, I can enter the base");
            System.out.println("I am in the base");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Ripristina lo stato interrotto
                System.out.println("Thread was interrupted, failed to complete operation");
            }
            System.out.println("I am leaving the base");
            setRequestTimeStamp(0);
            try {
                giveGrantToPlayersInList();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Ripristina lo stato interrotto
                System.out.println("Thread was interrupted, failed to complete operation");
            }
            try {
                leaveGame();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Ripristina lo stato interrotto
                System.out.println("Thread was interrupted, failed to complete operation");
            }
        }


    }

    private synchronized boolean checkEnterCondition(){
        return grantCounter == playersMapWithoutSeeker.size() - 1;
    }

    public void giveGrantToPlayersInList() throws InterruptedException {
        if (playersListToGiveAccess == null || playersListToGiveAccess.isEmpty()) {
            return;
        }

        synchronized (updatingLock) {
            while (isUpdating) {
                try {
                    updatingLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread was interrupted, failed to complete operation");
                }
            }

            List<String> playersListCopy = new ArrayList<>(playersListToGiveAccess);

            System.out.println("Players list to give access: " + playersListCopy);

            for (String playerId : playersListCopy) {
                for (PlayerBean player : playerCoordinateMap.keySet()) {
                    if (player.getId().equals(playerId)) {
                        System.out.println("Giving access to player " + player.getId());
                        asynchronousGiveGrantToBaseCall(player);
                    }
                }
            }
        }
    }

    public void leaveGame() throws InterruptedException {
        synchronized (updatingLock) {
            while (isUpdating) {
                try {
                    updatingLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread was interrupted, failed to complete operation");
                }
            }

            isUpdating = true;
            for (PlayerBean player : playerCoordinateMap.keySet()) {
                if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                    // Ask access to the base
                    asynchronousLeaveGameCall(player, ExitGameReason.SAVE);
                }
            }
            isUpdating = false;
            updatingLock.notifyAll();
        }
    }

    //endregion

    // region gRPC
    public static void asynchronousGreetingsCall(PlayerBean player, Coordinate coordinate) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(player.getAddress() + ":" + player.getPortNumber()).usePlaintext().build();
        GreetingServiceGrpc.GreetingServiceStub stub = GreetingServiceGrpc.newStub(channel);
        GreetingServiceOuterClass.HelloRequest request = GreetingServiceOuterClass.HelloRequest.newBuilder()
                .setPlayer(GreetingServiceOuterClass.Player.newBuilder()
                        .setId(currentPlayer.getId())
                        .setAddress(currentPlayer.getAddress())
                        .setPortNumber(Integer.parseInt(currentPlayer.getPort()))
                        .setCoordinates(GreetingServiceOuterClass.Player.Coordinates.newBuilder()
                                .setX(coordinate.getX())
                                .setY(coordinate.getY())
                                .build())
                        .build())
                .build();

        stub.sayHello(request, new StreamObserver<GreetingServiceOuterClass.HelloReply>() {

            @Override
            public void onNext(GreetingServiceOuterClass.HelloReply helloReply) {
                System.out.println("Hello reply: " + helloReply.getMessage() + "");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                channel.shutdownNow();
            }
        });

        //you need this. otherwise the method will terminate before that answers from the server are received
        channel.awaitTermination(10, TimeUnit.SECONDS);
    }

    public static void asynchronousStartElectionCall(PlayerBean player, Coordinate coordinate) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(player.getAddress() + ":" + player.getPortNumber()).usePlaintext().build();
        ElectionServiceGrpc.ElectionServiceStub stub = ElectionServiceGrpc.newStub(channel);
        System.out.println("Sending election request to " + player.getId());
        ElectionServiceOuterClass.ElectionRequest request = ElectionServiceOuterClass.ElectionRequest.newBuilder()
                .setPlayerId(currentPlayer.getId())
                .setCoordinates(ElectionServiceOuterClass.Coordinates.newBuilder()
                        .setX(coordinate.getX())
                        .setY(coordinate.getY())
                        .build())
                .build();

        stub.startElection(request, new StreamObserver<ElectionServiceOuterClass.ElectionResponse>() {
            @Override
            public void onNext(ElectionServiceOuterClass.ElectionResponse electionResponse) {
                System.out.println("Election response: " + electionResponse.getMessage() + "");
                allFartherThanMe = allFartherThanMe && Boolean.parseBoolean(electionResponse.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        //you need this. otherwise the method will terminate before that answers from the server are received
        channel.awaitTermination(10, TimeUnit.SECONDS);
    }

    public static void declareVictoryCall(PlayerBean player, Coordinate coordinate) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(player.getAddress() + ":" + player.getPortNumber()).usePlaintext().build();
        ElectionServiceGrpc.ElectionServiceStub stub = ElectionServiceGrpc.newStub(channel);
        ElectionServiceOuterClass.ElectionRequest request = ElectionServiceOuterClass.ElectionRequest.newBuilder()
                .setPlayerId(currentPlayer.getId())
                .setCoordinates(ElectionServiceOuterClass.Coordinates.newBuilder()
                        .setX(coordinate.getX())
                        .setY(coordinate.getY())
                        .build())
                .build();

        stub.declareVictory(request, new StreamObserver<ElectionServiceOuterClass.ElectionResponse>() {
            @Override
            public void onNext(ElectionServiceOuterClass.ElectionResponse electionResponse) {
                System.out.println("Declare victory response: " + electionResponse.getMessage() + "");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        //you need this. otherwise the method will terminate before that answers from the server are received
        channel.awaitTermination(10, TimeUnit.SECONDS);
    }

    public static void asynchronousAskAccessToBaseCall(PlayerBean player) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(player.getAddress() + ":" + player.getPortNumber()).usePlaintext().build();
        AccessServiceGrpc.AccessServiceStub stub = AccessServiceGrpc.newStub(channel);
        AccessBaseService.AccessRequest request = AccessBaseService.AccessRequest.newBuilder()
                .setId(currentPlayer.getId())
                .setTimestamp(getInstance().getRequestTimeStamp())
                .build();
        
        stub.requestAccess(request, new StreamObserver<AccessBaseService.AccessResponse>() {
            @Override
            public void onNext(AccessBaseService.AccessResponse accessResponse) {
                System.out.println("access response " + accessResponse.getGranted() + "");
                if(accessResponse.getGranted()) {
                    try {
                        synchronized (lock) {
                            // Increment the grant counter (number of players that granted access to the base
                            grantCounter++;
                            System.out.println("Grant counter incremented, the new value is: " + grantCounter);
                            lock.notify();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        //you need this. otherwise the method will terminate before that answers from the server are received
        channel.awaitTermination(10, TimeUnit.SECONDS);
    }

    public static void asynchronousGiveGrantToBaseCall(PlayerBean player) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(player.getAddress() + ":" + player.getPortNumber()).usePlaintext().build();
        AccessServiceGrpc.AccessServiceStub stub = AccessServiceGrpc.newStub(channel);
        AccessBaseService.AccessResponse request = AccessBaseService.AccessResponse.newBuilder()
                .setGranted(true)
                .build();

        stub.grantAccess(request, new StreamObserver<AccessBaseService.Thanks>() {
            @Override
            public void onNext(AccessBaseService.Thanks thanks) {
                System.out.println("thanks response " + thanks.getMessage() + "");
                playersListToGiveAccess.remove(player.getId());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        //you need this. otherwise the method will terminate before that answers from the server are received
        channel.awaitTermination(10, TimeUnit.SECONDS);
    }

    public static void asynchronousLeaveGameCall(PlayerBean player, ExitGameReason reason) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(player.getAddress() + ":" + player.getPortNumber()).usePlaintext().build();
        ExitGameServiceGrpc.ExitGameServiceStub stub = ExitGameServiceGrpc.newStub(channel);
        ExitGameServiceOuterClass.ExitGameRequest request;
        if (Objects.requireNonNull(reason) == ExitGameReason.TAG) {
            request = ExitGameServiceOuterClass.ExitGameRequest.newBuilder()
                    .setId(currentPlayer.getId())
                    .setReason(ExitGameServiceOuterClass.ExitGameReason.TAG)
                    .build();
        } else {
            request = ExitGameServiceOuterClass.ExitGameRequest.newBuilder()
                    .setId(currentPlayer.getId())
                    .setReason(ExitGameServiceOuterClass.ExitGameReason.SAVE)
                    .build();
        }

        stub.exitGame(request, new StreamObserver<ExitGameServiceOuterClass.ExitGameResponse>() {
            @Override
            public void onNext(ExitGameServiceOuterClass.ExitGameResponse exitGameResponse) {
                System.out.println("Exit game response: " + exitGameResponse.getMessage() + "");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        //you need this. otherwise the method will terminate before that answers from the server are received
        channel.awaitTermination(10, TimeUnit.SECONDS);
    }
    // endregion

    //region Utils

    public void addNewPlayerToNetworkTopology(PlayerBean player, Coordinate newCoordinate) {
        playerCoordinateMap.put(player, newCoordinate);
    }

    public void updatePlayerCoordinate(String playerId, Coordinate newCoordinate) {
        for (PlayerBean player : playerCoordinateMap.keySet()) {
            if (player.getId().equals(playerId)) {
                playerCoordinateMap.put(player, newCoordinate);
            }
        }
    }

    public void removePlayerFromNetworkTopology(String playerId) {
        synchronized (updatingLock) {
            while (isUpdating) {
                try {
                    updatingLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread was interrupted, failed to complete operation");
                }
            }

            isUpdating = true;
            Iterator<Map.Entry<PlayerBean, Coordinate>> iterator = playerCoordinateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PlayerBean, Coordinate> entry = iterator.next();
                if (entry.getKey().getId().equals(playerId)) {
                    iterator.remove();
                    break;
                }
            }
            isUpdating = false;
            updatingLock.notifyAll();
        }
    }

    public void incrementGrantCounter(){
        synchronized (lock) {
            grantCounter++;
            System.out.println("Grant counter: " + grantCounter);
            lock.notify();
        }
    }


    public boolean isMyTimeStampMinor(long timeStampRequested){
        System.out.println("My timestamp: " + requestTimeStamp + " Requested timestamp: " + timeStampRequested);
        if(requestTimeStamp == 0){
            return false;
        }
        return requestTimeStamp < timeStampRequested;
    }


    public double calculateDistanceToBase(Coordinate playerCoordinate) {
        int baseX1 = 4, baseY1 = 4, baseX2 = 5, baseY2 = 5;

        int playerX = playerCoordinate.getX();
        int playerY = playerCoordinate.getY();

        if (playerX < baseX1) {
            playerX = baseX1;
        } else if (playerX > baseX2) {
            playerX = baseX2;
        }

        if (playerY < baseY1) {
            playerY = baseY1;
        } else if (playerY > baseY2) {
            playerY = baseY2;
        }

        int dx = playerX - playerCoordinate.getX();
        int dy = playerY - playerCoordinate.getY();

        return Math.sqrt(dx * dx + dy * dy);
    }

    public Boolean playerIsCloserThenCurrentPlayer(String playerId) {
        Coordinate currentPlayerCoordinate = currentPlayer.getCoordinate();
        double currentPlayerDistance = calculateDistanceToBase(currentPlayerCoordinate);

        for (PlayerBean player : playerCoordinateMap.keySet()) {
            if (player.getId().equals(playerId)) {
                Coordinate playerCoordinate = playerCoordinateMap.get(player);
                double playerDistance = calculateDistanceToBase(playerCoordinate);

                if(playerDistance == currentPlayerDistance){
                    boolean flag = player.getId().compareTo(currentPlayer.getId()) < 0;
                    System.out.println("We are at the same distance " + flag);
                    System.out.println("Player id: " + player.getId() + " Current player id: " + currentPlayer.getId());
                    return player.getId().compareTo(currentPlayer.getId()) < 0;
                }

//                System.out.println("Player distance: " + playerDistance + " Current player distance: " + currentPlayerDistance);

                return playerDistance < currentPlayerDistance;
            }
        }

        return false;
    }

    public void removeSeekerFromNetworkTopology() {
        System.out.println("Removing seeker from network topology, the seeker is: " + seeker);
        Iterator<Map.Entry<PlayerBean, Coordinate>> iterator = playerCoordinateMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PlayerBean, Coordinate> entry = iterator.next();
            if (!entry.getKey().getId().equals(seeker)) {
                playersMapWithoutSeeker.put(entry.getKey(), entry.getValue());
            }
        }
    }

    //endregion

    //region Enums

    enum ExitGameReason {
        SAVE,
        TAG
    }

    //endregion
}
