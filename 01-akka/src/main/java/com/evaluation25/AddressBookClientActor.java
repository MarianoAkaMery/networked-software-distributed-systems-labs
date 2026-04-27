package com.evaluation25;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeoutException;
import static akka.pattern.Patterns.ask;
import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.ActorRef;

public class AddressBookClientActor extends AbstractActor {

	private scala.concurrent.duration.Duration timeout = scala.concurrent.duration.Duration.create(3, SECONDS);
	private ActorRef balancer;

	@Override
	public Receive createReceive() {

		return receiveBuilder()
				.match(PutMsg.class, this::putEntry)
				.match(GetMsg.class, this::query)
				.match(ConfigMsg.class, this::configureBalancer)
				.build();
	}

	void putEntry(PutMsg msg) {
		System.out.println("CLIENT: Sending new entry " + msg.getName() + " - " + msg.getEmail());
		balancer.tell(msg, getSelf());

	}

	void query(GetMsg msg) {
		// retreive email from balancer
		System.out.println("CLIENT: Issuing query for " + msg.getName());
		scala.concurrent.Future<Object> waitingForReply = ask(balancer, msg, 5000);
		try {
			GetMsg reply = (GetMsg) waitingForReply.result(timeout, null);
			if (reply.getName() != null) {
				System.out.println("CLIENT: Received reply, email is " + reply.getName());
			} else {
				System.out.println("CLIENT: Received reply, no email found!");
			}
		} catch (TimeoutException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	void handleReply(ReplyMsg msg) {
		System.out.println("CLIENT: Received reply, email is " + msg.getEmail());

	}

	static Props props() {
		return Props.create(AddressBookClientActor.class);
	}

	void configureBalancer(ConfigMsg msg) {
		this.balancer = msg.getBalancer();
	}

}
