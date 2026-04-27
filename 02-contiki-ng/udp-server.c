#include "contiki.h"
#include "net/routing/routing.h"
#include "net/netstack.h"
#include "net/ipv6/simple-udp.h"
#include <inttypes.h>

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

#define WITH_SERVER_REPLY  1
#define UDP_CLIENT_PORT	8765
#define UDP_SERVER_PORT	5678

/* Message types */
#define MSG_LOCK    1
#define MSG_WRITE   2
#define MSG_READ    3
#define MSG_REPLY   4

/* Response status */
#define STATUS_OK     0
#define STATUS_ERROR  1

/* Message structure */
typedef struct {
  uint8_t type;
  int32_t value;
  uint8_t status;
} message_t;

/* Data structure and lock management */
static int32_t data_value = 0;
static uip_ipaddr_t lock_owner;
static int is_locked = 0;
static struct etimer lock_timeout;

static struct simple_udp_connection udp_conn;

PROCESS(udp_server_process, "UDP server");
AUTOSTART_PROCESSES(&udp_server_process);
/*---------------------------------------------------------------------------*/
static void
release_lock()
{
  is_locked = 0;
  LOG_INFO("Lock released after timeout\n");
}
/*---------------------------------------------------------------------------*/
static void
udp_rx_callback(struct simple_udp_connection *c,
         const uip_ipaddr_t *sender_addr,
         uint16_t sender_port,
         const uip_ipaddr_t *receiver_addr,
         uint16_t receiver_port,
         const uint8_t *data,
         uint16_t datalen)
{
  message_t *msg_in = (message_t *)data;
  message_t msg_out;
  
  if(datalen < sizeof(message_t)) {
    LOG_INFO("Invalid message size\n");
    return;
  }

  msg_out.status = STATUS_OK;
  msg_out.value = data_value;

  switch(msg_in->type) {
    case MSG_LOCK:
      LOG_INFO("LOCK request from ");
      LOG_INFO_6ADDR(sender_addr);
      LOG_INFO_("\n");
      
      if(!is_locked) {
        is_locked = 1;
        uip_ipaddr_copy(&lock_owner, sender_addr);
        etimer_set(&lock_timeout, 5 * CLOCK_SECOND);
        msg_out.type = MSG_REPLY;
        LOG_INFO("Lock acquired by ");
        LOG_INFO_6ADDR(sender_addr);
        LOG_INFO_("\n");
      } else {
        LOG_INFO("Lock denied - already locked\n");
        return;
      }
      break;

    case MSG_WRITE:
      LOG_INFO("WRITE request: %" PRId32 " from ", msg_in->value);
      LOG_INFO_6ADDR(sender_addr);
      LOG_INFO_("\n");
      
      if(is_locked && uip_ipaddr_cmp(&lock_owner, sender_addr)) {
        data_value = msg_in->value;
        msg_out.type = MSG_REPLY;
        msg_out.value = data_value;
        LOG_INFO("Value written: %" PRId32 "\n", data_value);
        release_lock();
      } else {
        msg_out.type = MSG_REPLY;
        msg_out.status = STATUS_ERROR;
        LOG_INFO("WRITE denied - not locked by this client\n");
      }
      break;

    case MSG_READ:
      LOG_INFO("READ request from ");
      LOG_INFO_6ADDR(sender_addr);
      LOG_INFO_("\n");
      msg_out.type = MSG_REPLY;
      msg_out.value = data_value;
      LOG_INFO("Current value: %" PRId32 "\n", data_value);
      break;

    default:
      LOG_INFO("Unknown message type: %u\n", msg_in->type);
      return;
  }

#if WITH_SERVER_REPLY
  simple_udp_sendto(&udp_conn, &msg_out, sizeof(msg_out), sender_addr);
#endif 
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_server_process, ev, data)
{
  PROCESS_BEGIN();

  NETSTACK_ROUTING.root_start();

  simple_udp_register(&udp_conn, UDP_SERVER_PORT, NULL,
                      UDP_CLIENT_PORT, udp_rx_callback);

  while(1) {
    PROCESS_WAIT_EVENT();
    
    if(etimer_expired(&lock_timeout) && is_locked) {
      release_lock();
    }
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
