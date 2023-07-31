package moe.zodiia.simpledi

/**
 * Defines the scope of the injected instance, where it will be shared with other classes.
 *
 * @property global `true` if this scope is global, `false` if it is restrained to the requesting instance.
 */
enum class InjectionScope(val global: Boolean) {
    /**
     * There will only be one instance only in the whole application runtime.
     *
     * Note that when using multiple component maps, instances are not shared between component maps.
     */
    RUNTIME(true),

    /**
     * There will be one instance of the requested class per thread.
     *
     * Note that when using multiple component maps, instances are not shared between component maps.
     */
    THREAD(true),

    /**
     * There will be one instance of the requested class per instance of the requesting class.
     */
    INSTANCE(false),

    /**
     * There will be a new instance for every request, for example every time the variable is accessed.
     */
    REQUEST(false),
}
