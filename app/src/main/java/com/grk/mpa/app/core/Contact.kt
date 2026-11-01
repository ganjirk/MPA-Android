package com.grk.mpa.app.core

/** Topics offered on the website's Contact page. [wire] is the value the website expects. */
enum class ContactTopic(val wire: String) {
    QUESTION("question"),
    BUG("bug"),
    IDEA("idea"),
    ACCOUNT("account"),
    OTHER("other")
}

data class ContactDraft(
    val name: String = "",
    val email: String = "",
    val topic: ContactTopic = ContactTopic.QUESTION,
    val subject: String = "",
    val message: String = ""
)

enum class ContactError {
    NAME_REQUIRED,
    EMAIL_INVALID,
    SUBJECT_TOO_SHORT,
    MESSAGE_TOO_SHORT
}

/** The same limits as the website's contact form. */
object ContactRules {
    const val NAME_MAX = 100
    const val SUBJECT_MIN = 4
    const val SUBJECT_MAX = 150
    const val MESSAGE_MIN = 10
    const val MESSAGE_MAX = 1000

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** Text is trimmed before it is checked or sent. */
    fun clean(draft: ContactDraft) = draft.copy(
        name = draft.name.trim(),
        email = draft.email.trim(),
        subject = draft.subject.trim(),
        message = draft.message.trim()
    )

    fun validate(draft: ContactDraft): Set<ContactError> {
        val d = clean(draft)
        return buildSet {
            if (d.name.isEmpty()) add(ContactError.NAME_REQUIRED)
            if (!EMAIL.matches(d.email)) add(ContactError.EMAIL_INVALID)
            if (d.subject.length < SUBJECT_MIN) add(ContactError.SUBJECT_TOO_SHORT)
            if (d.message.length < MESSAGE_MIN) add(ContactError.MESSAGE_TOO_SHORT)
        }
    }
}
