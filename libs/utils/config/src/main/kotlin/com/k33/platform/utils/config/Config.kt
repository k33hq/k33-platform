package com.k33.platform.utils.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException

private val logger by lazy { LoggerFactory.getLogger("com.k33.platform.utils.config.Config") }

fun getConfig(
    name: String,
    path: String? = null,
): Lazy<Config> = lazy {
    val config = getConfigEager(name)
    if (!path.isNullOrBlank()) {
        config.getConfig(path)
    } else {
        config
    }
}

inline fun <reified CONFIG : Any> loadConfig(
    name: String,
    path: String? = null,
): Lazy<CONFIG> = lazy {
    loadConfigEager(name, path)
}

inline fun <reified CONFIG : Any> loadConfigEager(
    name: String,
    path: String? = null,
): CONFIG {
    val config = getConfigEager(name)
    return if (!path.isNullOrBlank()) {
        config.extract<CONFIG>(path)
    } else {
        config.extract<CONFIG>()
    }
}

fun getConfigEager(
    name: String,
): Config {
    val configFile = ConfigAsResourceFile("/$name.conf")
    if (configFile.exists()) {
        logger.info("Loading config: $configFile")
        return ConfigFactory.parseString(configFile.readText()).resolve()
    }
    throw FileNotFoundException("Config file not found - $configFile")
}

data class ConfigAsResourceFile(
    private val name: String,
) {
    fun exists(): Boolean = object {}.javaClass.getResource(name) != null
    override fun toString(): String = "resource:$name @ ${object {}.javaClass.getResource(name)?.file}"
    fun readText(): String? = object {}.javaClass.getResource(name)?.readText()
}
