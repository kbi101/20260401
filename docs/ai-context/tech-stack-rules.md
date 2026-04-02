# Tech Stack Rules

To prevent library hallucinations and maintain a modern, consistent codebase, the following third-party guardrails are enforced.

## Library Guardrails
- **JSON Processing**: MUST use `Jackson` (standard Spring Boot default). NEVER use `Gson` or other custom serializers unless explicitly required by a specific integration.
- **HTTP Clients**: MUST use Spring `RestClient` (introduced in Spring Framework 6.1) for modern, developer-friendly HTTP interaction. NEVER use the deprecated `RestTemplate`.
- **Testing**: MUST use `JUnit 5` and `AssertJ`. NEVER use JUnit 4.
- **Nullability**: Enforce standard Spring `@NonNull` and `@Nullable` annotations to ensure type safety and reduce NullPointerExceptions.
- **Spring AI**: Exclusively use Spring AI starters for any LLM-related features.

## Coding Standards
- Use **Lombok** sparingly (prefer **Java Records** for DTOs and value objects).
- All new features MUST be accompanied by integration tests using `spring-modulith-test`.
