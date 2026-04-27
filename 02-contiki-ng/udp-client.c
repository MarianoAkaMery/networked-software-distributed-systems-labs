#include "contiki.h"
#include "net/routing/routing.h"
#include "random.h"
#include "net/netstack.h"
#include "net/ipv6/simple-udp.h"
#include <inttypes.h>
#include "node-id.h"

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

#define START_INTERVAL		(15 * CLOCK_SECOND)
#define SEND_INTERVAL		  (60 * CLOCK_SECOND)

static struct simple_udp_connection udp_conn;

/*---------------------------------------------------------------------------*/
PROCESS(udp_client_process, "UDP client");
AUTOSTART_PROCESSES(&udp_client_process);
/*---------------------------------------------------------------------------*/

/* Test sequence states */
enum {
  STATE_LOCK_1 = 0,
  STATE_WRITE_1 = 1,
  STATE_READ_1 = 2,
  STATE_LOCK_2 = 3,
  STATE_WRITE_2 = 4,
  STATE_READ_2 = 5,
  STATE_DONE = 6
};

static int test_state = STATE_LOCK_1;
static int client_id;

static void
udp_rx_callback(struct simple_udp_connection *c,
         const uip_ipaddr_t *sender_addr,
         uint16_t sender_port,
         const uip_ipaddr_t *receiver_addr,
         uint16_t receiver_port,
         const uint8_t *data,
         uint16_t datalen)
{
  message_t *msg = (message_t *)data;
  
  if(datalen < sizeof(message_t)) {
    LOG_INFO("Invalid response size\n");
    return;
  }

  LOG_INFO("Response from ");
  LOG_INFO_6ADDR(sender_addr);
  
  if(msg->type == MSG_REPLY) {
    if(msg->status == STATUS_OK) {
      LOG_INFO(" - SUCCESS - Value: %" PRId32 "\n", msg->value);
    } else {
      LOG_INFO(" - ERROR\n");
    }
    test_state++;
  }
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_client_process, ev, data)
{
  static struct etimer periodic_timer;
  message_t msg_out;
  uip_ipaddr_t dest_ipaddr;

  PROCESS_BEGIN();

  simple_udp_register(&udp_conn, UDP_CLIENT_PORT, NULL,
                      UDP_SERVER_PORT, udp_rx_callback);

  etimer_set(&periodic_timer, START_INTERVAL);
  client_id = node_id;

  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));

    if(NETSTACK_ROUTING.node_is_reachable() && NETSTACK_ROUTING.get_root_ipaddr(&dest_ipaddr)) {
      
      switch(test_state) {
        case STATE_LOCK_1:
          LOG_INFO("Client %d: Sending LOCK\n", client_id);
          msg_out.type = MSG_LOCK;
          msg_out.value = 0;
          msg_out.status = 0;
          simple_udp_sendto(&udp_conn, &msg_out, sizeof(msg_out), &dest_ipaddr);
          break;

        case STATE_WRITE_1:
          LOG_INFO("Client %d: Sending WRITE (100)\n", client_id);
          msg_out.type = MSG_WRITE;
          msg_out.value = 100 + client_id;
          msg_out.status = 0;
          simple_udp_sendto(&udp_conn, &msg_out, sizeof(msg_out), &dest_ipaddr);
          break;

        case STATE_READ_1:
          LOG_INFO("Client %d: Sending READ\n", client_id);
          msg_out.type = MSG_READ;
          msg_out.value = 0;
          msg_out.status = 0;
          simple_udp_sendto(&udp_conn, &msg_out, sizeof(msg_out), &dest_ipaddr);
          break;

        case STATE_LOCK_2:
          LOG_INFO("Client %d: Sending LOCK (second)\n", client_id);
          msg_out.type = MSG_LOCK;
          msg_out.value = 0;
          msg_out.status = 0;
          simple_udp_sendto(&udp_conn, &msg_out, sizeof(msg_out), &dest_ipaddr);
          break;

        case STATE_WRITE_2:
          LOG_INFO("Client %d: Sending WRITE (200)\n", client_id);
          msg_out.type = MSG_WRITE;
          msg_out.value = 200 + client_id;
          msg_out.status = 0;
          simple_udp_sendto(&udp_conn, &msg_out, sizeof(msg_out), &dest_ipaddr);
          break;

        case STATE_READ_2:
          LOG_INFO("Client %d: Sending READ (final)\n", client_id);
          msg_out.type = MSG_READ;
          msg_out.value = 0;
          msg_out.status = 0;
          simple_udp_sendto(&udp_conn, &msg_out, sizeof(msg_out), &dest_ipaddr);
          break;

        case STATE_DONE:
          LOG_INFO("Client %d: Test sequence completed\n", client_id);
          PROCESS_EXIT();
          break;
      }
    } else {
      LOG_INFO("Not reachable yet\n");
    }

    /* Wait before next operation */
    etimer_set(&periodic_timer, 1 * CLOCK_SECOND);
  }

  PROCESS_END();
}
