# Evaluation Lab – Akka

## Group Number: 36

## Group Members

* **Rong Huang**
* **Salvatore Mariano Librici**
* **Mohammadali Amiri**

## Description of Implemented Actors

* **AddressBookClientActor**:
  Acts as the client. It sends `PutMsg` (insert) and `GetMsg` (search) requests to the balancer.

* **AddressBookBalancerActor**:
  Receives requests from the client and routes them to the appropriate worker based on the initial letter of the name, using the `splitByInitial(String s)` function. It also manages data replication between workers to ensure fault tolerance.

* **AddressBookWorkerActor**:
  Handles storing entries as either primary or replica data. It also processes `RestMsg`, `ResumeMsg`, and `TimeoutMsg` messages to handle availability and downtime scenarios.

## Message Flow

1. The client sends a `PutMsg` or `GetMsg` to the balancer.
2. The balancer determines the primary worker using `splitByInitial` and forwards the request accordingly.
3. The workers store or retrieve data, maintaining both primary and replicated entries as defined by the replication logic.
4. If one worker is resting, the replica ensures that data remains accessible through the other worker.
5. If both workers are resting, the client receives a timeout message.

## Balancing

* The `splitByInitial(String s)` function divides names into two groups — A–M and N–Z — to determine which worker will serve as the primary for that entry.

## Changes and Implementation

* **Actors implemented:**
  `AddressBookClientActor`, `AddressBookBalancerActor`, and `AddressBookWorkerActor`.

* **Messages implemented:**
  `PutMsg`, `GetMsg`, `RestMsg`, `ResumeMsg`, `TimeoutMsg`, `ConfigMsg`, and `ReplyMsg`.

  * **`ReplyMsg`**:
    Carries the email address found for a requested name. It is used by the workers (through the balancer) to send lookup results back to the client.

    * If the entry exists, the client receives a `ReplyMsg` containing the email.
    * If no entry is found or both workers are unavailable, the client is notified via console messages or receives a `TimeoutMsg`, depending on the situation.

