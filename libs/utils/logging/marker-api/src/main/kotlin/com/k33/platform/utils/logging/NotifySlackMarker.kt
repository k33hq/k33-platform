package com.k33.platform.utils.logging

import org.slf4j.Marker
import org.slf4j.MarkerFactory

enum class NotifySlack(name: String = this.toString()) : Marker by MarkerFactory.getMarker(name) {
    ALERTS,
    GENERAL,
    INVEST,
    PRODUCT,
    RESEARCH,
    RESEARCH_EVENTS,
}