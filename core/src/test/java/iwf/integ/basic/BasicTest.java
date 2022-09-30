package iwf.integ.basic;

import iwf.core.Client;
import iwf.core.ClientOptions;
import iwf.core.ImmutableWorkflowStartOptions;
import iwf.core.Registry;
import iwf.spring.TestSingletonWorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BasicTest {

    @BeforeEach
    public void setup() {
        TestSingletonWorkerService.startWorkerIfNotUp();
    }

    @Test
    public void testBasicWorkflow() throws InterruptedException {
        final Registry registry = new Registry();
        final BasicWorkflow wf = new BasicWorkflow();
        registry.addWorkflow(wf);

        final Client client = new Client(registry, ClientOptions.localDefault);
        client.StartWorkflow(BasicWorkflow.class, BasicWorkflowS1.StateId, "basic-test-id" + System.currentTimeMillis() / 1000,
                ImmutableWorkflowStartOptions.builder().workflowTimeoutSeconds(10).build());
        // wait for workflow to finish
        Thread.sleep(5 * 1000);
    }
}