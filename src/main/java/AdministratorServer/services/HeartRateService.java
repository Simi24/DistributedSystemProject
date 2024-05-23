package AdministratorServer.services;

import AdministratorServer.beans.ClientMesuramentAverage;
import AdministratorServer.beans.HeartRateDataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("heartRate")
public class HeartRateService {

    @POST
    @Path("/statistics")
    @Consumes({"application/json", "application/xml"})
    public Response receiveHeartRateStatistics(ClientMesuramentAverage heartRateStatistics) {
        boolean flag = HeartRateDataStore.getInstance().addClientMeasurementAverage(heartRateStatistics);
        if (flag) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }
}