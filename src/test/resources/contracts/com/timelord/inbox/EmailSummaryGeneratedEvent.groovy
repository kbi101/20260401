import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Verify the payload of EmailSummaryGeneratedEvent"
    label "email_summarized"
    input {
        triggeredBy('EmailSyncedEvent()')
    }
    outputMessage {
        sentTo "email-summaries"
        body([
            summaryId: anyUuid(),
            originalGmailId: anyNonEmptyString(),
            sourceEmail: "user@example.com",
            summaryText: "The Q1 release timeline has been shifted to Friday by the team. No exact deadline for config updates is specified.",
            keyActionItems: ["Update configuration files for the Friday release."],
            sentiment: "NEUTRAL",
            processedAt: $(regex('([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\.\\d+)?(Z|[+-][01]\\d:[0-5]\\d)?'))
        ])
    }
}
