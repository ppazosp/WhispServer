# WhispClient

WhispClient is the server implementation for **Whisp**, a peer-to-peer (P2P) instant messaging application focused on privacy and functionality. 
This project originated as part of a Distributed Computing course and has evolved into a fully functional application. 
Communication is powered by **Java RMI**, with security as a key feature.


## Key Features

- **Advanced Security**:
  - TLS encryption for client-server communication.
  - AES-GCM encryption for client-to-client communication.
  - Passwords stored encrypted in the database.
  - Two-factor authentication for enhanced security.

- **Client Functionality**:
  - Login/Registration system.
  - Friendship system.
  - Text/image-based chats.
  - Privacy-first design: messages are not stored and are deleted when both users disconnect.

- **P2P Communication**: Built using Java RMI for a smooth and decentralized experience.



## Contributors

- [ppazosp](https://github.com/ppazosp)
- [DavidMUSC](https://github.com/DavidMUSC)


## Related repositories

- [Client](https://github.com/ppazosp/WhispClient)
- [Server](https://github.com/ppazosp/WhispServer)
