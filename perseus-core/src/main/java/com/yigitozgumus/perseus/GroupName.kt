package com.yigitozgumus.perseus

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
public open class GroupName(public val name: String) {
    public override fun equals(other: Any?): Boolean = other is GroupName && name == other.name
    public override fun hashCode(): Int = name.hashCode()
    public override fun toString(): String = "GroupName($name)"
}
