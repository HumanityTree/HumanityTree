package at.crowdware.humanitytree.sml

import at.crowdware.sml.PropertyValue
import at.crowdware.sml.SmlHandler
import at.crowdware.sml.SmlSaxParser

/** Minimal DOM-like node used for decoding SML via the SAX parser. */
data class SmlElement(
    val name: String,
    val properties: MutableMap<String, PropertyValue> = mutableMapOf(),
    val children: MutableList<SmlElement> = mutableListOf()
)

private class DomHandler : SmlHandler {
    val roots = mutableListOf<SmlElement>()
    private val stack = ArrayDeque<SmlElement>()

    override fun startElement(name: String) {
        stack.addLast(SmlElement(name))
    }

    override fun onProperty(name: String, value: PropertyValue) {
        stack.last().properties[name] = value
    }

    override fun endElement(name: String) {
        val finished = stack.removeLast()
        if (stack.isEmpty()) {
            roots += finished
        } else {
            stack.last().children += finished
        }
    }
}

object SmlDomParser {
    fun parse(text: String): List<SmlElement> {
        val handler = DomHandler()
        SmlSaxParser(text).parse(handler)
        return handler.roots
    }
}

fun SmlElement.string(name: String): String? = (properties[name] as? PropertyValue.StringValue)?.value
fun SmlElement.int(name: String): Int? = (properties[name] as? PropertyValue.IntValue)?.value
fun SmlElement.boolean(name: String): Boolean? = (properties[name] as? PropertyValue.BooleanValue)?.value

fun PropertyValue.render(): String = when (this) {
    is PropertyValue.IntValue -> value.toString()
    is PropertyValue.FloatValue -> value.toString()
    is PropertyValue.BooleanValue -> value.toString()
    is PropertyValue.StringValue -> "\"${value}\""
}
