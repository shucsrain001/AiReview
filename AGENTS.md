# AGENTS.md

## Project

Spring Boot 3.2.5 + Java 17 Maven project for automated AI code review.

## Build

```bash
mvn clean package -DskipTests   # Build JAR
mvn spring-boot:run            # Run app
```

## Run

Requires Ollama running at `http://localhost:11434` with model `llama3` (configurable in `application.yml`).

## Config

Edit `src/main/resources/application.yml`:
- `code-review.git-repo-paths`: List of local git repo paths to review
- `spring.ai.ollama.base-url`: Ollama endpoint
- `spring.ai.ollama.chat.options.model`: Model name

## Architecture

- `DailyReviewTask` - Scheduled daily at 2 AM
- `GitService` - JGit wrapper for commit extraction
- `AiReviewService` - Spring AI + Ollama for code analysis
- `ContextExtractor` - JavaParser for code context

## Notes

- No test directory exists; add tests to `src/test/java`
- Requires local git repos (not remote URLs)
- Result storage and notifications are TODO stubs