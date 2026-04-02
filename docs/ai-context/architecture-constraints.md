# Architecture Constraints

This project strictly follows Spring Modulith principles to maintain a clean, domain-driven modular monolithic architecture.

## Core Mandates
- **Domain-Driven Modules**: The application is divided into logical modules based on business domains.
- **Spring Application Events**: Modules MUST prioritize interaction via Spring Application Events (asynchronous or synchronous) to ensure decoupling.
- **Well-Defined Interfaces**: Direct module-to-module interaction is only permitted through public interfaces within each module's base package.
- **No Cyclic Dependencies**: Cyclic dependencies between modules are strictly forbidden. Modulith verification tests will enforce this.
- **Hidden Internals**: Internal implementation details of a module (subpackages other than the module's root) MUST remain inaccessible to other modules.
