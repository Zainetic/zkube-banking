# ZKube Distributed Resilient Banking Architecture & Chaos Engine

A fully containerized, highly available, and fault-tolerant microservices banking engine built to demonstrate modern distributed systems architecture. Orchestrated via Kubernetes, this system features decoupled ACID-compliant financial transactions, asynchronous Redis-based event queuing, and programmatic resilience testing using Chaos Mesh.

This architecture proves that even under extreme infrastructural duress (e.g., random server assassination mid-transaction), financial data remains strictly consistent, message queues heal themselves, and the system achieves eventual consistency without dropping a single user request.

# Architectural Overview

The cluster is divided into independent, scalable domains:

**Synchronous Accounts Service (REST):** Manages user creation and balance states, persisting directly to PostgreSQL via HikariCP connection pooling.

**Decoupled Transactions Service (REST -> Redis):** Executes atomic database locks for transferring funds between accounts. Upon successful commit, it publishes a notification payload to a Redis queue rather than waiting for a synchronous notification response.

**Asynchronous Notifications Worker:** A background consumer that listens to the Redis notification_queue. It processes the queue sequentially, ensuring delivery even if the worker pod is killed and restarted.

**Automated Infrastructure State:** PostgreSQL is bootstrapped automatically via Kubernetes ConfigMaps, ensuring the database schema is injected and ready before the Java applications boot.

# Key Technical Features

**Kubernetes Orchestration:** Implements declarative Deployments, LoadBalancers, and ClusterIP routing to ensure pods are constantly monitored and load-balanced. Strict livenessProbes ensure deadlocked applications are ruthlessly terminated and replaced.

**Event-Driven Self-Healing:** The Notification engine is completely decoupled. If the notification pod crashes, the core payment engine continues processing transactions perfectly, queuing messages in Redis until a new notification pod boots and processes the backlog.

**Automated Data Provisioning:** Utilizes K8s Volumes and ConfigMaps (/docker-entrypoint-initdb.d/init.sql) to completely automate database table creation (accounts and transfers) upon cluster initialization.

**Chaos Engineering Ready:** Fully integrated with Chaos Mesh to programmatically inject Pod Kill faults into the live cluster, proving zero-downtime resilience under active transactional load.

# Technology Stack

**Language:** Java 21

**Web Framework:** Javalin 6

**In-Memory Datastore / Queuing:** Redis 7.2 (Alpine)

**Relational Database:** PostgreSQL 16 (Alpine)

**Orchestration & DevOps:** Docker, Kubernetes (Kubeadm local cluster)

**Chaos Engineering:** Helm, Chaos Mesh

# Local Development & Deployment Guide

This guide walks through deploying the system from scratch to a local Docker Desktop Kubernetes cluster.

## 1. Build the Docker Images**
Navigate to each microservice directory and package the Java applications into Docker images. (Ensure you have run mvn clean package in Eclipse or Maven first to generate the FAT jars).

**Build Accounts Service**

_cd accounts-service
docker build -t zkube-accounts:latest ._

**Build Transactions Service**

_cd ../transactions-service
docker build -t zkube-transactions:latest ._

**Build Notifications Service**

_cd ../notifications-service
docker build -t zkube-notifications:latest ._

**Return to root directory**

_cd .._

## 2. Deploy Infrastructure & Databases
Deploy the persistence layer first. This applies the ConfigMap containing the SQL INIT scripts, spins up Redis, and boots Postgres.

_kubectl apply -f k8s/01-infrastructure.yaml_

**Wait for the databases to boot and run the automated SQL scripts:**

_kubectl get pods -w_

## 3. Deploy the Microservices

Once the databases are 1/1 Running, deploy the Java application tier.

_kubectl apply -f k8s/02-microservices.yaml_

## 4. Verify Cluster Health
 
Check that all pods are stable and all LoadBalancers have assigned ports.

_kubectl get pods_

**Check Networking and Ports**

_kubectl get svc_

**Note:** accounts-service will be exposed on port 8081 and transactions-service on port 8082.

# Chaos Engineering (Resilience Testing)
T
o prove the system's fault tolerance, we utilize Chaos Mesh to simulate catastrophic server failures during active transaction processing.

## 1. Install Helm (Windows)

If Helm is not installed, use the Windows Package Manager:

_winget install Helm.Helm_
(Restart terminal after installation).

## 2. Inject the Chaos Operator into Kubernetes

Add the Chaos Mesh repository and install the operator into an isolated namespace.

_helm repo add chaos-mesh https://charts.chaos-mesh.org
helm repo update
helm install chaos-mesh chaos-mesh/chaos-mesh -n chaos-mesh --create-namespace_

Wait for the Chaos pods to boot:

_kubectl get pods -n chaos-mesh -w_

## 3. Securely Access the War Room

Chaos Mesh requires a secure tunnel and a God-Mode token to access the attack dashboard.


**A. Open the Port-Forward Tunnel (Leave this terminal running):**

_kubectl port-forward -n chaos-mesh svc/chaos-dashboard 2333:2333_


**B. Generate the Master Authentication Token (Open a new terminal):**

Create Admin Account
_kubectl create serviceaccount chaos-admin -n chaos-mesh_

Grant Cluster-Admin Role
_kubectl create clusterrolebinding chaos-admin-binding --clusterrole=cluster-admin --serviceaccount=chaos-mesh:chaos-admin_

Generate 48-Hour Token
_kubectl create token chaos-admin -n chaos-mesh --duration=48h_


**C. Execute the Attack:**

Copy the generated token.

Navigate to http://localhost:2333 in your browser.

Paste the token to log in.

Set up a Pod Fault -> Pod Kill experiment targeting app: transactions with a continuous schedule.

## 4. Observe the Self-Healing

While the Chaos attack is running, stream the logs of the decoupled notification worker:

_kubectl logs deployment/notifications-deployment -f_

Fire high-volume traffic at the Transactions API using a Postman Runner. You will observe that even as pods are violently terminated, Kubernetes instantly reroutes traffic, transactions do not fail (200 OK), and the Redis queue safely preserves all notifications until a new worker spins up to process the backlog.

# API Documentation

**1. Create Account**

**Endpoint:** POST http://localhost:8081/api/accounts
**Payload:**

_JSON
{
    "ownerName": "Alice",
    "balance": 1000.00
}_


**2. Execute Transfer (Atomic)**

**Endpoint:** POST http://localhost:8082/api/transfers
**Payload:**

_JSON
{
    "fromAccount": "uuid-of-alice",
    "toAccount": "uuid-of-bob",
    "amount": 150.00
}_


**3. Kubernetes Health Probes**

Used internally by the K8s livenessProbe to ensure pod stability.

**Endpoint:** GET http://localhost:8082/api/transactions/health
**Response:**

_JSON
{
    "status": "UP", 
    "service": "transactions"
}_
