package io.github.cadenceoss.iwf.core.mapper;

import io.github.cadenceoss.iwf.core.ObjectEncoder;
import io.github.cadenceoss.iwf.gen.models.StateDecision;

import java.util.stream.Collectors;

public class StateDecisionMapper {
    public static StateDecision toGenerated(io.github.cadenceoss.iwf.core.StateDecision stateDecision, final ObjectEncoder objectEncoder) {
        if (stateDecision.getNextStates() == null) {
            return null;
        }
        return new StateDecision()
                .nextStates(stateDecision.getNextStates()
                        .stream()
                        .map(e -> {
                            return StateMovementMapper.toGenerated(e, objectEncoder);
                        })
                        .collect(Collectors.toList()));
    }
}
