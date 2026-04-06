package contracts.com.timelord.inbox

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Should send a reply to an email thread")

    request {
        method POST()
        url("/api/v1/inbox/emails/golden-q1-id/reply")
        headers {
            contentType applicationJson()
        }
        body(
            gmailId: "golden-q1-id",
            body: "Got it, I will update the configs.",
            replyAll: false
        )
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body(
            originalGmailId: "golden-q1-id",
            sourceEmail: "pmo@timelord.com",
            replyMessageId: "reply-msg-1",
            sentAt: $(anyNonBlankString())
        )
    }
}
