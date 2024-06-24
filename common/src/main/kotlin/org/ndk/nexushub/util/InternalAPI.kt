package org.ndk.nexushub.util;

import kotlin.annotation.AnnotationTarget;
import kotlin.annotation.Target;

/**
 * The API marked with this annotation is internal, and it is not intended to be used outside NexusHub.
 * It could be modified or removed without any notice. Using it outside Ktor could cause undefined behaviour and/or
 * any unexpected effects.
 */
@Suppress("DEPRECATION")
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is internal in NexusHub and should not be used. It could be removed or changed without notice."
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class InternalAPI