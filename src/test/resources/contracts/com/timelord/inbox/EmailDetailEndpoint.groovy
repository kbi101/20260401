package contracts.com.timelord.inbox

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Should return an email detail from the Bronze layer")

    request {
        method GET()
        url("/api/v1/inbox/emails/golden-q1-id")
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body(
            gmailId: "golden-q1-id",
            sourceEmail: "pmo@timelord.com",
            threadId: "thread-1",
            sender: "pmo@timelord.com",
            receivedAt: "2026-04-06T10:00:00",
            subject: "Q1 Timeline Review",
            bodyContent: "Hi team, we are pushing the release to Friday. Please update the configs.",
            status: "PROCESSED",
            attachments: []
        )
    }
}
