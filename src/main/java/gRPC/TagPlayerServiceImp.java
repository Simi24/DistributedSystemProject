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
        System.out.println("------------ I'VE BEEN TAGGED --------------");

        NetworkTopologyModule networkTopologyModule = NetworkTopologyModule.getInstance();

        boolean amIInTheBase = networkTopologyModule.getAmIInTheBase();

        TagPlayerServiceOuterClass.TagPlayerResponse response = TagPlayerServiceOuterClass.TagPlayerResponse.newBuilder()
                .setSuccess(amIInTheBase)
                .build();

        responseObserver.onNext(response);

        synchronized (networkTopologyModule.updatingLock) {
            networkTopologyModule.setiBeenTagged(true);
            networkTopologyModule.updatingLock.notifyAll();
        }

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
