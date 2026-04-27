package com.evaluation25;

import java.util.HashMap;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class AddressBookWorkerActor extends AbstractActor {

	private HashMap<String, String> primaryAddresses;
	private HashMap<String, String> replicaAddresses;
	private ActorRef balancer;

	public AddressBookWorkerActor() {
		this.primaryAddresses = new HashMap<String, String>();
		this.replicaAddresses = new HashMap<String, String>();
	}

	@Override
	public Receive createReceive() {
		return awake();
	}

	private final Receive awake() {
		return receiveBuilder()
				.match(GetMsg.class, this::generateReply)
				.match(RestMsg.class, this::onRestMsg)
				.match(ConfigMsg.class, this::configureBalancer)
				.build();
	}

	private final Receive asleep() {
		return receiveBuilder()
				.matchAny(null)
				.build();
	}

	void storeEntry(PutMsg msg) {
		System.out.println(this.toString() + ": Storing new entry " + msg.getName() + " - " + msg.getEmail());
		this.primaryAddresses.put(msg.getName(), msg.getEmail());
		this.replicaAddresses.put(msg.getName(), msg.getEmail());
	}

	void generateReply(GetMsg msg) {
		System.out.println(this.toString() + ": Received query for name " + msg.getName());
		// WE SHOULD REPLY TO THE CLIENT FROM PRIMARY COPY
		if (this.primaryAddresses.containsKey(msg.getName())) {
			String email = this.primaryAddresses.get(msg.getName());
			System.out.println(
					this.toString() + ": Replying to query for name " + msg.getName() + " with email " + email);

			// SEND BACK TO THE balancer ACTOR THE NAME
			balancer.tell(new ReplyMsg(email), getSelf());

		} else {
			System.out.println(this.toString() + ": No entry found for name " + msg.getName());
		}

	}

	void onRestMsg(RestMsg msg) {
		System.out.println(this.toString() + ": Received REST message.");
		this.replicaAddresses = new HashMap<String, String>(this.primaryAddresses);
		getContext().become(asleep());
	}

	void onTimeoutMsg(TimeoutMsg msg) {
		System.out.println(this.toString() + ": Timeout occurred, switching to replica addresses.");
		// When a worker receives a RestMsg, it stops responding to queries and silenty
		// drops them.

	}

	void onResumeMessages(ResumeMsg msg) {
		System.out.println(this.toString() + ": Resuming message processing.");
		getContext().become(awake());
	}

	void configureBalancer(ConfigMsg msg) {
		this.balancer = msg.getBalancer();
	}

	static Props props() {
		return Props.create(AddressBookWorkerActor.class);
	}

}
