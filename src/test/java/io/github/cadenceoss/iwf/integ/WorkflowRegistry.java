package io.github.cadenceoss.iwf.integ;

import io.github.cadenceoss.iwf.core.Registry;
import io.github.cadenceoss.iwf.integ.attribute.BasicAttributeWorkflow;
import io.github.cadenceoss.iwf.integ.basic.BasicWorkflow;
import io.github.cadenceoss.iwf.integ.interstatechannel.BasicInterStateChannelWorkflow;
import io.github.cadenceoss.iwf.integ.signal.BasicSignalWorkflow;
import io.github.cadenceoss.iwf.integ.timer.BasicTimerWorkflow;

public class WorkflowRegistry {
    public static final Registry registry = new Registry();

    // TODO move to each workflow implementation
    static {
        registry.addWorkflow(new BasicWorkflow());
        registry.addWorkflow(new BasicSignalWorkflow());
        registry.addWorkflow(new BasicAttributeWorkflow());
        registry.addWorkflow(new BasicTimerWorkflow());
        registry.addWorkflow(new BasicInterStateChannelWorkflow());
    }
}