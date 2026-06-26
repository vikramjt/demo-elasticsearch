# Spring Boot + Elasticsearch + ELK Demo

This repository provides:

1. A multi-module Maven project with Elasticsearch document CRUD, pagination search, and aggregations (including bar charts by category).
2. Log ingestion to Elasticsearch through Logstash using structured JSON logs (industry pattern).
3. A second Spring Boot application that emits logs and calls the first app so trace-correlated interservice logs are searchable.
4. Docker Compose to run Elasticsearch + Logstash + Kibana (ELK stack).
5. A bulk-loading script for 10k+ documents.

## Project Structure

```text
.
├── docker/
│   ├── docker-compose.yml
│   └── logstash/
│       ├── config/logstash.yml
│       └── pipeline/logstash.conf
├── elasticsearch-demo-app/           # main Spring Boot app (submodule)
│   ├── pom.xml
│   └── src/main/java/org/example/demoelasticsearch
├── log-producer-app/                 # second Spring Boot app (submodule)
│   ├── pom.xml
│   └── src/main/java/org/example/logproducer
├── scripts/bulk-load-sample.sh
└── pom.xml                           # parent pom (multi-module)
```

## Multi-Module Setup

Both apps are now organized as Maven modules under a parent `pom.xml`:

```bash
# Build all modules
./mvnw clean install

# Run specific module
./mvnw spring-boot:run -pl elasticsearch-demo-app
./mvnw spring-boot:run -pl log-producer-app
```

## 1) Main Spring Boot App (Document CRUD + Search + Aggregation)

Base URL: `http://localhost:8080`

### Start

```bash
./mvnw spring-boot:run -pl elasticsearch-demo-app
```

### APIs

OpenAPI + Swagger UI is enabled.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Use Swagger UI **Try it out** for:

1. `POST /api/documents` (Create document)
2. `GET /api/documents/{documentId}` (Get document)
3. `PUT /api/documents/{documentId}` (Update document)
4. `DELETE /api/documents/{documentId}` (Delete document)
5. `POST /api/documents/search` (Paginated search with criteria)
6. `GET /api/documents/aggregations/overview` (Aggregation overview)
7. `GET /api/documents/aggregations/bar-chart` (Bar chart aggregation)

For create/update/search, use payloads like:

```json
{
  "name": "Laptop",
  "category": "electronics",
  "price": 1200.0,
  "quantity": 10,
  "tags": ["hardware", "premium"]
}
```

**Bar chart aggregation by category (Quantity)**
   Returns data suitable for bar graphs showing total quantity grouped by category.
   
   Response format:
   ```json
   {
     "aggregationType": "category_quantity",
     "metric": "quantity",
     "totalDocuments": 10000,
     "data": [
       { "category": "category-0", "value": 2500.0, "valueType": "quantity" },
       { "category": "category-1", "value": 2100.0, "valueType": "quantity" }
     ]
   }
   ```

**Bar chart aggregation by category (Price - Average)**
   Returns average price grouped by category (perfect for bar graphs).
   
   Response format:
   ```json
   {
     "aggregationType": "category_price",
     "metric": "price",
     "totalDocuments": 10000,
     "data": [
       { "category": "category-0", "value": 105.5, "valueType": "price" },
       { "category": "category-1", "value": 98.2, "valueType": "price" }
     ]
   }
   ```

## 2) Bulk Insert 10k+ Documents

Script path: `scripts/bulk-load-sample.sh`

```bash
chmod +x scripts/bulk-load-sample.sh
./scripts/bulk-load-sample.sh 10000
```

Optional env vars:

- `ES_URL` (default `http://localhost:9200`)
- `ES_USER` (default `elastic`)
- `ES_PASSWORD` (default `elastic`)
- `INDEX` (default `sample-index`)
- `BATCH_SIZE` (default `1000`)

## 3) ELK Stack via Docker Compose

From repository root:

```bash
cd docker
docker compose up -d
```

Services:

- Elasticsearch: `http://localhost:9200`
- Logstash TCP input: `localhost:5000`
- Kibana: `http://localhost:5601`

Credentials used in this demo:

- username: `elastic`
- password: `elastic`

## 4) Structured Logging and Automatic Logstash Shipping (Best Practice)

Both apps use `logstash-logback-encoder` and send JSON logs directly to Logstash TCP input.

See `LOGSTASH_PIPELINE_GUIDE.md` for a detailed breakdown of the Logstash configuration and data flow.

Why this is a standard industry approach:

- Structured JSON logs (machine-readable).
- Async non-blocking logging appender.
- Trace identifiers (`traceId`, `spanId`) included for cross-service correlation.
- Centralized routing via Logstash, storage in Elasticsearch (`app-logs-*`).

Log destination is configurable with env var:

```bash
LOGSTASH_DESTINATION=localhost:5000
```

## 5) Second Spring Boot App for Interservice Log Tracing

Path: `log-producer-app`

Run (as a module):

```bash
./mvnw spring-boot:run -pl log-producer-app
```

Or directly from the module directory:

```bash
cd log-producer-app
../mvnw spring-boot:run
```

Base URL: `http://localhost:8081`

Generate interservice traced logs:

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Use `GET /api/demo/call-main` from Swagger UI with `message=test-call`.

This calls the main app endpoint (`/api/internal/ping`) and creates logs in both services sharing distributed-tracing context, which Logstash stores in Elasticsearch.

## 6) Fetch/Search/Trace Interservice Logs from Elasticsearch

Main app exposes log query APIs:

1. `GET /api/logs/search` for full-text log search
2. `GET /api/logs/traces/{traceId}` for trace lookup by traceId

Invoke both endpoints from `http://localhost:8080/swagger-ui/index.html`.

Use these APIs to retrieve end-to-end interservice interactions from Elasticsearch.

## 7) Kibana Dashboards and Log Viewing Best Practices

In Kibana (`http://localhost:5601`):

1. Go to **Stack Management → Data Views** and create data view: `app-logs-*`.
2. Set time field as `@timestamp`.
3. Use **Discover** for raw log exploration and ad-hoc searches.
4. Create dashboard visualizations commonly used in production:
   - Logs over time (line chart by `@timestamp`)
   - Error rate (`log.level:ERROR`) over time
   - Top services (`terms` on `service`)
   - Top exceptions (`terms` on `stack_trace`/exception field if present)
   - Trace drill-down panels (`traceId` and `spanId` breakdown)

Recommended operational dashboards:

- **Service Health Logs Dashboard**: request volume, errors, warning spikes.
- **Interservice Trace Dashboard**: traceId filtering, service hop timelines.
- **Incident Triage Dashboard**: top error messages, affected services, recent anomalies.

## Configuration Reference

Main app `src/main/resources/application.properties`:

- `elasticsearch.url`
- `elasticsearch.username`
- `elasticsearch.password`
- `elasticsearch.sample-index`
- `elasticsearch.logs-index-pattern`

Second app `log-producer-app/src/main/resources/application.properties`:

- `downstream.base-url`

## Notes

- `sample-index` is auto-created on startup by the main app if it does not exist.
- Logstash pipeline stores logs in `app-logs-YYYY.MM.dd`.
- For production, replace static credentials with secrets manager/Kubernetes secrets and enable TLS for all ELK endpoints.
