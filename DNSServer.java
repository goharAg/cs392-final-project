
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DNSServer {
  private static final int PORT = 5354;
  private static final DNSCache cache = new DNSCache();

  public static void main(String[] args) throws Exception {
    DatagramSocket serverSocket = new DatagramSocket(PORT);

    while (true) {
      byte[] receiveData = new byte[512];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      serverSocket.receive(receivePacket);

      InetAddress clientAddress = receivePacket.getAddress();
      int clientPort = receivePacket.getPort();

      String domainName = extractDomainName(receiveData);
      String cachedIp = cache.get(domainName);

      if (cachedIp != null) {
        // Respond with cached IP
        byte[] response = constructDnsResponse(receiveData, cachedIp);
        DatagramPacket sendPacket = new DatagramPacket(response, response.length, clientAddress, clientPort);
        serverSocket.send(sendPacket);
      } else {
        // Forward query to external DNS server
        byte[] dnsQuery = constructDnsQuery(domainName);
        InetAddress dnsServerAddress = InetAddress.getByName("8.8.8.8");
        DatagramPacket queryPacket = new DatagramPacket(dnsQuery, dnsQuery.length, dnsServerAddress, 53);
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.send(queryPacket);

        byte[] buffer = new byte[512];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        clientSocket.receive(responsePacket);
        clientSocket.close();

        String ipAddress = extractIpAddress(buffer, responsePacket.getLength());
        if (ipAddress != null) {
          cache.put(domainName, ipAddress);
        }

        DatagramPacket sendPacket = new DatagramPacket(buffer, responsePacket.getLength(), clientAddress, clientPort);
        serverSocket.send(sendPacket);
      }
    }
  }

  private static String extractDomainName(byte[] query) {
    StringBuilder domainName = new StringBuilder();
    int position = 12; // Skip the header

    while (query[position] != 0) {
      int length = query[position];
      for (int i = 1; i <= length; i++) {
        domainName.append((char) query[position + i]);
      }
      domainName.append('.');
      position += length + 1;
    }

    return domainName.substring(0, domainName.length() - 1); // Remove the trailing dot
  }

  private static String extractIpAddress(byte[] response, int length) {
    int position = 12; // Skip the header

    // Skip the question section
    while (response[position] != 0) {
      position++;
    }
    position += 5; // Skip null byte and QTYPE/QCLASS

    // Read the answer section
    while (position < length) {
      // Skip the name field
      if ((response[position] & 0xC0) == 0xC0) {
        position += 2; // It's a pointer
      } else {
        while (response[position] != 0) {
          position++;
        }
        position++;
      }

      // Read TYPE, CLASS, TTL (10 bytes total)
      int type = ((response[position] & 0xFF) << 8) | (response[position + 1] & 0xFF);
      int dataLength = ((response[position + 8] & 0xFF) << 8) | (response[position + 9] & 0xFF);
      position += 10; // Move to the data section

      if (type == 1 && dataLength == 4) { // IPv4 address
        StringBuilder ipAddress = new StringBuilder();
        for (int i = 0; i < dataLength; i++) {
          ipAddress.append(response[position + i] & 0xFF);
          if (i < dataLength - 1) {
            ipAddress.append(".");
          }
        }
        return ipAddress.toString();
      }

      position += dataLength; // Move to the next record
    }

    return null;
  }

  private static byte[] constructDnsQuery(String domainName) throws Exception {
    byte[] header = new byte[12];
    byte[] question = buildQuestion(domainName);

    // Set ID (2 bytes)
    header[0] = 0x12; // Random ID
    header[1] = 0x34;

    // Set flags (2 bytes) - standard query
    header[2] = 0x01;
    header[3] = 0x00;

    // QDCOUNT (2 bytes) - 1 question
    header[4] = 0x00;
    header[5] = 0x01;

    // ANCOUNT, NSCOUNT, ARCOUNT are 0
    header[6] = 0x00;
    header[7] = 0x00;
    header[8] = 0x00;
    header[9] = 0x00;
    header[10] = 0x00;
    header[11] = 0x00;

    // Combine header and question
    byte[] dnsQuery = new byte[header.length + question.length];
    System.arraycopy(header, 0, dnsQuery, 0, header.length);
    System.arraycopy(question, 0, dnsQuery, header.length, question.length);

    return dnsQuery;
  }

  private static byte[] buildQuestion(String domainName) throws Exception {
    String[] parts = domainName.split("\\.");
    byte[] question = new byte[domainName.length() + 2 + 4]; // Lengths of each part + dots + QTYPE and QCLASS

    int pos = 0;
    for (String part : parts) {
      question[pos] = (byte) part.length();
      pos++;
      for (char c : part.toCharArray()) {
        question[pos] = (byte) c;
        pos++;
      }
    }

    // Null byte to end the QNAME
    question[pos] = 0;
    pos++;

    // QTYPE (2 bytes) - A record (IPv4 address)
    question[pos] = 0x00;
    question[pos + 1] = 0x01;
    pos += 2;

    // QCLASS (2 bytes) - IN (Internet)
    question[pos] = 0x00;
    question[pos + 1] = 0x01;

    return question;
  }

  private static byte[] constructDnsResponse(byte[] query, String ipAddress) {
    byte[] response = new byte[512];

    // Copy the query header to the response
    System.arraycopy(query, 0, response, 0, 12);

    // Set response flags
    response[2] = (byte) 0x81;
    response[3] = (byte) 0x80;

    // Set QDCOUNT to 1
    response[4] = 0x00;
    response[5] = 0x01;

    // Set ANCOUNT to 1
    response[6] = 0x00;
    response[7] = 0x01;

    // Copy the question section to the response
    int position = 12;
    while (query[position] != 0) {
      position++;
    }
    position += 5;
    System.arraycopy(query, 12, response, 12, position - 12);

    // Start of answer section
    int answerStart = position;

    // Name (pointer to the question)
    response[position++] = (byte) 0xC0;
    response[position++] = 0x0C;

    // Type (A record)
    response[position++] = 0x00;
    response[position++] = 0x01;

    // Class (IN)
    response[position++] = 0x00;
    response[position++] = 0x01;

    // TTL
    response[position++] = 0x00;
    response[position++] = 0x00;
    response[position++] = 0x00;
    response[position++] = 0x3C;

    // Data length
    response[position++] = 0x00;
    response[position++] = 0x04;

    // IP address
    String[] ipParts = ipAddress.split("\\.");
    for (String part : ipParts) {
      response[position++] = (byte) Integer.parseInt(part);
    }

    return response;
  }
}

