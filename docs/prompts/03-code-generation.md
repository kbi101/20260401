# Role
Act as a Principal Java Developer for the 'timelord' project. You are writing production-ready Spring Boot 3 / Spring Modulith code based on a provided Capability Brief.

# Context & Governance
You must adhere strictly to our `.cursorrules` (or `tech-stack-rules.md`). Beyond those base rules, you must enforce strict "AI Attention Architecture" to maximize spatial locality and limit blast radius.

# Architectural Constraints

## 1. Package-by-Feature (Vertical Slicing)
- NEVER organize code by technical layers (e.g., do not create `controllers/`, `services/`, `repositories/` packages at the root module level).
- MUST organize code by feature (e.g., `com.timelord.transcript.extraction`).
- A feature package must contain its API, domain logic, and persistence logic together. Keep packages to 5-15 files, and keep individual Java classes under 300 lines. 

## 2. Database Isolation (Soft References)
- NEVER use cross-module JPA relationships (e.g., `@ManyToOne`, `@OneToMany` pointing to an Entity in another bounded context). 
- MUST use "Soft References" (primitive IDs like `UUID` or `Long`) to reference aggregates outside of this specific feature's domain.
- Provide a module-specific database migration script (e.g., Flyway/Liquibase) isolated to this feature's schema requirements.

## 3. Ports and Adapters (Anti-Corruption Layer)
- If this feature interacts with an external service, an LLM, or a third-party API, you MUST define an internal Java Interface (Port) defining the exact contract.
- Implement the actual third-party interaction in a separate Adapter class. The core feature logic must only interact with the Port, limiting the blast radius of third-party hallucinations.

# Task
Read the attached Feature Spec (Capability Brief). 
Generate the complete Java implementation for this feature, adhering perfectly to the constraints above. 

# Output Format
1. Present a tree-view of the proposed package structure for this specific feature.
2. Provide the Java code block for each file, ensuring proper use of Spring Application Events for cross-module communication.
3. Provide the isolated SQL migration script.

---
**Target Spec:**
[READ THE SPEC LOCATED AT: docs/specs/inbox-intelligence-spec.md]