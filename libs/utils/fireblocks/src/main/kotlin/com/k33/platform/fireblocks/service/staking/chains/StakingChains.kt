package com.k33.platform.fireblocks.service.staking.chains

import com.k33.platform.fireblocks.client.FireblocksClient
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService

//
// staking/chains
//

/**
 * List supported chains for Fireblocks Staking.
 * [`staking/chains`](https://developers.fireblocks.com/reference/getchains)
 */
suspend fun FireblocksStakingService.listSupportedChains(): List<String> = FireblocksClient.get(
    path = "staking/chains"
) ?: emptyList()