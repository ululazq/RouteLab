package io.routelab.app.data.parser

import io.routelab.app.domain.model.GeoPoint
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

class GpxParser {

    fun parse(inputStream: InputStream): List<GeoPoint> {
        return parseInputSource(InputSource(inputStream))
    }

    fun parse(xmlString: String): List<GeoPoint> {
        return parseInputSource(InputSource(StringReader(xmlString)))
    }

    private fun parseInputSource(source: InputSource): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        try {
            val factory = SAXParserFactory.newInstance()
            factory.isNamespaceAware = false
            val saxParser = factory.newSAXParser()

            val handler = object : DefaultHandler() {
                var currentLat: Double? = null
                var currentLon: Double? = null
                var currentEle: Double? = null
                var currentTime: String? = null
                val contentBuffer = StringBuilder()

                override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                    val tag = qName.lowercase()
                    contentBuffer.setLength(0)

                    if (tag.endsWith("trkpt") || tag.endsWith("rtept") || tag.endsWith("wpt")) {
                        currentLat = attributes.getValue("lat")?.toDoubleOrNull()
                        currentLon = attributes.getValue("lon")?.toDoubleOrNull()
                        currentEle = null
                        currentTime = null
                    }
                }

                override fun characters(ch: CharArray, start: Int, length: Int) {
                    contentBuffer.append(ch, start, length)
                }

                override fun endElement(uri: String, localName: String, qName: String) {
                    val tag = qName.lowercase()
                    val text = contentBuffer.toString().trim()

                    when {
                        tag.endsWith("ele") -> currentEle = text.toDoubleOrNull()
                        tag.endsWith("time") -> currentTime = text
                        tag.endsWith("trkpt") || tag.endsWith("rtept") || tag.endsWith("wpt") -> {
                            val lat = currentLat
                            val lon = currentLon
                            if (lat != null && lon != null) {
                                points.add(
                                    GeoPoint(
                                        latitude = lat,
                                        longitude = lon,
                                        elevation = currentEle ?: 0.0,
                                        time = currentTime
                                    )
                                )
                            }
                            currentLat = null
                            currentLon = null
                            currentEle = null
                            currentTime = null
                        }
                    }
                }
            }

            saxParser.parse(source, handler)
        } catch (e: Exception) {
            // Return whatever valid points were accumulated
        }
        return points
    }
}
