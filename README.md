
# Java Hex Arch (Spring Boot 3, Java 21)

Arquitectura **hexagonal** lista para MVP RAG con:
- **Interfaces (in)**: Web (Webhook WhatsApp de ejemplo)
- **Application (use cases)**: Search, Ingest, Migrate
- **Ports (out)**: EmbeddingsPort, VectorStore
- **Infrastructure (adapters out)**: PgvectorAdapter (JDBC), HttpEmbeddingsClient
- **Domain**: modelos puros

## Ejecutar
./gradlew bootRun

## Migración (perfil `migrate`)
./gradlew bootRun --args='' -Dspring.profiles.active=migrate \
  -Dsource.ns=kb -Ddest.ns=kb2 -Dvector.dim=1536 -Dbatch.size=500

## Ajustes
- `application.yml` → `ai.base-url` apunta al microservicio de IA (Python/otras)
- Agrega adapters para Qdrant/OpenSearch/Pinecone implementando `VectorStore`
