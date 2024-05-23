package Player;

import AdministratorServer.beans.Player;
import Utils.Coordinate;
import com.greetings.grpc.GreetingServiceGrpc;
import com.greetings.grpc.GreetingServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class NetworkTopologyModule {

    private static NetworkTopologyModule instance = null;
    private HashMap<Player, Coordinate> playerCoordinateMap;

    private NetworkTopologyModule() {
        playerCoordinateMap = new HashMap<>();
    }

    public static NetworkTopologyModule getInstance() {
        if (instance == null) {
            instance = new NetworkTopologyModule();
        }
        return instance;
    }

    public HashMap<Player, Coordinate> getPlayerCoordinateMap() {
        return playerCoordinateMap;
    }

    public void setPlayerCoordinateMap(HashMap<Player, Coordinate> playerCoordinateMap) {
        this.playerCoordinateMap = playerCoordinateMap;
    }

    public void sendPlayerCoordinates(Player currentPlayer, Coordinate currentCoordinate) throws InterruptedException {
        for (Player player : playerCoordinateMap.keySet()) {
            if (!player.equals(currentPlayer)) {
                // Send the current player's coordinates to each other player
                asynchronousGreetingsCall(player, currentCoordinate);
            }
        }
    }

    public void updateNetworkTopology(Player player, Coordinate newCoordinate) {
        playerCoordinateMap.put(player, newCoordinate);
    }

    // region gRPC

    public static void synchronousSimpleSumCall(Player player, Coordinate coordinate){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(player.getAddress() + ":" + player.getPortNumber()).usePlaintext().build();
        GreetingServiceGrpc.GreetingServiceBlockingStub stub = GreetingServiceGrpc.newBlockingStub(channel);
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

        GreetingServiceOuterClass.HelloReply response = stub.sayHello(request);
        System.out.println(response.getMessage()+"");
        channel.shutdown();
    }

    public static void asynchronousGreetingsCall(Player player, Coordinate coordinate) throws InterruptedException {
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

    // endregion
}
