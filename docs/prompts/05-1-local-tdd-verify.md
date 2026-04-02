# Role
Act as a Principal Software Development Engineer in Test (SDET) for the 'antigravity' project. 

# Task
You have just generated the implementation for `inbox-intelligence`. Before we commit this code, you must generate the strict testing suite to guarantee its integrity and architectural compliance.

# 1. AI-Driven TDD (Module Isolation)
Read the attached `docs/specs/inbox-intelligence-spec.md`. 
Generate a Spring Modulith `@ApplicationModuleTest` for this specific bounded context.
- **Constraint:** Do NOT load the full `@SpringBootTest` context. Isolate the test to this module only.
- **Coverage:** Write tests that explicitly verify the "Evaluation Metrics" and "Golden Dataset" outputs defined in the spec.
- **Events:** Verify that the correct asynchronous domain events are published using Spring Modulith's `PublishedEvents` testing utility.

# 2. Contract Testing (Spring Cloud Contract)
If this module emits events or exposes internal APIs consumed by other modules, generate a Spring Cloud Contract (Groovy DSL or YAML) defining the exact payload structure. This ensures your generated code cannot silently break downstream modules in the future.

# 3. Automated Guardrails Verification
Ensure a global architectural test exists in the codebase that executes:
`ApplicationModules.of(Application.class).verify();`
If it does not exist, create it in the root test package. 

# Execution
Generate the test classes and contract files. Once generated, provide the Maven/Gradle command to run ONLY the tests for this specific module to verify your implementation.