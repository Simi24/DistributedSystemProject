package gRPC;

import AdministratorServer.beans.PlayerBean;
import Player.NetworkTopologyModule;
import Player.Player;
import io.grpc.stub.StreamObserver;
import tagPlayer.TagPlayerServiceGrpc;
import tagPlayer.TagPlayerServiceOuterClass;

public class TagPlayerServiceImp extends TagPlayerServiceGrpc.TagPlayerServiceImplBase {

    @Override
    public void tagPlayer(TagPlayerServiceOuterClass.TagPlayerRequest request, StreamObserver<TagPlayerServiceOuterClass.TagPlayerResponse> responseObserver) {
        System.out.println("------------ I'VE BEEN TAGGED -------------- at timestamp: " + System.currentTimeMillis());

        NetworkTopologyModule networkTopologyModule = NetworkTopologyModule.getInstance();

        boolean amIInTheBase = networkTopologyModule.getAmIInTheBase();

        TagPlayerServiceOuterClass.TagPlayerResponse response = TagPlayerServiceOuterClass.TagPlayerResponse.newBuilder()
                .setSuccess(amIInTheBase)
                .build();


        networkTopologyModule.setiBeenTagged(true);

        responseObserver.onNext(response);

        if(!amIInTheBase) {
            try {
                networkTopologyModule.giveGrantToPlayersInList();
                networkTopologyModule.leaveGame(NetworkTopologyModule.ExitGameReason.TAG);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        responseObserver.onCompleted();
    }
}
