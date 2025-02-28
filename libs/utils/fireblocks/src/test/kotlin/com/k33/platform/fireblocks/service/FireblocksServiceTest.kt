package com.k33.platform.fireblocks.service

import io.kotest.core.spec.style.StringSpec

class FireblocksServiceTest : StringSpec({
    "get supported assets" {
        val supportedAssets = FireblocksService.fetchAllSupportedAssets()
        println("""id,name,nativeAsset""")
        supportedAssets.forEach {
            supportedAsset -> println("""${supportedAsset.id},"${supportedAsset.name}","${supportedAsset.nativeAsset}"""")
        }
    }
})