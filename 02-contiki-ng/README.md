# Evaluation lab - Contiki-NG

## Group number: 36

## Group members
- Salvatore Mariano Librici
- Rong Huang
- Mohammadali Amiri

## Solution description

**udp-server.c**  
From the initial include and definition section: the server import Contiki-NG networking modules and defines the UDP message protocol with message types (LOCK, WRITE, READ, REPLY), status codes (OK, ERROR) and a shared `message_t` structure.  
From the data structure section: the server maintains the shared value, a lock flag the IPv6 address of the lock owner, and a 5-second timeout timer.  
From `udp_rx_callback()`: the server processe client requests. LOCK is granted only if the structure is unlocked and starts the timeout. WRITE is accepted only if the sender holds the lock, updates the value, and releases the lock. READ is always allowed and returns the current value.  
From the main process loop: the server run as RPL DAG root and automaticaly releases the lock when the timeout expires.

**udp-client.c**  
From the protocol section: the client uses the same message format as the server.  
From the state machine section: each client executes a predetermined sequence (LOCK → WRITE → READ → LOCK → WRITE → READ).  
Each client derives its identifier from `node_id`, alowing multiple clients to behave differently in the same COOJA simulation.

**Test scenario**  
The COOJA simulation includes one server and two clients, validating exclusive locking, write protection, timeout-based lock release, and read-anytime access.
