package io.github.cadenceoss.iwf.integ.signal;

import io.github.cadenceoss.iwf.core.StateDef;
import io.github.cadenceoss.iwf.core.Workflow;
import io.github.cadenceoss.iwf.core.command.SignalChannelDef;

import java.util.Arrays;
import java.util.List;

public class BasicSignalWorkflow implements Workflow {
    @Override
    public List<SignalChannelDef> getSignalChannels() {
        return Arrays.asList(
                SignalChannelDef.create(Integer.class, BasicSignalWorkflowState1.SIGNAL_CHANNEL_NAME_1),
                SignalChannelDef.create(Integer.class, BasicSignalWorkflowState1.SIGNAL_CHANNEL_NAME_2)
        );
    }

    @Override
    public List<StateDef> getStates() {
        return Arrays.asList(
                StateDef.startingState(new BasicSignalWorkflowState1())
        );
    }
}
