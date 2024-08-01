import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class DNSClient {
  public static void main(String[] args) throws Exception {
    // DNS server address (Local DNS server)
    String dnsServer = "127.0.0.1";
    int port = 5354;

    // Create a scanner to get input from the user
    Scanner scanner = new Scanner(System.in);

    // Inform the user about the exit option
    System.out.println("DNS Client");
    System.out.println("Enter the domain name to resolve or type 'end' to quit.");

    // Infinite loop to continuously prompt the user
    while (true) {
      // Prompt user for the domain name to query
      System.out.print("Enter the domain name to resolve: ");
      String domainName = scanner.nextLine();

      // Check if the user wants to exit
      if (domainName.equalsIgnoreCase("end")) {
        break;
      }

      // Construct DNS query packet
      byte[] dnsQuery = constructDnsQuery(domainName);

      // Send DNS query
      DatagramSocket socket = new DatagramSocket();
      InetAddress address = InetAddress.getByName(dnsServer);
      DatagramPacket packet = new DatagramPacket(dnsQuery, dnsQuery.length, address, port);
      socket.send(packet);

      // Receive DNS response
      byte[] buffer = new byte[512];
      DatagramPacket response = new DatagramPacket(buffer, buffer.length);
      socket.receive(response);

      // Process and print the response
      System.out.println("Received response from DNS server:");
      parseDnsResponse(buffer, response.getLength());

      socket.close();
    }

    scanner.close();
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

  private static void parseDnsResponse(byte[] response, int length) {
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
        System.out.print("IPv4 Address: ");
        for (int i = 0; i < dataLength; i++) {
          System.out.print((response[position + i] & 0xFF));
          if (i < dataLength - 1) {
            System.out.print(".");
          }
        }
        System.out.println();
        return; // Exit after printing the first IPv4 address found
      }

      position += dataLength; // Move to the next record
    }

    System.out.println("No IPv4 address found in the DNS response.");
  }
}
