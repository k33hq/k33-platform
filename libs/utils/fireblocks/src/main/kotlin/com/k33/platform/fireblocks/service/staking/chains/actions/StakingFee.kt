package com.k33.platform.fireblocks.service.staking.chains.actions

sealed interface StakingFee {
    operator fun component1(): Double? {
        return when(this) {
            is FeeLevel -> null
            is FeePercent -> value
        }
    }
    operator fun component2(): String? {
        return when(this) {
            is FeeLevel -> name
            is FeePercent -> null
        }
    }
}

data class FeePercent(val value: Double) : StakingFee

enum class FeeLevel : StakingFee {
    SLOW,
    MEDIUM,
    FAST
}
