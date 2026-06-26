# Logstash Configuration and Data Flow

This repository ships application logs to Elasticsearch through Logstash over TCP.
The setup is split across three places:

1. The Spring Boot apps create structured JSON logs.
2. Logstash receives those logs on port `5000`.
3. Logstash writes them into Elasticsearch using a daily index.

## 1) High-level flow

```text
Spring Boot app
  -> Logback async Logstash appender
    -> TCP socket on Logstash port 5000
      -> Logstash pipeline
        -> Elasticsearch index app-logs-YYYY.MM.dd
```

## 2) Application-side logging

Both Spring Boot apps use the same `logback-spring.xml`.

### What it does

- Sends logs to the console for local debugging.
- Sends the same logs asynchronously to Logstash.
- Formats events as JSON so Logstash can parse them directly.
- Includes trace identifiers from MDC (`traceId`, `spanId`) for correlation.

### Important parts

`LOGSTASH_DESTINATION`

- Default: `localhost:5000`
- In Docker Compose: `logstash:5000`

`spring.application.name`

- Used as the `service` field in the JSON payload.

`AsyncAppender`

- Prevents log shipping from blocking the request thread.
- Buffers log events and forwards them to the TCP appender.

## 3) Logstash pipeline

The pipeline lives in:

`docker/logstash/pipeline/logstash.conf`

### Input

```conf
input {
  tcp {
    port => 5000
    codec => json
  }
}
```

This means:

- Logstash listens on TCP port `5000`.
- Incoming data must be JSON.
- The JSON is decoded into Logstash events automatically.

### Filter

```conf
filter {
  mutate {
    add_field => { "ingest_source" => "logstash-tcp" }
  }
}
```

This adds a small metadata field to every event so you can tell the event came from the TCP ingestion path.

### Output

```conf
output {
  elasticsearch {
    hosts => [ "${ELASTICSEARCH_HOST}" ]
    user => "${ELASTICSEARCH_USER}"
    password => "${ELASTICSEARCH_PASSWORD}"
    index => "app-logs-%{+YYYY.MM.dd}"
    ilm_enabled => false
  }
  stdout {
    codec => rubydebug
  }
}
```

This does two things:

- Sends the event to Elasticsearch.
- Prints the event to Logstash stdout for debugging.

## 4) Docker Compose wiring

The Docker Compose file connects the services together:

- Elasticsearch runs on `9200`
- Logstash exposes `5000` for incoming logs
- Kibana uses Elasticsearch for visualization

Logstash gets these environment variables:

- `ELASTICSEARCH_HOST=http://elasticsearch:9200`
- `ELASTICSEARCH_USER=elastic`
- `ELASTICSEARCH_PASSWORD=elastic`

Those values are injected into `logstash.conf` through `${...}` placeholders.

## 5) End-to-end data path

When the app logs something like:

```java
log.info("Calling main service with message={}", message);
```

the following happens:

1. Logback creates a structured JSON event.
2. The async appender queues the event.
3. The TCP appender sends the JSON to Logstash.
4. Logstash decodes the JSON with `codec => json`.
5. The filter adds `ingest_source=logstash-tcp`.
6. Logstash writes the event to Elasticsearch.
7. The document lands in an index named like `app-logs-2026.06.25`.

## 6) Why the logs are useful

Because the log event includes:

- timestamp
- message
- logger name
- log level
- thread name
- MDC fields
- trace and span IDs
- service name

you can search logs in Elasticsearch and Kibana by service, trace, time, or message content.

## 7) How to verify the pipeline

1. Start Elasticsearch, Logstash, and Kibana with Docker Compose.
2. Run either Spring Boot app.
3. Trigger an endpoint that writes logs.
4. Check Logstash stdout for decoded events.
5. Search `app-logs-*` in Kibana Discover.

## 8) Common configuration points

If you want to change the pipeline:

- Change the input port in `logstash.conf` if your app should send logs elsewhere.
- Change `LOGSTASH_DESTINATION` in the app if Logstash is not on `localhost:5000`.
- Change the Elasticsearch index pattern in `logstash.conf` if you want a different naming scheme.

