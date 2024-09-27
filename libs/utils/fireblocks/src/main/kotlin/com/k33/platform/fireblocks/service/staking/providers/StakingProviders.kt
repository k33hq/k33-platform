package com.k33.platform.fireblocks.service.staking.providers

import com.k33.platform.fireblocks.client.FireblocksClient
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

//
// staking/providers
//
@Serializable
data class StakingProvider(
    val id: String,
    val providerName: String,
    val validators: List<StakingProviderValidator>,
    val iconUrl: String,
    val termsOfServiceUrl: String,
    val isTermsOfServiceApproved: Boolean,
)

@Serializable
data class StakingProviderValidator(
    val chainDescriptor: String,
    val feePercent: Double,
)

/**
 * List staking providers details.
 * [staking/providers](https://developers.fireblocks.com/reference/getproviders)
 */
suspend fun FireblocksStakingService.getStakingProviders(): List<StakingProvider> = FireblocksClient.get(
    path = "staking/providers"
) ?: emptyList()

//
// staking/providers/$providerId/approveTermsOfService
//

/**
 * Approve staking terms of service.
 * [staking/providers/{providerId}/approveTermsOfService](https://developers.fireblocks.com/reference/approvetermsofservicebyproviderid)
 */
@OptIn(ExperimentalEncodingApi::class)
suspend fun FireblocksStakingService.approveTermsOfServiceByProvider(providerId: String): String? =
    FireblocksClient.post(
        path = "staking/providers/$providerId/approveTermsOfService",
        "Idempotency-Key" to Base64.UrlSafe.encode(providerId.toByteArray())
    )