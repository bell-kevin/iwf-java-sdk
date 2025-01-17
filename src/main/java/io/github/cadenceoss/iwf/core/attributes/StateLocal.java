package io.github.cadenceoss.iwf.core.attributes;

public interface StateLocal {
    /**
     * set a local attribute. The scope of the attribute is only within the execution of this state.
     * Usually it's for passing from State Start API to State Decide API
     * User code must make sure using the same type for both get & set
     *
     * @param key
     * @param value
     */
    void setLocalAttribute(String key, Object value);

    /**
     * Retrieve a local state attribute
     * User code must make sure using the same type for both get & set
     *
     * @param key
     * @param type
     * @param <T>
     * @return
     */
    <T> T getLocalAttribute(String key, Class<T> type);

    /**
     * Record an arbitrary event in State Start/Decide API for debugging/tracking purpose
     *
     * @param key       the key of the event. Within a Start/Decide API, the same key cannot be used for more than once.
     * @param eventData the data of the event.
     */
    void recordEvent(String key, Object eventData);
}
