package com.zendroid.nmapgui.data.parser

import android.util.Xml
import com.zendroid.nmapgui.data.model.HostEntity
import com.zendroid.nmapgui.data.model.PortEntity
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class NmapXmlParser {

    data class ParseResult(
        val hosts: List<HostEntity>,
        val portsByHost: Map<Long, List<PortEntity>>,
        val rawOutput: String
    )

    fun parse(xmlInput: String, sessionId: Long): ParseResult {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xmlInput))

        val hosts = mutableListOf<HostEntity>()
        val portsByHost = mutableMapOf<Long, MutableList<PortEntity>>()
        var currentHost: HostEntity? = null
        var currentHostId: Long = 0
        val currentPorts = mutableListOf<PortEntity>()
        var scriptOutputs = mutableMapOf<String, String>()
        var cpeList = mutableListOf<String>()
        var inPort = false
        var inScript = false
        var inOsMatch = false
        var currentScriptId = ""
        var currentScriptOutput = ""
        var osName = ""
        var osAccuracy = 0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "host" -> {
                            currentHost = null
                            currentPorts.clear()
                            scriptOutputs = mutableMapOf()
                            cpeList = mutableListOf()
                            osName = ""
                            osAccuracy = 0
                        }
                        "address" -> {
                            val addr = parser.getAttributeValue(null, "addr")
                            val addrType = parser.getAttributeValue(null, "addrtype")
                            if (currentHost == null) {
                                currentHost = HostEntity(
                                    sessionId = sessionId,
                                    ipAddress = "",
                                    status = "up"
                                )
                            }
                            if (addrType == "ipv4") {
                                currentHost = currentHost!!.copy(ipAddress = addr)
                            } else if (addrType == "mac") {
                                currentHost = currentHost!!.copy(macAddress = addr)
                            }
                        }
                        "hostname" -> {
                            val name = parser.getAttributeValue(null, "name")
                            val type = parser.getAttributeValue(null, "type")
                            if (type == "user" || currentHost?.hostname == null) {
                                currentHost = currentHost?.copy(hostname = name)
                            }
                        }
                        "status" -> {
                            val state = parser.getAttributeValue(null, "state")
                            currentHost = currentHost?.copy(status = state ?: "unknown")
                        }
                        "port" -> {
                            inPort = true
                            val protocol = parser.getAttributeValue(null, "protocol") ?: "tcp"
                            val portId = parser.getAttributeValue(null, "portid")?.toIntOrNull() ?: 0
                            currentPorts.add(
                                PortEntity(
                                    hostId = currentHostId,
                                    portNumber = portId,
                                    protocol = protocol,
                                    state = "unknown"
                                )
                            )
                        }
                        "state" -> {
                            if (inPort && currentPorts.isNotEmpty()) {
                                val state = parser.getAttributeValue(null, "state") ?: "unknown"
                                val lastIndex = currentPorts.size - 1
                                currentPorts[lastIndex] = currentPorts[lastIndex].copy(state = state)
                            }
                        }
                        "service" -> {
                            if (inPort && currentPorts.isNotEmpty()) {
                                val name = parser.getAttributeValue(null, "name")
                                val product = parser.getAttributeValue(null, "product")
                                val version = parser.getAttributeValue(null, "version")
                                val lastIndex = currentPorts.size - 1
                                currentPorts[lastIndex] = currentPorts[lastIndex].copy(
                                    serviceName = name,
                                    serviceProduct = product,
                                    serviceVersion = version
                                )
                            }
                        }
                        "script" -> {
                            inScript = true
                            currentScriptId = parser.getAttributeValue(null, "id") ?: ""
                            currentScriptOutput = ""
                        }
                        "elem" -> {
                            if (inScript) {
                                val key = parser.getAttributeValue(null, "key")
                                // Parser will get the text content in TEXT event
                            }
                        }
                        "cpe" -> {
                            parser.nextText()?.let { cpeList.add(it) }
                        }
                        "osmatch" -> {
                            inOsMatch = true
                            osName = parser.getAttributeValue(null, "name") ?: ""
                            osAccuracy = parser.getAttributeValue(null, "accuracy")?.toIntOrNull() ?: 0
                        }
                        "hop" -> {
                            val ttl = parser.getAttributeValue(null, "ttl")?.toIntOrNull()
                            if (ttl != null && currentHost != null) {
                                currentHost = currentHost.copy(hopCount = ttl)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inScript) {
                        currentScriptOutput += parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "host" -> {
                            if (currentHost != null && currentHost.ipAddress.isNotEmpty()) {
                                currentHost = currentHost.copy(
                                    osName = osName.takeIf { it.isNotEmpty() },
                                    osAccuracy = osAccuracy.takeIf { it > 0 }
                                )
                                hosts.add(currentHost)
                            }
                        }
                        "port" -> {
                            inPort = false
                        }
                        "script" -> {
                            inScript = false
                            if (currentScriptId.isNotEmpty() && currentPorts.isNotEmpty()) {
                                val lastIndex = currentPorts.size - 1
                                val existingScripts = currentPorts[lastIndex].scriptOutput ?: emptyMap()
                                scriptOutputs = existingScripts + (currentScriptId to currentScriptOutput.trim())
                                currentPorts[lastIndex] = currentPorts[lastIndex].copy(
                                    scriptOutput = scriptOutputs.toMap()
                                )
                            }
                        }
                        "osmatch" -> {
                            inOsMatch = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        // Assign temporary IDs and build the map
        val hostsWithIds = hosts.mapIndexed { index, host ->
            host.copy(id = (index + 1).toLong())
        }
        
        val portMap = mutableMapOf<Long, List<PortEntity>>()
        hostsWithIds.forEachIndexed { index, host ->
            val ports = currentPorts.map { it.copy(hostId = host.id) }
            portMap[host.id] = ports
        }

        return ParseResult(hostsWithIds, portMap, xmlInput)
    }
}
