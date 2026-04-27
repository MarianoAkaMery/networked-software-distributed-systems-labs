package com.evaluation25;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class AddressBookBalancerActor extends AbstractActor {

	ActorRef worker1 = null;
	ActorRef worker0 = null;
	ActorRef client = null;

	public AddressBookBalancerActor() {

	}

	@Override
	public Receive createReceive() {
		// TODO: Rewrite next line...

		return receiveBuilder()
				.match(ConfigMsg.class, this::configureWorkers)
				.match(PutMsg.class, this::storeEntry)
				.match(GetMsg.class, this::routeQuery)
				.build();

	}

	int splitByInitial(String s) {
		char firstChar = s.charAt(0);

		// Normalize case for comparison
		char upper = Character.toUpperCase(firstChar);

		if (upper >= 'A' && upper <= 'M') {
			return 0;
		} else {
			return 1;
		}
	}

	void routeQuery(GetMsg msg) {

		System.out.println("BALANCER: Received query for name " + msg.getName());
		if (splitByInitial(msg.getName()) == 0) {
			worker0.tell(msg, getSelf());
		} else {
			worker1.tell(msg, getSelf());
		}
		// ...
		System.out.println("BALANCER: Primary copy query for name " + msg.getName() + " is resting!");

		// ...
		System.out.println("BALANCER: Both copies are resting for name " + msg.getName() + "!");

	}

	void routeReply(ReplyMsg msg) {

		System.out.println("BALANCER: Received query for name " + msg.getEmail());
		// send to client
		client.tell(msg, getSelf());

		System.out.println("BALANCER: Primary copy query for name " + msg.getEmail() + " is resting!");

		// ...
		System.out.println("BALANCER: Both copies are resting for name " + msg.getEmail() + "!");

	}

	void storeEntry(PutMsg msg) {
		System.out.println("BALANCER: Received new entry " + msg.getName() + " - " + msg.getEmail());
		if (splitByInitial(msg.getName()) == 0) {
			worker0.tell(msg, getSelf());
		} else {
			worker1.tell(msg, getSelf());
		}
	}

	static Props props() {
		return Props.create(AddressBookBalancerActor.class);
	}

	void configureWorkers(ConfigMsg msg) {
		this.worker0 = msg.getWorker0();
		this.worker1 = msg.getWorker1();
		this.client = msg.getClient();
	}

}
