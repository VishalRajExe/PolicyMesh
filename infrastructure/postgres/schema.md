# PostgreSQL schema ownership

Infrastructure creates the PostgreSQL database and `pgcrypto` extension only. Spring Boot owns migrations and the application data model: `User`, `Policy`, `ServiceNode`, `DataFlowEdge`, `Decision`, `LineageRecord`, `CIScan`, and `AIClassification`. Do not add a competing table schema here. The local configuration currently uses JPA/Hibernate; migrate to Flyway/Liquibase only as a backend-owned decision.
