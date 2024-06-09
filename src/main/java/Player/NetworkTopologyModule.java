package Player;

import AdministratorServer.beans.PlayerBean;
import Utils.Coordinate;
import com.google.common.base.Verify;
import com.greetings.grpc.GreetingServiceGrpc;
import com.greetings.grpc.GreetingServiceOuterClass;
import election.ElectionServiceGrpc;
import election.ElectionServiceOuterClass;
import exitGame.ExitGameServiceGrpc;
import exitGame.ExitGameServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import reachBase.AccessBaseService;
import reachBase.AccessServiceGrpc;
import tagPlayer.TagPlayerServiceGrpc;
import tagPlayer.TagPlayerServiceOuterClass;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NetworkTopologyModule {

    private static NetworkTopologyModule instance = null;
    private final static HashMap<PlayerBean, Coordinate> playerCoordinateMap = new HashMap<>();
    public static final List<String> playersListToGiveAccess = new ArrayList<>();
    private final static HashMap<PlayerBean, Coordinate> playersMapWithoutSeeker = new HashMap<>();

    private static Player currentPlayer;

    private static String seeker;

    private static int electionVoteCounter = 0;

    private boolean isUpdating = false;

    private long requestTimeStamp = 0;

    private static int grantCounter = 0;

    private boolean amIInTheBase = false;

    private boolean iBeenTagged = false;

    private static GameStatus gameStatus = GameStatus.WAITING;

    private NetworkTopologyModule() {
    }

    public static final Object lock = new Object();
    public final Object updatingLock = new Object();
    private static final Object electionLock = new Object();
    private static final Object gameStatusLock = new Object();

    public static synchronized NetworkTopologyModule getInstance() {
        if (instance == null) {
            instance = new NetworkTopologyModule();
        }
        return instance;
    }

    //region Getters and Setters

    public HashMap<PlayerBean, Coordinate> getPlayerCoordinateMap() {
        return playerCoordinateMap;
    }

    public synchronized void setPlayerCoordinateMap(HashMap<PlayerBean, Coordinate> playerCoordinateMap) {
        for (Map.Entry<PlayerBean, Coordinate> entry : playerCoordinateMap.entrySet()) {
            this.playerCoordinateMap.put(entry.getKey(), entry.getValue());
        }
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

    public int getGameStatus() {
        return gameStatus.ordinal();
    }

    public static void setGameStatus(int gameStatus) {
        NetworkTopologyModule.gameStatus = GameStatus.values()[gameStatus];
    }

    public boolean getAmIInTheBase() {
        return amIInTheBase;
    }

    public void setiBeenTagged(boolean iBeenTagged){
        this.iBeenTagged = iBeenTagged;
    }

    public boolean getiBeenTagged(){
        return iBeenTagged;
    }

    //endregion

    //region Asynchronous calls methods

    public void sendPlayerCoordinates(PlayerBean currentPlayer, Coordinate currentCoordinate) throws InterruptedException {
        List<PlayerBean> playersToSendGreetings;
        synchronized (playerCoordinateMap) {
            System.out.println("Lock acquired");
            playersToSendGreetings = playerCoordinateMap.keySet().stream()
                    .filter(player -> !player.equals(currentPlayer))
                    .collect(Collectors.toList());
            System.out.println("Lock released");
        }

        for (PlayerBean player : playersToSendGreetings) {
            // Send the current player's coordinates to each other player
            asynchronousGreetingsCall(player, currentCoordinate);
        }
    }

    public void startElection(PlayerBean currentPlayer, Coordinate currentCoordinate) throws InterruptedException {
        if (gameStatus == GameStatus.BASE_ACCESS) {
            askForAccessToBase(currentPlayer);
            return;
        }

        Set<PlayerBean> playersToStartElection;
        synchronized (playerCoordinateMap) {

            System.out.println("Starting election");
            System.out.println("Size of player map: " + playerCoordinateMap.size());
            gameStatus = GameStatus.ELECTION;

            playersToStartElection = new HashSet<>(playerCoordinateMap.keySet());
        }

        for (PlayerBean player : playersToStartElection) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted, failed to complete operation");
            }

            synchronized (gameStatusLock) {
                if (gameStatus == GameStatus.ELECTION) {
                    if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                        // Send the current player's coordinates to each other player
                        asynchronousStartElectionCall(player, currentCoordinate);
                    }
                } else {
                    break;
                }
            }
        }

        synchronized (electionLock) {
            while (electionVoteCounter < playerCoordinateMap.size() - 1) {
                try {
                    electionLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread was interrupted, failed to complete operation");
                }
            }
            System.out.println("I AM THE LEADER !!!!");
            Player.setIsSeeker(true);
            setSeeker(NetworkTopologyModule.currentPlayer.getId());
            setGameStatus(GameStatus.BASE_ACCESS.ordinal());
            playersToStartElection = new HashSet<>(playerCoordinateMap.keySet());
            for (PlayerBean player : playersToStartElection) {
                if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                    // Send the current player's coordinates to each other player
                    declareVictoryCall(player, currentCoordinate);
                }
            }
            startSeeker();
        }

    }

    public void askForAccessToBase(PlayerBean currentPlayer) throws InterruptedException {
        gameStatus = GameStatus.BASE_ACCESS;

        requestTimeStamp = System.currentTimeMillis();

        System.out.println("PlayerCoordinateMap size after election process: " + playersMapWithoutSeeker.size());

        synchronized (playersMapWithoutSeeker) {
            for (PlayerBean player : playersMapWithoutSeeker.keySet()) {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    System.out.println("Thread was interrupted, failed to complete operation");
//                }
                if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                    // Ask access to the base
                    if (!iBeenTagged) {
                        System.out.println("Player " + currentPlayer.getId() + " is asking for access to the base at player " + player.getId() + " at timestamp " + requestTimeStamp + " ");
                        asynchronousAskAccessToBaseCall(player);
                    } else {
                        break;
                    }
                }
            }
        }

        synchronized (lock) {
            while(!checkEnterCondition()){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread was interrupted, failed to complete operation");
                }
            }

            System.out.println("Access granted, I can enter the base");
            System.out.println("---------------- I AM IN THE BASE !!!!!" + " at timestamp " + System.currentTimeMillis() + " ");
            amIInTheBase = true;
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted, failed to complete operation");
            }
            System.out.println("---------------- I AM LEAVING THE BASE !!!!!" + " at timestamp " + System.currentTimeMillis());
            amIInTheBase = false;
            setRequestTimeStamp(0);
            try {
                giveGrantToPlayersInList();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted, failed to complete operation");
            }
            try {
                leaveGame(ExitGameReason.SAVE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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

            System.out.println("Players list to give access: " + playersListToGiveAccess);

            for (String playerId : playersListToGiveAccess) {
                for (PlayerBean player : playerCoordinateMap.keySet()) {
                    if (player.getId().equals(playerId)) {
                        System.out.println("Giving access to player " + player.getId());
                        asynchronousGiveGrantToBaseCall(player);
                    }
                }
            }
        }
    }

    public void leaveGame(ExitGameReason reason) throws InterruptedException {
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
                    asynchronousLeaveGameCall(player, reason);
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
                System.out.println("Hello reply: " + helloReply.getGameStatus() + "" + helloReply.getRole());
                if (helloReply.getGameStatus() == GreetingServiceOuterClass.GameStatus.ELECTION && gameStatus != GameStatus.BASE_ACCESS) {
                    System.out.println("Game status is election");
                    System.out.println("Old game status: " + gameStatus);
                    NetworkTopologyModule.getInstance().setGameStatus(GameStatus.ELECTION.ordinal());
                } else if (helloReply.getGameStatus() == GreetingServiceOuterClass.GameStatus.BASE_ACCESS) {
                    System.out.println("Game status is base access");
                    NetworkTopologyModule.getInstance().setGameStatus(GameStatus.BASE_ACCESS.ordinal());
                } else {
                    System.out.println("Game status is waiting");
                    NetworkTopologyModule.getInstance().setGameStatus(GameStatus.WAITING.ordinal());
                }

                if (helloReply.getRole() == GreetingServiceOuterClass.Role.SEEKER) {
                    System.out.println("Role of player " + helloReply.getId() + " is seeker");
                    NetworkTopologyModule.getInstance().setSeeker(helloReply.getId());
                    removeSeekerFromNetworkTopology();
                } else {
                    System.out.println("Role is not seeker");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable instanceof StatusRuntimeException) {
                    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;
                    if (statusRuntimeException.getStatus().getCode() == Status.Code.CANCELLED) {
                        System.out.println("The call was cancelled. SONO NEL GREETING CALL -----------------------");
                        // Handle cancellation
                    } else {
                        System.out.println("Error! " + throwable.getMessage());
                    }
                } else {
                    System.out.println("Error! " + throwable.getMessage());
                }
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
                System.out.println("Election response: " + electionResponse.getMessage() + " from player " + electionResponse.getId());
                synchronized (electionLock) {
                    if (Boolean.parseBoolean(electionResponse.getMessage())) {
                        electionVoteCounter++;
                        System.out.println("Election vote counter incremented, the new value is: " + electionVoteCounter);
                    }
                    electionLock.notify();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable instanceof StatusRuntimeException) {
                    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;
                    if (statusRuntimeException.getStatus().getCode() == Status.Code.CANCELLED) {
                        System.out.println("The call was cancelled. SONO NEL START ELECTION__________________");
                        Status status = Status.fromThrowable(throwable);
                        Verify.verify(status.getCode() == Status.Code.INTERNAL);
                        Verify.verify(status.getDescription().contains("Overbite"));
                        // Handle cancellation
                    } else {
                        System.out.println("Error! " + throwable.getMessage());
                    }
                } else {
                    System.out.println("Error! " + throwable.getMessage());
                }
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
                synchronized (gameStatusLock) {
                    setGameStatus(GameStatus.BASE_ACCESS.ordinal());
                    gameStatusLock.notify();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable instanceof StatusRuntimeException) {
                    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;
                    if (statusRuntimeException.getStatus().getCode() == Status.Code.CANCELLED) {
                        System.out.println("The call was cancelled. SONO NEL DECLARE VICTORY --------------");
                        // Handle cancellation
                    } else {
                        System.out.println("Error! " + throwable.getMessage());
                    }
                } else {
                    System.out.println("Error! " + throwable.getMessage());
                }
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
                            System.out.println(NetworkTopologyModule.getInstance().iBeenTagged + " I have been tagged");
                            if(NetworkTopologyModule.getInstance().iBeenTagged) {
                                lock.wait();
                            }
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
                System.out.println("The call was cancelled. SONO NEL ASK ACCESS TO BASE __________________");
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
                synchronized (getInstance().updatingLock) {
                    getInstance().isUpdating = true;
                    playersListToGiveAccess.remove(player.getId());
                    getInstance().isUpdating = false;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
                System.out.println("The call was cancelled. SONO NEL GIVE GRANT __________________");
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
                System.out.println("The call was cancelled. SONO NEL EXIT GAME __________________");
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        //you need this. otherwise the method will terminate before that answers from the server are received
        channel.awaitTermination(50, TimeUnit.SECONDS);
    }

    public static void asynchronousTagPlayerCall(PlayerBean player) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(player.getAddress() + ":" + player.getPortNumber()).usePlaintext().build();
        TagPlayerServiceGrpc.TagPlayerServiceStub stub = TagPlayerServiceGrpc.newStub(channel);

        TagPlayerServiceOuterClass.TagPlayerRequest request = TagPlayerServiceOuterClass.TagPlayerRequest.newBuilder()
                .setMessage("You have been tagged by the seeker")
                .build();

        stub.tagPlayer(request, new StreamObserver<TagPlayerServiceOuterClass.TagPlayerResponse>() {
            @Override
            public void onNext(TagPlayerServiceOuterClass.TagPlayerResponse tagPlayerResponse) {
                System.out.println("Tag player response: " + tagPlayerResponse.getSuccess() + "");
                NetworkTopologyModule.getInstance().removePlayerFromNetworkTopology(player.getId());
                NetworkTopologyModule.getInstance().moveTowardsClosestPlayer();
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
                System.out.println("The call was cancelled. SONO NEL TAG PLAYER __________________");
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

        synchronized (playerCoordinateMap) {
            playerCoordinateMap.put(player, newCoordinate);
        }

        if (gameStatus == GameStatus.ELECTION) {
            try {
                asynchronousStartElectionCall(player, getCurrentPlayer().getCoordinate());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else if (gameStatus == GameStatus.BASE_ACCESS && !currentPlayer.getIsSeeker() && !amIInTheBase) {
            synchronized (playersMapWithoutSeeker) {
                playersMapWithoutSeeker.put(player, newCoordinate);
            }
            try {
                asynchronousAskAccessToBaseCall(player);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void updatePlayerCoordinate(String playerId, Coordinate newCoordinate) {
        synchronized (playerCoordinateMap) {
            for (PlayerBean player : playerCoordinateMap.keySet()) {
                if (player.getId().equals(playerId)) {
                    playerCoordinateMap.put(player, newCoordinate);
                }
            }
        }
    }

    public void removePlayerFromNetworkTopology(String playerId) {
        if(playerCoordinateMap.size() == 1){
            System.out.println("---------------- GAME IS OVER ----------------");
        } else {

            synchronized (playerCoordinateMap) {
                Iterator<Map.Entry<PlayerBean, Coordinate>> iterator = playerCoordinateMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<PlayerBean, Coordinate> entry = iterator.next();
                    if (entry.getKey().getId().equals(playerId)) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    public void removePlayerFromListOfPlayerToGiveGrant(String playerId) {
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
            synchronized (playersListToGiveAccess) {
                playersListToGiveAccess.remove(playerId);
            }
            isUpdating = false;
            updatingLock.notifyAll();
        }
    }

    public void removePlayerFromMapPlayerWithoutSeeker(String playerId) {
        synchronized (playersMapWithoutSeeker) {
            if(playersMapWithoutSeeker.isEmpty()){
                return;
            }

            Iterator<Map.Entry<PlayerBean, Coordinate>> iterator = playersMapWithoutSeeker.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PlayerBean, Coordinate> entry = iterator.next();
                if (entry.getKey().getId().equals(playerId)) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    public void incrementGrantCounter(){
        synchronized (lock) {
            if(this.iBeenTagged){
                return;
            }
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

    public double calculateDistanceBetweenTwoPlayers(Coordinate player1Coordinate, Coordinate player2Coordinate) {
        int dx = player1Coordinate.getX() - player2Coordinate.getX();
        int dy = player1Coordinate.getY() - player2Coordinate.getY();

        return Math.sqrt(dx * dx + dy * dy);
    }

    public Boolean playerIsCloserThenCurrentPlayer(String playerId) {
        Coordinate currentPlayerCoordinate = currentPlayer.getCoordinate();
        double currentPlayerDistance = calculateDistanceToBase(currentPlayerCoordinate);

        PlayerBean playerToCheck = null;
        synchronized (playerCoordinateMap) {
            System.out.println("Lock acquired in playerIsCloserThenCurrentPlayer");

            for (PlayerBean player : playerCoordinateMap.keySet()) {
                if (player.getId().equals(playerId)) {
                    playerToCheck = player;
                    break;
                }
            }

            System.out.println("Lock released in playerIsCloserThenCurrentPlayer");
        }

        if (playerToCheck != null) {
            Coordinate playerCoordinate = playerCoordinateMap.get(playerToCheck);
            double playerDistance = calculateDistanceToBase(playerCoordinate);

            if (playerDistance == currentPlayerDistance) {
                boolean flag = playerToCheck.getId().compareTo(currentPlayer.getId()) < 0;
                System.out.println("We are at the same distance " + flag);
                System.out.println("Player id: " + playerToCheck.getId() + " Current player id: " + currentPlayer.getId());
                return playerToCheck.getId().compareTo(currentPlayer.getId()) < 0;
            }

            return playerDistance < currentPlayerDistance;
        }

        return false;
    }

    public static void removeSeekerFromNetworkTopology() {
        System.out.println("Removing seeker from network topology, the seeker is: " + seeker);
        List<PlayerBean> playersToKeep = new ArrayList<>();
        synchronized (playerCoordinateMap) {
            Iterator<Map.Entry<PlayerBean, Coordinate>> iterator = playerCoordinateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PlayerBean, Coordinate> entry = iterator.next();
                if (!entry.getKey().getId().equals(seeker)) {
                    playersToKeep.add(entry.getKey());
                }
            }
        }

        for (PlayerBean player : playersToKeep) {
            playersMapWithoutSeeker.put(player, playerCoordinateMap.get(player));
        }
    }
    //endregion

    //region Enums

    public enum ExitGameReason {
        SAVE,
        TAG
    }

    enum GameStatus {
        WAITING,
        ELECTION, BASE_ACCESS,
    }

    //endregion

    //region Seeker

    public void startSeeker(){
        System.out.println("Starting seeker");
        moveTowardsClosestPlayer();
    }

    public void moveTowardsClosestPlayer() {
        PlayerBean closestPlayer = findClosestPlayer();
        if (closestPlayer != null) {
            // Calculate the distance to the closest player
            double distance = calculateDistanceBetweenTwoPlayers(currentPlayer.getCoordinate(), playerCoordinateMap.get(closestPlayer));

            // Calculate the time it takes for the seeker to reach the closest player
            double speed = 2.0;
            long time = (long) (distance / speed * 1000);

            System.out.println("Moving towards player " + closestPlayer.getId() + " with distance " + distance + " and time " + time);

            try {
                // Simulate the movement of the seeker
                Thread.sleep(time);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted, failed to complete operation");
            }

            Coordinate closestPlayerCoordinate = playerCoordinateMap.get(closestPlayer);
            currentPlayer.setCoordinate(closestPlayerCoordinate);

            try {
                asynchronousTagPlayerCall(closestPlayer);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private PlayerBean findClosestPlayer() {
        PlayerBean closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;
        Coordinate seekerCoordinate = currentPlayer.getCoordinate();

        synchronized (playerCoordinateMap) {
            for (PlayerBean player : playerCoordinateMap.keySet()) {
                if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                    double distance = calculateDistanceBetweenTwoPlayers(seekerCoordinate, playerCoordinateMap.get(player));
                    if (distance < closestDistance) {
                        closestPlayer = player;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closestPlayer;
    }

    //endregion
}
