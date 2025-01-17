package io.github.cadenceoss.iwf.core;

import com.google.common.base.Preconditions;
import io.github.cadenceoss.iwf.gen.api.ApiClient;
import io.github.cadenceoss.iwf.gen.api.DefaultApi;
import io.github.cadenceoss.iwf.gen.models.SearchAttributeKeyAndType;
import io.github.cadenceoss.iwf.gen.models.StateCompletionOutput;
import io.github.cadenceoss.iwf.gen.models.WorkflowCancelRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowGetQueryAttributesRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowGetQueryAttributesResponse;
import io.github.cadenceoss.iwf.gen.models.WorkflowGetRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowGetResponse;
import io.github.cadenceoss.iwf.gen.models.WorkflowGetSearchAttributesRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowGetSearchAttributesResponse;
import io.github.cadenceoss.iwf.gen.models.WorkflowResetRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowResetResponse;
import io.github.cadenceoss.iwf.gen.models.WorkflowSearchRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowSearchResponse;
import io.github.cadenceoss.iwf.gen.models.WorkflowSignalRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowStartRequest;
import io.github.cadenceoss.iwf.gen.models.WorkflowStartResponse;

import java.util.List;

/**
 * UntypedClient will let you invoke the APIs to iWF server without much type checks.
 * It's useful for some use cases like dynamic workflow instance that workflowType is not from class simple name.
 */
public class UntypedClient {
    private final DefaultApi defaultApi;

    private final ClientOptions clientOptions;

    public UntypedClient(final ClientOptions clientOptions) {
        this.clientOptions = clientOptions;
        this.defaultApi = new ApiClient()
                .setBasePath(clientOptions.getServerUrl())
                .buildClient(DefaultApi.class);
    }

    public String StartWorkflow(
            final String workflowType,
            final String startStateId,
            final Object input,
            final String workflowId,
            final WorkflowStartOptions options) {
        WorkflowStartResponse workflowStartResponse = defaultApi.apiV1WorkflowStartPost(new WorkflowStartRequest()
                .workflowId(workflowId)
                .iwfWorkerUrl(clientOptions.getWorkerUrl())
                .iwfWorkflowType(workflowType)
                .workflowTimeoutSeconds(options.getWorkflowTimeoutSeconds())
                .stateInput(clientOptions.getObjectEncoder().encode(input))
                .startStateId(startStateId));
        return workflowStartResponse.getWorkflowRunId();
    }

    /**
     * For most cases, a workflow only has one result(one completion state)
     * Use this API to retrieve the output of the state
     *
     * @param valueClass    the type class of the output
     * @param workflowId    the workflowId
     * @param workflowRunId optional runId, can be empty string
     * @param <T>           type of the output
     * @return
     */
    public <T> T GetSimpleWorkflowResultWithWait(
            Class<T> valueClass,
            final String workflowId,
            final String workflowRunId) {
        WorkflowGetResponse workflowGetResponse = defaultApi.apiV1WorkflowGetWithWaitPost(
                new WorkflowGetRequest()
                        .needsResults(true)
                        .workflowId(workflowId)
                        .workflowRunId(workflowRunId)
        );

        if (workflowGetResponse.getResults() == null || workflowGetResponse.getResults().size() == 0) {
            return null;
        }

        String checkErrorMessage = "this workflow should have one or zero state output for using this API";
        Preconditions.checkNotNull(workflowGetResponse.getResults(), checkErrorMessage);
        Preconditions.checkArgument(workflowGetResponse.getResults().size() == 1, checkErrorMessage);
        Preconditions.checkNotNull(workflowGetResponse.getResults().get(0).getCompletedStateOutput(), checkErrorMessage);

        final StateCompletionOutput output = workflowGetResponse.getResults().get(0);
        return clientOptions.getObjectEncoder().decode(output.getCompletedStateOutput(), valueClass);
    }

    public <T> T GetSimpleWorkflowResultWithWait(
            Class<T> valueClass,
            final String workflowId) {
        return GetSimpleWorkflowResultWithWait(valueClass, workflowId, "");
    }

