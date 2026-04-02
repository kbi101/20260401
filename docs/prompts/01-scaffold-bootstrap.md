# Role
Act as an expert Java Software Architect specializing in Spring Boot 3, Spring Modulith, and AI-native application design.

# Task
Your task is to scaffold a new project called `timelord and establish strict AI governance using the "Pointer Pattern". Do not write any business logic yet; focus entirely on the project structure and AI instruction files.

Execute the following steps in exact order:

### Step 1: Scaffold the Base Application
Initialize the project structure for a Spring Boot application named `timelord`.
- **Language:** Java 21 (or latest LTS)
- **Build Tool:** Maven (or Gradle, based on standard Spring Initializr output)
- **Core Dependencies:** Spring Web, Spring Modulith, Spring AI (standard BOM)

### Step 2: Establish the AI Governance Directory
Create a new directory at the root of the project called `docs/ai-context/`. 
Inside this directory, generate two files with the following exact constraints:

**1. `docs/ai-context/architecture-constraints.md`**
Write a concise markdown file mandating that the project strictly follows Spring Modulith principles. Explicitly state:
- The application is divided into domain-driven modules.
- Modules must interact via Spring Application Events or well-defined interfaces.
- Cyclic dependencies between modules are strictly forbidden.

**2. `docs/ai-context/tech-stack-rules.md`**
Write a concise markdown file defining strict third-party guardrails to prevent library hallucinations. Explicitly state:
- JSON Processing: MUST use `Jackson`. NEVER use `Gson`.
- HTTP Clients: MUST use Spring `RestClient`. NEVER use the deprecated `RestTemplate`.
- Testing: MUST use `JUnit 5` and `AssertJ`. NEVER use JUnit 4.
- Nullability: Enforce standard Spring `@NonNull` and `@Nullable` annotations.

### Step 3: Implement the Pointer Pattern
Create a `.cursorrules` file at the root of the project. (If using a different platform, adapt the filename to `.geminirules` or equivalent).
Inside this file, write a strict directive that forces the AI to read the context files. Use this exact text:

`You are an expert AI developer working on the 'timelord' project. Before generating, modifying, or refactoring ANY code, you MUST read, understand, and adhere to the rules defined in:`
`- docs/ai-context/architecture-constraints.md`
`- docs/ai-context/tech-stack-rules.md`
`If a user requests code that violates these rules, you must refuse and correct them.`

### Output Requirement
Execute the file creations and respond with a printed tree view of the newly generated project structure to confirm the scaffolding is complete.