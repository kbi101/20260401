package contracts.com.timelord.inbox

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Should return a paged summary feed")

    request {
        method GET()
        urlPath("/api/v1/inbox/feed") {
            queryParameters {
                parameter("limit", "1")
            }
        }
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body(
            summaries: [
                [
                    summaryId: $(anyNonBlankString()),
                    originalGmailId: "golden-q1-id",
                    sourceEmail: "pmo@timelord.com",
                    sender: "pmo@timelord.com",
                    subject: "Q1 Timeline Review",
                    receivedAt: "2026-04-06T10:00:00",
                    summaryText: "The Q1 release timeline has been shifted to Friday by the team. No exact deadline for config updates is specified.",
                    keyActionItems: [
                        "Update configuration files for the Friday release."
                    ],
                    sentiment: "NEUTRAL",
                    processedAt: "2026-04-06T10:05:00"
                ]
            ],
            nextCursor: "2026-04-06T10:00:00",
            hasMore: false
        )
    }
}