    /**
     * In some cases, a workflow may have more than one completion states
     *
     * @param workflowId
     * @param workflowRunId optional runId, can be empty string
     * @return a list of the state output for completion states. User code will figure how to use ObjectEncoder to decode the output
     */
    public List<StateCompletionOutput> GetComplexWorkflowResultWithWait(
            final String workflowId, final String workflowRunId) {
        WorkflowGetResponse workflowGetResponse = defaultApi.apiV1WorkflowGetWithWaitPost(
                new WorkflowGetRequest()
                        .needsResults(true)
                        .workflowId(workflowId)
                        .workflowRunId(workflowRunId)
        );

        return workflowGetResponse.getResults();
    }

    public void SignalWorkflow(
            final String workflowId,
            final String workflowRunId,
            final String signalChannelName,
            final Object signalValue) {
        defaultApi.apiV1WorkflowSignalPost(new WorkflowSignalRequest()
                .workflowId(workflowId)
                .workflowRunId(workflowRunId)
                .signalChannelName(signalChannelName)
                .signalValue(clientOptions.getObjectEncoder().encode(signalValue)));
    }

    /**
     * @param workflowId             required
     * @param workflowRunId          optional, default to current runId
     * @param resetType              rquired
     * @param historyEventId         required for resetType of HISTORY_EVENT_ID. The eventID of any event after DecisionTaskStarted you want to reset to (this event is exclusive in a new run. The new run history will fork and continue from the previous eventID of this). It can be DecisionTaskCompleted, DecisionTaskFailed or others
     * @param reason                 reason to do the reset for tracking purpose
     * @param resetBadBinaryChecksum required for resetType of BAD_BINARY. Binary checksum for resetType of BadBinary
     * @param decisionOffset         based on the reset point calculated by resetType, this offset will move/offset the point by decision. Currently only negative number is supported, and only works with LastDecisionCompleted
     * @param earliestTime           required for resetType of DECISION_COMPLETED_TIME. EarliestTime of decision start time, required for resetType of DecisionCompletedTime.Supported formats are '2006-01-02T15:04:05+07:00', raw UnixNano and time range (N<duration>), where 0 < N < 1000000 and duration (full-notation/short-notation) can be second/s, minute/m, hour/h, day/d, week/w, month/M or year/y. For example, '15minute' or '15m' implies last 15 minutes, meaning that workflow will be reset to the first decision that completed in last 15 minutes
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
    ) {

        final WorkflowResetResponse resp = defaultApi.apiV1WorkflowResetPost(new WorkflowResetRequest()
                .workflowId(workflowId)
                .workflowRunId(workflowRunId)
                .resetType(resetType)
                .historyEventId(historyEventId)
                .reason(reason)
                .decisionOffset(decisionOffset)
                .resetBadBinaryChecksum(resetBadBinaryChecksum)
                .earliestTime(earliestTime)
                .skipSignalReapply(skipSignalReapply)
        );
        return resp.getWorkflowRunId();
    }

    /**
     * Cancel a workflow, this is essentially terminate the workflow gracefully
     *
     * @param workflowId    required
     * @param workflowRunId optional
     * @return
     */
    public void CancelWorkflow(
            final String workflowId,
            final String workflowRunId) {
        defaultApi.apiV1WorkflowCancelPost(new WorkflowCancelRequest()
                .workflowId(workflowId)
                .workflowRunId(workflowRunId));
    }

    /**
     * @param workflowId
     * @param workflowRunId
     * @param attributeKeys, return all attributes if this is empty or null
     * @return
     */
    public WorkflowGetQueryAttributesResponse GetAnyWorkflowQueryAttributes(
            final String workflowId,
            final String workflowRunId,
            List<String> attributeKeys) {

        return defaultApi.apiV1WorkflowQueryattributesGetPost(
                new WorkflowGetQueryAttributesRequest()
                        .workflowId(workflowId)
                        .workflowRunId(workflowRunId)
                        .attributeKeys(attributeKeys)
        );
    }

    public WorkflowSearchResponse SearchWorkflow(final String query, final int pageSize) {
        return defaultApi.apiV1WorkflowSearchPost(
                new WorkflowSearchRequest()
                        .query(query)
                        .pageSize(pageSize)
        );
    }

    public WorkflowGetSearchAttributesResponse GetAnyWorkflowSearchAttributes(
            final String workflowId,
            final String workflowRunId,
            List<SearchAttributeKeyAndType> attributeKeys) {
        return defaultApi.apiV1WorkflowSearchattributesGetPost(
                new WorkflowGetSearchAttributesRequest()
                        .workflowId(workflowId)
                        .workflowRunId(workflowRunId)
                        .attributeKeys(attributeKeys)
        );
    }
}
