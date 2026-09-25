package io.routelab.app.data.parser

import io.routelab.app.domain.model.GeoPoint
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader

class GpxParser {

    fun parse(inputStream: InputStream): List<GeoPoint> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        return parseInternal(parser)
    }

    fun parse(xmlString: String): List<GeoPoint> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlString))
        return parseInternal(parser)
    }

    private fun parseInternal(parser: XmlPullParser): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var eventType = parser.eventType

        var currentLat: Double? = null
        var currentLon: Double? = null
        var currentEle: Double? = null
        var currentTime: String? = null
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("trkpt", ignoreCase = true) ||
                        currentTag.equals("rtept", ignoreCase = true) ||
                        currentTag.equals("wpt", ignoreCase = true)
                    ) {
                        currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        currentEle = null
                        currentTime = null
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when {
                            currentTag.equals("ele", ignoreCase = true) -> {
                                currentEle = text.toDoubleOrNull()
                            }
                            currentTag.equals("time", ignoreCase = true) -> {
                                currentTime = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val endTag = parser.name
                    if (endTag.equals("trkpt", ignoreCase = true) ||
                        endTag.equals("rtept", ignoreCase = true) ||
                        endTag.equals("wpt", ignoreCase = true)
                    ) {
                        if (currentLat != null && currentLon != null) {
                            points.add(
                                GeoPoint(
                                    latitude = currentLat,
                                    longitude = currentLon,
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
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return points
    }
}
