# DNS Server with Caching Mechanism

## Overview

This project implements a simple DNS server with a caching mechanism to resolve domain names to IP addresses, improve performance, and reduce latency. The server handles client requests, resolves domain names, and caches the results. Additionally, it provides a management interface for the DNS cache.

## Features

- Resolve domain names to IP addresses.
- Cache DNS responses to reduce latency.
- Provide a management interface for managing the DNS cache.
- Handle DNS queries efficiently and securely.

## Project owners
Gohar Aghajanyan - gohar_aghajanyan@edu.aua.am
Ruzanna Hunanyan -  ruzanna_hunanyan@edu.aua.am


## Prerequisites

- Java Development Kit (JDK) installed on your system.

## Setup

1. **Clone the Repository**

   Clone the repository or download the project files to your local machine.

   ```sh
   git clone https://github.com/goharAg/cs392-final-project
   

2. **Run the project**

   To run the project compile the files. 

   ```sh
   javac DNSCache.java DNSServer.java DNSClient.java
   ```
   Then run the server and client files in different tabs.
   ```sh
   java DNSServer

   java  DNSClient
   ```

