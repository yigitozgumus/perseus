package com.yigitozgumus.perseus.api

/**
 * Type-safe navigation group identifier.
 *
 * Replaces Medusa's string-based group names with compile-time safety.
 *
 * Usage:
 * ```kotlin
 * object LoginFlowGroup : GroupName("LoginFlow")
 * object CheckoutGroup : GroupName("Checkout")
 *
 * navigator.navigateTo(StepOneKey, groupName = LoginFlowGroup)
 * navigator.navigateTo(StepTwoKey, groupName = LoginFlowGroup)
 * navigator.popUntil(LoginFlowGroup) // clears both steps
 * ```
 */
open class GroupName(val name: String) {
    override fun equals(other: Any?): Boolean = other is GroupName && name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "GroupName($name)"
}
