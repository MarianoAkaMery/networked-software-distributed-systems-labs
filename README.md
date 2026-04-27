# Networked Software for Distributed Systems - Evaluation Labs

This repository collects six evaluation labs for the Networked Software for Distributed Systems course at Politecnico di Milano.

Each folder contains the final material for one distributed-systems technology. Tutorial sources, framework distributions, compiled outputs, local IDE settings, and temporary build artifacts were intentionally left out to keep the repository focused and lightweight.

## Repository Structure

| Folder | Technology | Content |
| --- | --- | --- |
| `01-akka/` | Akka | Java/Akka actor-based address book evaluation and related source code. |
| `02-contiki-ng/` | Contiki-NG | UDP client/server implementation for the Contiki-NG evaluation. |
| `03-kafka/` | Apache Kafka | Kafka producer/consumer evaluation project with Maven configuration. |
| `04-mpi/` | MPI | C implementation for the MPI local-minima evaluation. |
| `05-node-red/` | Node-RED | Node-RED flow export and lab README. |
| `06-spark/` | Apache Spark | Spark evaluation project with Java source code, input files, and slides. |

## Lab Notes

### 01 - Akka

Main project files:

- `pom.xml`
- `src/main/java/com/evaluation25/`
- `HOW_TO_RUN.txt`

Run from the folder with Maven, for example:

```bash
mvn compile
```

### 02 - Contiki-NG

Main project files:

- `udp-client.c`
- `udp-server.c`
- `Makefile`
- `debugFinal.csc`

The project is intended to be compiled and simulated inside a Contiki-NG/Cooja environment.

### 03 - Kafka

Main project files:

- `pom.xml`
- `src/main/java/it/polimi/nsds/kafka/eval/Producer.java`
- `src/main/java/it/polimi/nsds/kafka/eval/Consumers36.java`
- `src/main/java/it/polimi/nsds/kafka/admin/TopicManager.java`

Run from the folder with Maven after starting a Kafka broker:

```bash
mvn compile
```

### 04 - MPI

Main project files:

- `local_minima_36.c`
- `local_minima_XX.c`
- `NSDS25_MPI_eval.pdf`

Example compilation command:

```bash
mpicc local_minima_36.c -o local_minima
```

Example execution command:

```bash
mpirun -np 4 ./local_minima
```

### 05 - Node-RED

Main project files:

- `flows.json`
- `README.md`

Import `flows.json` into a Node-RED instance to inspect or run the flow.

### 06 - Spark

Main project files:

- `pom.xml`
- `src/main/java/it/polimi/nsds/eval/ProductAnalytics36.java`
- `input/`
- `slides/`
- `HOW_TO_RUN_MASTER_SLAVE.txt`

Run from the folder with Maven/Spark according to the notes in `HOW_TO_RUN_MASTER_SLAVE.txt`.
