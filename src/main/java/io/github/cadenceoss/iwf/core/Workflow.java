package io.github.cadenceoss.iwf.core;

import io.github.cadenceoss.iwf.core.attributes.QueryAttributeDef;
import io.github.cadenceoss.iwf.core.attributes.SearchAttributeDef;
import io.github.cadenceoss.iwf.core.command.LongRunningActivityDef;
import io.github.cadenceoss.iwf.core.command.SignalChannelDef;

import java.util.Collections;
import java.util.List;

/**
 * This is a simplified Cadence/Temporal workflow. All the complexity of
 * history replay and decision task processing are hidden. No matter how you modify the workflow code, it will never run
 * into non-deterministic errors. The signal/timer/activities will be defined into a way that you don't need to understand
 * what is a workflow decision task and how it's executed.
 * It preserves the capabilities of executed activities, setting timers, processing signals, upsert search attributes, and
 * setting query methods. So basically, you still have the full power of using Cadence/Temporal, without needing to understand
 * the complex technology.
 * The workflow is still defined as code but in a different way. Instead of having a whole piece of workflow method to define the
 * workflow code, you will have to split the logic and place into different states.
 */
public interface Workflow {
    /**
     * defines the states of the workflow. A state represents a step of the workflow state machine.
     * A state can execute some commands (activities/signal/timer) and wait for result
     * See more details in the state definition.
     */
    List<StateDef> getStates();

    /**
     * defines all the signal channels supported by this workflow.
     */
    default List<SignalChannelDef> getSignalChannels() {
        return Collections.emptyList();
    }

    /**
     * defines all the search attributes supported by this workflow.
     */
    default List<SearchAttributeDef> getSearchAttributes() {
        return Collections.emptyList();
    }

    /**
     * defines all the query attributes supported by this workflow.
     */
    default List<QueryAttributeDef<?>> getQueryAttributes() {
        return Collections.emptyList();
    }

    /**
     * defines all the long running activity types supported by this workflow.
     * NOTE that there is NO regular activities in iwf. For non-long-running activities, you just implement them
     * in the workflow state APIs(start/decide).
     */
    default List<LongRunningActivityDef<?>> getLongRunningActivityTypes(){
        return Collections.emptyList();
    }
}

