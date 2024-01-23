package dev.zodiia.simpledi.providers

import dev.zodiia.simpledi.ComponentMap
import dev.zodiia.simpledi.InjectionScope
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

internal class InstanceProvider<in I, out T : Any>(
    private val scope: InjectionScope,
    private val componentMap: ComponentMap,
) : ReadOnlyProperty<I, T> {
    private var instance: T? = null

    private fun requestInstance(type: KType, thisRef: I): T {
        val newInstance = componentMap.requestInstance<T>(type, thisRef, scope)
        instance = newInstance
        return newInstance
    }

    override fun getValue(thisRef: I, property: KProperty<*>): T {
        if (scope == InjectionScope.INSTANCE) {
            return instance ?: requestInstance(property.returnType, thisRef)
        }
        return componentMap.requestInstance(property.returnType, thisRef, scope)
    }
}
