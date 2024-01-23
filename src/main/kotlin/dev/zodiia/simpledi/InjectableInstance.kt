package dev.zodiia.simpledi

import kotlin.reflect.KType

internal data class InjectableInstance<out T : Any>(
    val type: KType,
    val instance: T,
    val pid: Long? = null,
)
