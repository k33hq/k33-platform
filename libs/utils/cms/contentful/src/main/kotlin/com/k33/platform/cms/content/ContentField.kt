package com.k33.platform.cms.content

data class ContentField(
    val fieldId: String,
    val mapValues: (values: List<String>) -> Any = {}
)