# Trade Event Bus

![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-2.3-087CFA?logo=ktor&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)

Lightweight event bus for customs trade systems. Pub/sub messaging for declaration events, risk alerts, and compliance notifications. Ktor-based REST API with WebSocket streaming. In-memory event store with replay capability.

## Architecture

```
Producer --> [REST API] --> EventBus --> EventStore (circular buffer)
                                |
                                +--> Dispatcher --> Subscriber callbacks
                                |
                           [SSE Stream] --> Connected clients
```

## Event Types

| Event | Description |
|-------|-------------|
| `DECLARATION_SUBMITTED` | New customs declaration filed |
| `RISK_SCORED` | Risk assessment completed |
| `INSPECTION_SCHEDULED` | Physical inspection ordered |
| `CLEARANCE_GRANTED` | Goods cleared for release |
| `ALERT_RAISED` | Compliance alert triggered |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/publish` | Publish a trade event |
| `GET` | `/events` | Query stored events with optional filters |
| `GET` | `/events/stream` | SSE stream of real-time events |
| `POST` | `/subscribe` | Register a webhook subscriber |
| `DELETE` | `/subscribe/{id}` | Remove a subscriber |

## Quick Start

```bash
# Build
make build

# Run
make run

# Test
make test

# Docker
make docker-build
make docker-run
```

## Configuration

The server starts on port `8080` by default. Configure via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | Server port |
| `EVENT_STORE_CAPACITY` | `10000` | Max events in circular buffer |

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
