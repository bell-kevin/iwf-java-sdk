package io.github.cadenceoss.iwf.core;

import io.github.cadenceoss.iwf.core.attributes.SearchAttributeType;
import io.github.cadenceoss.iwf.gen.models.KeyValue;
import io.github.cadenceoss.iwf.gen.models.SearchAttribute;
import io.github.cadenceoss.iwf.gen.models.SearchAttributeKeyAndType;
import io.github.cadenceoss.iwf.gen.models.StateCompletionOutput;
import io.github.cadenceoss.iwf.gen.models.WorkflowGetQueryAttributesResponse;
import io.github.cadenceoss.iwf.gen.models.WorkflowGetSearchAttributesResponse;
import io.github.cadenceoss.iwf.gen.models.WorkflowResetRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowSearchResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Client {
    private final Registry registry;

    private final UntypedClient untypedClient;

    final ClientOptions clientOptions;

    public Client(final Registry registry, final ClientOptions clientOptions) {
        this.registry = registry;
        this.clientOptions = clientOptions;
        this.untypedClient = new UntypedClient(clientOptions);
    }

    public String StartWorkflow(
            final Class<? extends Workflow> workflowClass,
            final String startStateId,
            final String workflowId,
            final WorkflowStartOptions options) {
        return StartWorkflow(workflowClass, startStateId, null, workflowId, options);
    }
    
    public String StartWorkflow(
            final Class<? extends Workflow> workflowClass,
            final String startStateId,
            final Object input,
            final String workflowId,
            final WorkflowStartOptions options) {
        final String wfType = workflowClass.getSimpleName();
        final StateDef stateDef = registry.getWorkflowState(wfType, startStateId);
        if (stateDef == null || !stateDef.getCanStartWorkflow()) {
            throw new IllegalArgumentException("invalid start stateId " + startStateId);
        }

        return untypedClient.StartWorkflow(wfType, startStateId, input, workflowId, options);
    }

    /**
     * For most cases, a workflow only has one result(one completion state)
     * Use this API to retrieve the output of the state
     *
     * @param valueClass    the type class of the output
     * @param workflowId    the workflowId
     * @param workflowRunId optional runId, can be empty
     * @param <T>           type of the output
     * @return
     */
    public <T> T GetSimpleWorkflowResultWithWait(
            Class<T> valueClass,
            final String workflowId,
            final String workflowRunId) {
        return untypedClient.GetSimpleWorkflowResultWithWait(valueClass, workflowId, workflowRunId);
    }

    public <T> T GetSimpleWorkflowResultWithWait(
            Class<T> valueClass,
            final String workflowId) {
        return GetSimpleWorkflowResultWithWait(valueClass, workflowId, "");
    }

    /**
     * In some cases, a workflow may have more than one completion states
     * @param workflowId
     * @param workflowRunId
     * @return a list of the state output for completion states. User code will figure how to use ObjectEncoder to decode the output
     */
    public List<StateCompletionOutput> GetComplexWorkflowResultWithWait(
            final String workflowId, final String workflowRunId) {
        return untypedClient.GetComplexWorkflowResultWithWait(workflowId, workflowRunId);
    }

    public List<StateCompletionOutput> GetComplexWorkflowResultWithWait(final String workflowId) {
        return GetComplexWorkflowResultWithWait(workflowId, "");
    }
    public void SignalWorkflow(
            final Class<? extends Workflow> workflowClass,
            final String workflowId,
            final String workflowRunId,
            final String signalChannelName,
            final Object signalValue) {
        final String wfType = workflowClass.getSimpleName();

        Map<String, Class<?>> nameToTypeMap = registry.getSignalChannelNameToSignalTypeMap(wfType);
        if (nameToTypeMap == null) {
            throw new IllegalArgumentException(
                    String.format("Workflow %s is not registered", wfType)
            );
        }

        if (!nameToTypeMap.containsKey(signalChannelName)) {
            throw new IllegalArgumentException(String.format("Workflow %s doesn't have signal %s", wfType, signalChannelName));
        }
        Class<?> signalType = nameToTypeMap.get(signalChannelName);
        if (!signalType.isInstance(signalValue)) {
            throw new IllegalArgumentException(String.format("Signal value is not of type %s", signalType.getName()));
        }

        untypedClient.SignalWorkflow(workflowId, workflowRunId, signalChannelName, signalValue);
    }

    /**
     * @param workflowId required
     * @param workflowRunId optional, default to current runId
     * @param resetType rquired
     * @param historyEventId required for resetType of HISTORY_EVENT_ID. The eventID of any event after DecisionTaskStarted you want to reset to (this event is exclusive in a new run. The new run history will fork and continue from the previous eventID of this). It can be DecisionTaskCompleted, DecisionTaskFailed or others
     * @param reason reason to do the reset for tracking purpose
     * @param resetBadBinaryChecksum required for resetType of BAD_BINARY. Binary checksum for resetType of BadBinary
     * @param decisionOffset based on the reset point calculated by resetType, this offset will move/offset the point by decision. Currently only negative number is supported, and only works with LastDecisionCompleted
     * @param earliestTime required for resetType of DECISION_COMPLETED_TIME. EarliestTime of decision start time, required for resetType of DecisionCompletedTime.Supported formats are '2006-01-02T15:04:05+07:00', raw UnixNano and time range (N<duration>), where 0 < N < 1000000 and duration (full-notation/short-notation) can be second/s, minute/m, hour/h, day/d, week/w, month/M or year/y. For example, '15minute' or '15m' implies last 15 minutes, meaning that workflow will be reset to the first decision that completed in last 15 minutes
     * @param skipSignalReapply
     * @return
     */
    public String ResetWorkflow(
            final String workflowId,
            final String workflowRunId,
            final WorkflowResetRequest.ResetTypeEnum resetType,
            final int historyEventId,
            final String reason,
            final String resetBadBinaryChecksum,
            final int decisionOffset,
            final String earliestTime,
            final boolean skipSignalReapply
            ){

        return untypedClient.ResetWorkflow(workflowId, workflowRunId, resetType, historyEventId, reason, resetBadBinaryChecksum, decisionOffset, earliestTime, skipSignalReapply);
    }

    /**
     * Cancel a workflow, this is essentially terminate the workflow gracefully
     *
     * @param workflowId    required
     * @param workflowRunId optional, can be empty
     * @return
     */
    public void CancelWorkflow(
            final String workflowId,
            final String workflowRunId) {
        untypedClient.CancelWorkflow(workflowId, workflowRunId);
    }

    public Map<String, Object> GetWorkflowQueryAttributes(
            final Class<? extends Workflow> workflowClass,
            final String workflowId,
            final String workflowRunId,
            List<String> attributeKeys) {
        if (attributeKeys == null || attributeKeys.isEmpty()) {
            throw new IllegalArgumentException("attributeKeys must contain at least one entry, or use getAllQueryAttributes API to get all");
        }
        return doGetWorkflowQueryAttributes(workflowClass, workflowId, workflowRunId, attributeKeys);
    }

    public Map<String, Object> GetAllQueryAttributes(
            final Class<? extends Workflow> workflowClass,
            final String workflowId,
            final String workflowRunId) {
        return doGetWorkflowQueryAttributes(workflowClass, workflowId, workflowRunId, null);
    }

    private Map<String, Object> doGetWorkflowQueryAttributes(
            final Class<? extends Workflow> workflowClass,
            final String workflowId,
            final String workflowRunId,
            List<String> attributeKeys) {
        final String wfType = workflowClass.getSimpleName();

        Map<String, Class<?>> queryAttributeKeyToTypeMap = registry.getQueryAttributeKeyToTypeMap(wfType);
        if (queryAttributeKeyToTypeMap == null) {
            throw new IllegalArgumentException(
                    String.format("Workflow %s is not registered", wfType)
            );
        }

        // if attribute keys is null or empty, iwf server will return all query attributes
        if (attributeKeys != null && !attributeKeys.isEmpty()) {
            List<String> nonExistingQueryAttributeList = attributeKeys.stream()
                    .filter(s -> !queryAttributeKeyToTypeMap.containsKey(s))
                    .collect(Collectors.toList());
            if (!nonExistingQueryAttributeList.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Query attributes not registered: %s",
                                String.join(", ", nonExistingQueryAttributeList)
                        )
                );
            }
        }

        WorkflowGetQueryAttributesResponse response = untypedClient.GetAnyWorkflowQueryAttributes(workflowId, workflowRunId, attributeKeys);

        if (response.getQueryAttributes() == null) {
            throw new InternalServiceException("query attributes not returned");
        }
        Map<String, Object> result = new HashMap<>();
        for (KeyValue keyValue : response.getQueryAttributes()) {
            if (keyValue.getValue() != null) {
                result.put(
                        keyValue.getKey(),
                        clientOptions.getObjectEncoder().decode(keyValue.getValue(), queryAttributeKeyToTypeMap.get(keyValue.getKey()))
                );
            }
        }
        return result;
    }

    public WorkflowSearchResponse SearchWorkflow(final String query, final int pageSize) {
        return untypedClient.SearchWorkflow(query, pageSize);
    }

    public Map<String, Object> GetWorkflowSearchAttributes(
            final Class<? extends Workflow> workflowClass,
            final String workflowId,
            final String workflowRunId,
            List<String> attributeKeys) {
        if (attributeKeys == null || attributeKeys.isEmpty()) {
            throw new IllegalArgumentException("attributeKeys must contain at least one entry, or use GetAllSearchAttributes API to get all");
        }
        return doGetWorkflowSearchAttributes(workflowClass, workflowId, workflowRunId, attributeKeys);
    }

    public Map<String, Object> GetAllSearchAttributes(
            final Class<? extends Workflow> workflowClass,
            final String workflowId,
            final String workflowRunId) {
        return doGetWorkflowSearchAttributes(workflowClass, workflowId, workflowRunId, null);
    }

    private Map<String, Object> doGetWorkflowSearchAttributes(
            final Class<? extends Workflow> workflowClass,
            final String workflowId,
            final String workflowRunId,
            final List<String> attributeKeys) {
        final String wfType = workflowClass.getSimpleName();

        final Map<String, SearchAttributeType> searchAttributeKeyToTypeMap = registry.getSearchAttributeKeyToTypeMap(wfType);
        if (searchAttributeKeyToTypeMap == null) {
            throw new IllegalArgumentException(
                    String.format("Workflow %s is not registered", wfType)
            );
        }

        // if attribute keys is null or empty, iwf server will return all query attributes
        if (attributeKeys != null && !attributeKeys.isEmpty()) {
            List<String> nonExistingSearchAttributeList = attributeKeys.stream()
                    .filter(s -> !searchAttributeKeyToTypeMap.containsKey(s))
                    .collect(Collectors.toList());

            if (!nonExistingSearchAttributeList.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Search attributes not registered: %s",
                                String.join(", ", nonExistingSearchAttributeList)
                        )
                );
            }
        }

        List<SearchAttributeKeyAndType> keyAndTypes = new ArrayList<>();
        if (attributeKeys == null) {
            searchAttributeKeyToTypeMap.forEach((key, type) -> {
                final SearchAttributeKeyAndType keyAndType = new SearchAttributeKeyAndType()
                        .key(key)
                        .valueType(toGeneratedSearchAttributeType(type));
                keyAndTypes.add(keyAndType);
            });
        } else {
            attributeKeys.forEach((key) -> {
                final SearchAttributeType saType = searchAttributeKeyToTypeMap.get(key);
                final SearchAttributeKeyAndType keyAndType = new SearchAttributeKeyAndType()
                        .key(key)
                        .valueType(toGeneratedSearchAttributeType(saType));
                keyAndTypes.add(keyAndType);
            });
        }

        WorkflowGetSearchAttributesResponse response = untypedClient.GetAnyWorkflowSearchAttributes(workflowId, workflowRunId, keyAndTypes);

        if (response.getSearchAttributes() == null) {
            throw new InternalServiceException("query attributes not returned");
        }
        Map<String, Object> result = new HashMap<>();
        for (SearchAttribute searchAttribute : response.getSearchAttributes()) {
            final SearchAttributeType saType = searchAttributeKeyToTypeMap.get(searchAttribute.getKey());
            Object value = getSearchAttributeValue(saType, searchAttribute);
            result.put(searchAttribute.getKey(), value);
        }
        return result;
    }

    private Object getSearchAttributeValue(final SearchAttributeType saType, final SearchAttribute searchAttribute) {
        switch (saType) {
            case INT_64:
                return searchAttribute.getIntegerValue();
            case KEYWORD:
                return searchAttribute.getStringValue();
            default:
                throw new InternalServiceException("unsupported type");
        }
    }

    private SearchAttributeKeyAndType.ValueTypeEnum toGeneratedSearchAttributeType(final SearchAttributeType saType) {
        switch (saType) {
            case INT_64:
                return SearchAttributeKeyAndType.ValueTypeEnum.INT;
            case KEYWORD:
                return SearchAttributeKeyAndType.ValueTypeEnum.KEYWORD;
            default:
                throw new InternalServiceException("unsupported type");
        }
    }
}
