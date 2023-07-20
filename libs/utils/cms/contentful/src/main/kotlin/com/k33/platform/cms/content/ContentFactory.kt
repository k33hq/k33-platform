package com.k33.platform.cms.content

import com.k33.platform.cms.config.Sync
import com.k33.platform.cms.space.research.article.ResearchArticle

object ContentFactory {
    fun getContent(sync: Sync): Content {
        sync.config.contentfulSpace.config.spaceId
        return when (sync) {
            Sync.researchArticles -> ResearchArticle(
                spaceId = sync.config.contentfulSpace.config.spaceId,
                token = sync.config.contentfulSpace.config.token,
            )
        }
    }
}