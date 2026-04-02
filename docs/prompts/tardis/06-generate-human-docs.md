# Role
Act as a Product Manager and Technical Writer specializing in AI-Agent ecosystems.

# Task
Based on the TARDIS implementation and specification, generate two user-facing documents:
1.  **Usage Guide**: `docs/usage/tardis-guide.md` - explaining how other agents or users can interact with the TARDIS time services.
2.  **Operations Runbook**: `docs/runbooks/tardis-runbook.md` - documenting how to monitor and troubleshoot the temporal engine and persistent schedules.

# Required Content:
- **Visual Topology**: A Mermaid `graph TD` mapping the TARDIS integration with other modules, highlighting the `tardis_db` schema.
- **Aging & Awareness**: Document the rules for subjective aging feelings (YOUNG, OLD) and Circadian contexts.
- **Observability**: Document the specific TARDIS events that should be monitored for drift or late triggers.
- **Incident Response**: What to do if the scheduling engine stalls or daylight saving transitions fail.

# Execution
Generate the two complete Markdown files for the TARDIS feature.

---
**Target Spec:**
[READ THE SPEC LOCATED AT: docs/specs/tardis-spec.md]
