package Player;

import AdministratorServer.beans.PlayerBean;
import Utils.Coordinate;
import com.greetings.grpc.GreetingServiceGrpc;
import com.greetings.grpc.GreetingServiceOuterClass;
import election.ElectionServiceGrpc;
import election.ElectionServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import reachBase.AccessBaseService;
import reachBase.AccessServiceGrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class NetworkTopologyModule {

    private static NetworkTopologyModule instance = null;
    private HashMap<PlayerBean, Coordinate> playerCoordinateMap;
    public static List<String> playersListToGiveAccess;

    private static Player currentPlayer;

    private String seeker;

    private static boolean allFartherThanMe = true;
    
    private long requestTimeStamp = 0;

    private static int grantCounter = 0;

    private NetworkTopologyModule() {
        playerCoordinateMap = new HashMap<>();
    }

    public static NetworkTopologyModule getInstance() {
        if (instance == null) {
            instance = new NetworkTopologyModule();
        }
        return instance;
    }

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
        this.seeker = seeker;
    }

    public long getRequestTimeStamp() {
        return requestTimeStamp;
    }

    public void setRequestTimeStamp(long requestTimeStamp) {
        this.requestTimeStamp = requestTimeStamp;
    }

    public void incrementGrantCounter(){
        grantCounter++;
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
                    System.out.println("We are at the same distance");
                    return player.getId().compareTo(currentPlayer.getId()) < 0;
                }

                System.out.println("Player distance: " + playerDistance + " Current player distance: " + currentPlayerDistance);

                return playerDistance < currentPlayerDistance;
            }
        }

        return false;
    }

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
        System.out.println(playerCoordinateMap.size());
        for (PlayerBean player : playerCoordinateMap.keySet()) {
            System.out.println(player.getId() + " " + player.getAddress() + " " + player.getPortNumber());
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
        }
    }
    
    public boolean isMyTimeStampMinor(long timeStampRequested){
        return requestTimeStamp < timeStampRequested;
    }

    public void askForAccessToBase(PlayerBean currentPlayer) throws InterruptedException {
        System.out.println("Player " + currentPlayer.getId() + " is asking for access to the base");
        
        requestTimeStamp = System.currentTimeMillis();

        for (PlayerBean player : playerCoordinateMap.keySet()) {
            System.out.println(player.getId() + " " + player.getAddress() + " " + player.getPortNumber());
            if (!Objects.equals(player.getId(), currentPlayer.getId())) {
                // Ask access to the base
                asynchronousAskAccessToBaseCall(player);
            }
        }

        while(grantCounter < playerCoordinateMap.size() - 1){
            Thread.sleep(1000);
        }

        System.out.println("Access granted, I can enter the base");
        System.out.println("I am in the base");
        Thread.sleep(10000);
        System.out.println("I am leaving the base");
        setRequestTimeStamp(0);

        for (String playerId : playersListToGiveAccess) {
            for (PlayerBean player : playerCoordinateMap.keySet()) {
                if (player.getId().equals(playerId)) {
                    // Ask access to the base
                    asynchronousGiveGrantToBaseCall(player);
                }
            }
        }

        //TODO: send a leave game message to all players and stop process
    }

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
        ElectionServiceOuterClass.ElectionRequest request = ElectionServiceOuterClass.ElectionRequest.newBuilder()
                .setPlayerId(player.getId())
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
                .build();
        
        stub.requestAccess(request, new StreamObserver<AccessBaseService.AccessResponse>() {
            @Override
            public void onNext(AccessBaseService.AccessResponse accessResponse) {
                System.out.println("access response " + accessResponse.getGranted() + "");
                if(accessResponse.getGranted()){
                    grantCounter++;
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
    // endregion
}
