# Quantifying Object Hydration Overhead in Hibernate-Based Enterprise Applications

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.2-green)](https://spring.io/projects/spring-boot)
[![Hibernate](https://img.shields.io/badge/Hibernate-6.6.5-yellow)](https://hibernate.org/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)

This repository hosts the source code and experimental artifacts for the research paper **"Quantifying Object Hydration Overhead in Hibernate-Based Enterprise Applications"** (Submitted to *SN Computer Science*).

The project benchmarks the performance of **Hibernate 6 (JPQL)** vs. **Native SQL** to isolate the CPU overhead incurred during the **Object Hydration** phase (transforming JDBC ResultSets into Managed Entities) in high-throughput scenarios.

##  Research Context

* **Problem:** Enterprise applications often suffer from performance bottlenecks during bulk read operations, typically attributed to database query execution.
* **Finding:** Our research reveals a **"Hydration Cliff"**—a ~390ms latency gap caused purely by the mapping of ResultSets to Managed Entities, even when queries are fully optimized.
* **Dataset:** A synthetic invoice dataset of **1,000,000 records** with strict referential integrity (Invoice $\rightarrow$ Store, Customer, User).

##  Tech Stack

* **Language:** Java 21 (OpenJDK)
* **Framework:** Spring Boot 3.4.2
* **ORM:** Hibernate Core 6.6.5
* **Database:** Microsoft SQL Server 2022
* **Build Tool:** Maven/Gradle
* **IDE IntelliJ**

##  Benchmark Scenarios

We evaluated 4 distinct scenarios to measure latency and database load:

| ID | Scenario | Description | Key Metric |
| :--- | :--- | :--- | :--- |
| **S1** | **Simple Read** | Lookup single Invoice by ID (`findById`). | Baseline Latency |
| **S2** | **Aggregation** | Complex analytical query (`SUM`, `GROUP BY`) over 12 months. | Calculation Speed |
| **S3** | **Bulk Retrieval** | Fetch top 5,000 invoices + associated EAGER entities. | **Hydration Stress Test** |
| **S4** | **DTO Projection** | Fetch top 5,000 records mapped directly to Java Records. | Optimized Read |

##  Key Results

The benchmark revealed that **DTO Projections** significantly outperform Entity fetching, even when using Native SQL.

| Scenario | Strategy | Mean Latency | JDBC Statements | Verdict |
| :--- | :--- | :--- | :--- | :--- |
| **S3** | Native SQL (Entity) | **~4,563 ms** | ~4,810 | ❌ Slowest (Silent N+1) |
| **S3** | JPQL (Default) | ~3,851 ms | ~4,809 | ❌ Severe N+1 issues |
| **S3** | JPQL (Optimized) | ~535 ms | ~51 | ⚠️ Hydration Cliff (~390ms overhead) |
| **S4** | **DTO Projection** | **~145 ms** | **1** | ✅ **Recommended** |

*> **Insight:** Using Native SQL but mapping to Entities (`createNativeQuery(sql, Entity.class)`) resulted in the worst performance due to the overhead of constructing the Persistence Context.*

##  How to Run

### 1. Prerequisites
* Java 21+
* SQL Server 2022 (Local or Docker)

### 2. Configuration
Update your database credentials in `src/main/resources/application-benchmark.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=rice_store;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=YourStrongPassword
This project uses a dedicated benchmark profile to minimize logging and optimize connection pooling.
./mvnw spring-boot:run -Dspring-boot.run.profiles=benchmark-invoice
Data Generation & Benchmarking
Once the application is running (Console logs "Started RiceStoreApplication"), use cURL or Postman to trigger the processes.
Generate Fake Data (1M Records) This process uses the application logic to seed the database with synthetic invoice data.
