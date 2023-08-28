package moe.zodiia.simpledi.providers

import moe.zodiia.simpledi.ComponentMap
import moe.zodiia.simpledi.InjectionScope
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType

internal class GlobalProvider<in I : Any?, out T : Any>(
    private val scope: InjectionScope,
    private val componentMap: ComponentMap,
) : ReadOnlyProperty<I, T> {
    private var instance: T? = null
    private var threadInstances = HashMap<Long, T>()

    private fun requestInstance(type: KType, thisRef: I, pid: Long? = null): T {
        val newInstance = componentMap.requestInstance<T>(type, thisRef, scope, pid)

        if (pid == null) {
            instance = newInstance
        } else {
            threadInstances[pid] = newInstance
        }
        return newInstance
    }

    override fun getValue(thisRef: I, property: KProperty<*>): T {
        val pid = Thread.currentThread().id

        thisRef?.let {
            componentMap.addInstance(it, it::class.createType(), scope, if (scope == InjectionScope.THREAD) pid else null)
        }
        if (scope == InjectionScope.RUNTIME) {
            return instance ?: requestInstance(property.returnType, thisRef)
        }
        return threadInstances[pid] ?: requestInstance(property.returnType, thisRef, pid)
    }
}
