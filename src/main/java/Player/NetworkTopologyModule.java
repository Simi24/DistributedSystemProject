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

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class NetworkTopologyModule {

    private static NetworkTopologyModule instance = null;
    private HashMap<PlayerBean, Coordinate> playerCoordinateMap;

    private Player currentPlayer;

    private static boolean allFartherThanMe = false;

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
        int size = playerCoordinateMap.size();
        for (PlayerBean player : playerCoordinateMap.keySet()) {
            if (!player.equals(currentPlayer)) {
                // Send the current player's coordinates to each other player
                asynchronousStartElectionCall(player, currentCoordinate);
            }
        }

        if(allFartherThanMe){
            System.out.println("I am the leader");
            this.currentPlayer.setIsSeeker(true);
        }
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
                        .setId(player.getId())
                        .setAddress(player.getAddress())
                        .setCoordinates(GreetingServiceOuterClass.Player.Coordinates.newBuilder()
                                .setX(coordinate.getX())
                                .setY(coordinate.getY())
                                .build())
                        .build())
                .build();

        stub.sayHello(request, new StreamObserver<GreetingServiceOuterClass.HelloReply>() {

            @Override
            public void onNext(GreetingServiceOuterClass.HelloReply helloReply) {
                System.out.println(helloReply.getMessage() + "");
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
                allFartherThanMe = allFartherThanMe && Boolean.parseBoolean(electionResponse.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error! " + throwable.getMessage());
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
