# Guida all'Esecuzione - Kafka Eval 2025

## 1. Prerequisiti

- **Kafka** in esecuzione su `localhost:9092`
- **Java** compilato (il progetto usa Maven basato su `pom.xml`)

## 2. Compilare il Progetto

Esegui dalla radice del progetto:

\`\`\`bash
mvn clean compile
\`\`\`

## 3. Creare i Topic

Prima esegui \`TopicManager\` per creare i topic necessari:

```bash
mvn exec:java "-Dexec.mainClass=it.polimi.nsds.kafka.admin.TopicManager" "-Dexec.args=sensors1 4 1"
mvn exec:java "-Dexec.mainClass=it.polimi.nsds.kafka.admin.TopicManager" "-Dexec.args=sensors2 4 1"
mvn exec:java "-Dexec.mainClass=it.polimi.nsds.kafka.admin.TopicManager" "-Dexec.args=merged 4 1"
mvn exec:java "-Dexec.mainClass=it.polimi.nsds.kafka.admin.TopicManager" "-Dexec.args=output1 4 1"
mvn exec:java "-Dexec.mainClass=it.polimi.nsds.kafka.admin.TopicManager" "-Dexec.args=output2 4 1"
```

## 4. Eseguire i Componenti

**⚠️ Importante**: Ogni comando deve essere eseguito in un terminale separato.

### Terminal 1 - Merger
```bash
mvn exec:java "-Dexec.mainClass=it.polimi.nsds.kafka.eval.Consumers36" "-Dexec.args=0 merger-group"
```

### Terminal 2 - Validator
```bash
mvn exec:java "-Dexec.mainClass=it.polimi.nsds.kafka.eval.Consumers36" "-Dexec.args=1 1"
```

### Terminal 3 - Producer
````bash
mvn exec:java "-Dexec.mainClass=it.polimi.nsds.kafka.eval.Producer"
```

## Flusso di Dati

````bash
sensors1 ──┐
           ├─→ Merger ──→ merged ──→ Validator ──┬─→ output1
sensors2 ──┘                                      └─→ output2
```