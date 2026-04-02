import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Verify the payload of EmailSummaryGeneratedEvent"
    label "email_summarized"
    input {
        triggeredBy('EmailSyncedEvent')
    }
    outputMessage {
        sentTo "email-summaries"
        body([
            summaryId: anyUuid(),
            originalGmailId: anyNonEmptyString(),
            summaryText: "The email discusses a deployment push to Monday for the project.",
            keyActionItems: ["Confirm configurations for Monday deployment"],
            sentiment: "NEUTRAL",
            processedAt: anyIso8601WithOffset()
        ])
    }
}
