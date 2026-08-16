# dashboard-management-service

Dashboard storage and retrieval for the IoTMining/IIoTEdge platform:
tenant-scoped dashboard metadata plus the dashboard JSON payload itself
(stored via object-storage-service, presigned-URL-accessible).

## Features

- **Create/save/get/list** dashboards (`/api/dashboards/*`), all tenant-scoped.
- **Object storage integration** — dashboard content is stored/retrieved
  through `object-storage-service` via a Feign client (`ObjectStorageClient`),
  with presigned-URL support for direct client access.
- **Tenant isolation** — every endpoint takes `tenantId` directly as a
  parameter (not derived from a looked-up resource), so authorization is
  a direct match against the caller's own JWT `tenantId` claim rather
  than a per-resource ownership lookup (`DashboardSecurityEvaluator`,
  `@PreAuthorize("@dashboardSecurity.canAccessTenant(...)")`) - a super-admin
  claim bypasses this.
- **Stateless JWT auth** — `LiteJwtAuthenticationFilter` validates the
  bearer token and builds the `Authentication` from its claims; tokens
  are issued by auth-service and shared across services via a common
  signing secret.
- **CORS** via a standalone `CorsFilter` bean (`CorsConfig`), independent
  of Spring Security's own CORS integration.

## Running locally

Requires local Postgres (`iotmining` database), and optionally Eureka
(`localhost:8761`), Config Server (`localhost:8888`), and
object-storage-service - all degrade gracefully if unavailable.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Default port: `8052` (overridable via `SERVER_PORT`). JWT verification
requires `JWT_SECRET_KEY_BASE64` to match the key auth-service signs
with; the `dev` profile falls back to the same shared local-dev secret
used across this platform's other services if unset.

## Build

```bash
./mvnw clean verify
```

## Parent / dependency management

Resolves `iiotedge-parent`/`iiotedge-bom` from GitHub Packages
([`platform-build`](https://github.com/iiotedge/platform-build)) rather
than a local relative-path parent - this repo can be built standalone
with just this checkout, no monorepo checkout required (this service
has no dependency on the separate `common` repo).

## Docker

```bash
./mvnw clean package -DskipTests
docker build -t dashboard-management-service .
docker run -p 8052:8052 dashboard-management-service
```

The image is runtime-only (see `Dockerfile`): the jar is built beforehand
by CI, not inside the image, so the test gate in CI (`mvn verify`) is a
real gate that can't be bypassed by skipping straight to the Docker build.
