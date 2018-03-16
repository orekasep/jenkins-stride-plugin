package jenkins.plugins.stride;

import org.junit.Test;

public class StandardStrideServiceTest {

    /**
     * Publish should generally not rethrow exceptions, or it will cause a build job to fail at end.
     */
    @Test
    public void publishWithBadHostShouldNotRethrowExceptions() {
        StandardStrideService service = new StandardStrideService("token", "http://hostvaluethatwillcausepublishtofail/room");
        service.publish("message");
    }
}
