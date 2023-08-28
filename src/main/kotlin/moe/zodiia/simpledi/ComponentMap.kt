package moe.zodiia.simpledi

import moe.zodiia.simpledi.providers.GlobalProvider
import moe.zodiia.simpledi.providers.InstanceProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor

/**
 * A class responsible for holding all injectable instances of classes in the
 * application runtime.
 *
 * Usually, you only want one component map in the application, and you do not
 * have to manage it. However, you can create multiple component maps and pass
 * them to the [injection] delegate. Use this feature if you know what you're
 * doing.
 */
class ComponentMap {
    private val components = HashSet<InjectableInstance<*>>()

    /**
     * Add an instance to the component map for future injections.
     *
     * Note that it will not affect already injected instances, and will only affect RUNTIME and THREAD scopes.
     */
    fun <T : Any> addInstance(instance: T, type: KType, scope: InjectionScope, pid: Long? = null) {
        if (hasInstanceFor(type, scope, pid)) {
            return
        }
        makeInjectable(type, instance, scope, pid)
    }

    /**
     * Request an instance for a given scope and parameters.
     */
    fun <T : Any> requestInstance(type: KType, thisRef: Any?, scope: InjectionScope, pid: Long? = null): T {
        if (scope.global) {
            components.find {
                try {
                    it.type.isSubtypeOf(type) && pid == it.pid
                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                    false
                }
            }?.let { return it.instance as T }
        }
        return newInstanceOf<T>(type, thisRef, scope, pid).instance
    }

    /**
     * Check if an instance is available given a scope and parameters.
     *
     * For REQUEST and INSTANCE scopes, it will always return `false`.
     */
    fun hasInstanceFor(type: KType, scope: InjectionScope, pid: Long? = null) = when (scope) {
        InjectionScope.REQUEST, InjectionScope.INSTANCE -> false
        InjectionScope.THREAD -> components.any { it.type.isSubtypeOf(type) && it.pid == pid }
        InjectionScope.RUNTIME -> components.any { it.type.isSubtypeOf(type) && it.pid == null }
    }

    private fun <T : Any> newInstanceOf(
        type: KType,
        thisRef: Any?,
        scope: InjectionScope,
        pid: Long? = null,
    ): InjectableInstance<T> {
        val klass = type.classifier as KClass<T>
        val args = HashMap<KParameter, Any>()
        val primaryConstructor = klass.primaryConstructor ?: error("Cannot find a primary constructor on type $type")

        primaryConstructor.parameters.forEach {
            try {
                args[it] = requestInstance(it.type, thisRef, scope, pid)
            } catch (ex: IllegalStateException) {
                if (!it.isOptional) {
                    throw IllegalStateException("Cannot create a valid instance of type $it", ex)
                }
            }
        }
        val instance = try {
            primaryConstructor.callBy(args)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException("Could not create a new instance of type $type", ex)
        }

        return makeInjectable(type, instance, scope, pid)
    }

    private fun <T : Any> makeInjectable(
        type: KType,
        instance: T,
        scope: InjectionScope,
        pid: Long? = null,
    ): InjectableInstance<T> {
        val injectable = InjectableInstance(type, instance, pid)

        if (scope.global) {
            components.add(injectable)
        }
        return injectable
    }

    companion object {
        val default = ComponentMap()
    }
}

/**
 * Inject an instance of the requested type via property delegation.
 *
 * By default, the injected instance will be shared across the whole runtime.
 * However, different scopes can be used to inject different instances
 * according to the context (for example depending on the current thread).
 *
 * When there is no instance available in the component map for this context,
 * a new instance will be created using its primary constructor. If the primary
 * constructor has parameters, then all of them will be injected using the
 * same logic.
 *
 * If the primary constructor does not exist, or if at any point an instance
 * cannot be injected, an exception will be thrown and nothing will be
 * injected.
 *
 * Instance injection must be used as a delegated property. For example:
 * ```kt
 * class Foo {
 *   // ...
 * }
 *
 * class Bar {
 *   val foo by injection<Foo>()
 * }
 * ```
 *
 * See [InjectionScope] and [ComponentMap].
 *
 * @param scope The injection scope. See [InjectionScope]. By default, it will
 * be [InjectionScope.RUNTIME]
 * @param componentMap The component map. See [ComponentMap]. By default, it
 * will be the global one. You shouldn't use multiple component maps, except
 * if you know what you're doing.
 */
fun <T : Any> injection(
    scope: InjectionScope = InjectionScope.RUNTIME,
    componentMap: ComponentMap = ComponentMap.default,
): ReadOnlyProperty<Any?, T> = when (scope) {
    InjectionScope.RUNTIME, InjectionScope.THREAD -> GlobalProvider(scope, componentMap)
    else -> InstanceProvider(scope, componentMap)
}
