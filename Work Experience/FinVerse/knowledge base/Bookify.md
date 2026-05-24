# 🗓️ Bookify.io — Scalable Reservation Booking System

**Bookify.io** is a production-grade reservation booking platform built with a modern **microservices architecture** using NestJS.
Designed for scalability, reliability, and maintainability, the system handles real-world booking workflows including secure payments, reservation management, authentication, notifications, and cloud deployment.

The platform is composed of four independent microservices that communicate through **gRPC** and RabbitMQ, enabling high-performance and fault-tolerant distributed communication.

---

## ✨ Features

* 📅 Reservation & booking management
* 💳 Secure payment processing with Stripe
* 🔐 JWT Authentication & Role-Based Access Control (RBAC)
* 📩 SMS & Email notifications using Twilio and SendGrid
* ⚡ High-performance microservice communication with gRPC
* 📨 Reliable asynchronous messaging with RabbitMQ
* ❄️ Unified GraphQL API Gateway using Apollo Federation
* ☁️ Cloud-native deployment on Amazon Web Services
* 🐳 Containerized infrastructure with Docker & Kubernetes
* 🚀 CI/CD automation using AWS CodePipeline & CodeBuild

---

## 🏗️ Architecture Highlights

### 🔹 Microservices Architecture

The application is split into independent services:

* 🔒 Auth Service
* 📅 Reservation Service
* 💳 Payment Service
* 📬 Notification Service

This architecture improves scalability, maintainability, and fault isolation.

---

### 🔹 Monorepo & Shared Common Library

The project follows a **NestJS Monorepo** structure with a centralized shared library for:

* Authentication
* Database access
* Logging
* Exception handling
* Shared utilities

This eliminates duplicated logic and keeps all services consistent.

---

### 🔹 Advanced Authentication & RBAC

The Auth Service implements:

* JWT authentication
* PassportJS strategies
* Dynamic role-based access control
* Route-level authorization decorators

Supports flexible user roles such as:

* Super Admin
* Admin
* Moderator
* Free
* Pro
* Premium
* Enterprise

---

### 🔹 Reliable Messaging & Communication

The system evolved from TCP-based communication to a more reliable architecture using:

* ⚡ gRPC for synchronous service communication
* 📨 RabbitMQ for asynchronous event-driven workflows

This ensures:

* Reliable message delivery
* Retry mechanisms
* Queue persistence
* Better scalability under heavy traffic

---

### 🔹 GraphQL API Gateway

Using Apollo Federation, multiple microservices are unified behind a single GraphQL endpoint, simplifying frontend integration while keeping services independent internally.

---

### 🔹 Production-Ready Infrastructure

The platform is fully containerized and cloud-ready with:

* Docker
* Kubernetes
* Helm
* AWS EKS
* AWS ECR
* AWS CodePipeline
* AWS CodeBuild

---

# 🚀 Tech Stack

## Backend

* NestJS
* Node.js
* TypeScript
* GraphQL
* Apollo Federation

## Databases

* MongoDB
* MySQL

## Communication & Messaging

* gRPC
* RabbitMQ
* TCP

## Cloud & DevOps

* Docker
* Kubernetes
* Helm
* Amazon Web Services
* AWS EKS
* AWS ECR
* AWS CodePipeline
* AWS CodeBuild

## Third-Party Services

* Stripe
* Twilio
* SendGrid

---
