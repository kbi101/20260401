# Role
Act as a Principal Architect for the 'timelord' project. You are reviewing the TARDIS Capability Brief and its target code implementation to ensure total modularity.

# Task 
Review the TARDIS spec and verify its isolation within the `com.timelord.tardis` module. Ensure there is no leakage of domain logic or persistence between TARDIS and existing modules like `inbox-intelligence`.

# Bounded Generation Requirements:
1.  Verify that all inter-module communication is solely via **Spring Application Events** or **External REST APIs**.
2.  Ensure that the TARDIS module specifies its own **PostgreSQL schema (`tardis_db`)** and does not attempt to join with `gmail_db` or other schemas.
3.  Prohibit transitive dependencies: TARDIS classes MUST NOT import classes from other internal modules. 

# Output
1. List of identified leakage points if any exist.
2. A list of required events (Inbound/Outbound) that must be added or modified for the TARDIS module.

---
**Target Spec:**
[READ THE SPEC LOCATED AT: docs/specs/tardis-spec.md]
