package com.paypal.aurora

import com.paypal.aurora.joke.FailureImage

class CustomTagLib {

    def jokeService

    def exclaim = { out << jokeService.randomExclamation() }

    def excuse = { out << jokeService.randomExcuse() }

    def failureImage = {
        FailureImage failureImage = jokeService.randomFailureImage()
        String caption = failureImage.url ? """<figcaption>Creative Commons image by <a \
href="${failureImage.url}">${failureImage.owner}</a></figcaption>""" : ''
        out << """<figure class="failure"><img src="${failureImage.path}"/>${caption}</figure>"""
    }

    /**
     * This buttonSubmit tag is similar to actionSubmit. However, buttonSubmit create a <button> element, while
     * actionSubmit creates an <input> element. The button element allows for more rendering options in some browsers,
     * as well as allowing nested elements inside the button, such as images and paragraphs.
     *
     * Examples:
     *
     * <g:actionSubmit action="Edit" value="Some label for editing" />
     * HTML output:
     * <input type="submit" name="_action_edit" value="Some label for editing"/>
     *
     * Creates a submit button that submits to an action in the controller specified by the form action.
     * The name of the action attribute is translated into the action name, for example "Edit" becomes
     * "_action_edit" or "List People" becomes "_action_listPeople"
     * If the action attribute is not specified, the value attribute will be used as part of the action name.
     * If neither an action nor a value is specified, the action name will be "_action_".
     * This tag requires either a value or a body (inner html).
     *
     * <g:buttonSubmit action="Edit" value="Some label for editing" />
     * <g:buttonSubmit action="Edit">Some label for editing</g:buttonSubmit>
     * HTML output:
     * <button type="submit" name="_action_edit">Some label for editing</button>
     *
     * <g:buttonSubmit value="Edit" />
     * HTML output:
     * <button type="submit" name="_action_edit">Edit</button>
     */
    def buttonSubmit = { attrs, body ->
        attrs.tagName = "buttonSubmit"
        def innerBody = body()
        if (!attrs.value && !innerBody) {
            throwTagError("Tag [$attrs.tagName] requires either a [value] attribute or a body")
        }

        // add action
        def value = attrs.value ? attrs.remove('value') : null
        def action = attrs.action ? attrs.remove('action') : value

        def disabled = attrs.disabled ? attrs.remove('disabled') : null

        // Use the body or the value inside the button
        def inner = innerBody ?: value

        out << "<button type=\"submit\" name=\"_action_${action}\" "

        // Only add HTML disabled attribute if the value is true or disabled, not empty, missing, or false.
        if (disabled in ['true', 'disabled']) { out << 'disabled="disabled" ' }

        // process remaining attributes
        outputAttributes(attrs)

        // close tag
        out << '><div>'
        out << inner
        out << '</div></button>'
    }

    /**
     * Dump out attributes in HTML compliant fashion.
     * This utility is copied from org.codehaus.groovy.grails.plugins.web.taglib.FormTagLib in Grails 1.3.5
     * Extending that class caused runtime errors because of its declaration of the implicit 'out' variable
     */
    void outputAttributes(attrs) {
        attrs.remove('tagName') // Just in case one is left
        def writer = getOut()
        attrs.each {k, v ->
            writer << "$k=\"${v.encodeAsHTML()}\" "
        }
    }

    /**
     * Shows a styled version of an availability zone name, either in a specified tag or in a span tag by default.
     */
    def availabilityZone = { attrs, body ->
        String innerBody = body()
        String value = attrs.value ? attrs.remove('value') : null
        String tag = attrs.tag ?: 'span'

        // Use the body or the value inside the tag
        String inner = innerBody ?: value
        if (inner) {
            String styleClass = Styler.availabilityZoneToStyleClass(inner)
            out << "<${tag} class=\"${styleClass}\">${inner}</${tag}>"
        }
    }
}
