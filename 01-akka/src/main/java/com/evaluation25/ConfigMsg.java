package com.evaluation25;

import akka.actor.ActorRef;

public class ConfigMsg {

    private ActorRef worker0;
    private ActorRef worker1;
    private ActorRef Balancer;
    private ActorRef Client;

    public ConfigMsg(ActorRef Client, ActorRef Balancer, ActorRef worker0, ActorRef worker1) {
        this.Balancer = Balancer;
        this.worker0 = worker0;
        this.worker1 = worker1;
        this.Client = Client;
    }

    public ActorRef getBalancer() {
        return Balancer;
    }

    public ActorRef getClient() {
        return Client;
    }

    public ActorRef getWorker0() {
        return worker0;
    }

    public ActorRef getWorker1() {
        return worker1;
    }

}