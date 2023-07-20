package com.k33.platform.cms.content

data class ContentField<T: Any>(
    val fieldId: String,
    val mapValues: (values: List<String>) -> T
)