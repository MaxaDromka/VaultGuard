# VaultGuard 🔒

VaultGuard is a powerful and user-friendly encryption management system that provides secure container-based encryption using LUKS2. It offers a modern Java-based interface for managing encrypted containers with ease.

## 🌟 Features

- **Secure Container Management**
  - Create encrypted containers with customizable sizes
  - Support for multiple encryption algorithms
  - LUKS2 encryption format
  - Automatic container mounting/unmounting

- **User-Friendly Interface**
  - Modern JavaFX-based GUI
  - Intuitive container management
  - Real-time encryption status monitoring
  - Easy-to-use container operations

- **Advanced Security Features**
  - LUKS2 encryption support
  - Multiple encryption algorithms
  - Secure password handling
  - Automatic key management

- **System Integration**
  - Automatic mounting support
  - Integration with system security policies
  - Support for various filesystem types
  - UDisks integration for device management

## 🚀 Getting Started

### Prerequisites

- Java Runtime Environment (JRE) 11 or higher
- cryptsetup-devel package
- Root access for encryption operations
- Linux-based operating system

### Installation

1. Install required dependencies:
```bash
# For Fedora/RHEL-based systems
sudo dnf install cryptsetup-devel

# For Ubuntu/Debian-based systems
sudo apt-get install cryptsetup-dev
```

2. Clone the repository:
```bash
git clone https://github.com/yourusername/VaultGuard.git
cd VaultGuard
```

3. Build the project:
```bash
./gradlew build
```

4. Run the application:
```bash
./gradlew run
```

## 💻 Usage

### Creating a New Container

1. Launch VaultGuard
2. Click "Create New Container"
3. Specify:
   - Container name
   - Size
   - Encryption algorithm
   - Password
   - Filesystem type

### Managing Containers

- **Mount Container**: Select container and click "Mount"
- **Unmount Container**: Select mounted container and click "Unmount"
- **Delete Container**: Select container and click "Delete"

## 🔧 Technical Details

### Supported Encryption Algorithms

- AES
- Twofish
- Serpent
- And more (depending on system capabilities)

### Filesystem Support

- ext4 (default)
- ext3
- ext2
- And other Linux filesystems

### Security Features

- LUKS2 encryption format
- Secure password handling
- Automatic key management
- System-level security integration

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
