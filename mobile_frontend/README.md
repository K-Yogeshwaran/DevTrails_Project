# GigShield Mobile Frontend

A Flutter mobile application that provides parametric income protection for gig workers. This is an exact mobile replica of the DevTrails web frontend, offering comprehensive insurance management, real-time disruption monitoring, and automated claim processing.

## Features

### Core Functionality
- **Authentication**: Secure login and registration with JWT tokens
- **Dashboard**: Real-time disruption monitoring and overview
- **Policy Management**: Tier-based insurance plans with seasonal options
- **Claims Processing**: Automated claim tracking with real-time updates
- **Wallet Management**: Transaction history and balance tracking
- **Admin Portal**: Comprehensive admin dashboard for management

### Real-time Features
- **Socket.io Integration**: Live disruption event monitoring
- **Push Notifications**: Instant claim status updates
- **Real-time Processing**: Live claim processing pipeline visualization

### Mobile Optimizations
- **Biometric Authentication**: Fingerprint/Face ID support
- **Location Services**: Automatic zone detection
- **Offline Support**: Local data caching and sync
- **Responsive Design**: Optimized for all screen sizes

## Technology Stack

- **Framework**: Flutter 3.x with Material Design 3
- **State Management**: Provider pattern
- **Networking**: Dio for HTTP requests
- **Real-time**: Socket.io client
- **Storage**: Flutter Secure Storage
- **Navigation**: GoRouter for declarative routing
- **Authentication**: JWT with secure storage

## Project Structure

```
lib/
|-- core/
|   |-- constants/          # API endpoints and app constants
|   |-- themes/             # App theme and styling
|   |-- utils/              # Utility functions
|   |-- services/           # API and socket services
|-- features/
|   |-- auth/               # Authentication screens
|   |-- dashboard/          # Main dashboard
|   |-- policy/             # Policy management
|   |-- claims/             # Claims processing
|   |-- wallet/             # Wallet functionality
|   |-- admin/              # Admin portal
|-- shared/
|   |-- widgets/            # Reusable UI components
|   |-- models/             # Data models
|   |-- providers/          # State management
```

## Getting Started

### Prerequisites
- Flutter SDK 3.0+
- Dart 3.0+
- Android Studio / VS Code
- Backend services running (see API documentation)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd mobile_frontend
```

2. Install dependencies:
```bash
flutter pub get
```

3. Run the app:
```bash
flutter run
```

### Backend Configuration

The mobile app requires the following backend services to be running:

- **Spring Boot Backend**: `http://localhost:8080`
- **Trigger Engine**: `http://localhost:5001`
- **Mock Platform API**: `http://localhost:5002`
- **ML Payout Calculator**: `http://localhost:5003`

See the API documentation for detailed endpoint information.

## API Integration

### Authentication
- Worker login and registration
- JWT token management
- Admin authentication

### Data Models
- Workers: Profile and earnings data
- Policies: Insurance plans and coverage
- Claims: Disruption claims and processing
- Wallet: Transactions and balance
- Trigger Events: Real-time disruption data

### Real-time Features
- Socket.io connection for live updates
- Claim processing pipeline
- Disruption event monitoring

## Development

### Code Style
- Follow Flutter/Dart conventions
- Use Provider for state management
- Implement proper error handling
- Write unit tests for business logic

### Testing
```bash
# Run all tests
flutter test

# Run specific test file
flutter test test/widget_test.dart

# Generate test coverage
flutter test --coverage
```

### Build for Production

```bash
# Android
flutter build apk --release

# iOS
flutter build ios --release

# Web (if supported)
flutter build web --release
```

## Configuration

### Environment Variables
Key configuration is handled in `lib/core/constants/api_constants.dart`:

- `baseUrl`: Backend API base URL
- `socketUrl`: Socket.io server URL
- Various API endpoints

### Theme Customization
App theme is defined in `lib/core/themes/app_theme.dart`:
- Dark theme by default
- Custom color scheme
- Material Design 3 components

## Security

- JWT tokens stored securely
- HTTPS in production
- Input validation and sanitization
- Biometric authentication support

## Performance

- Lazy loading for large datasets
- Efficient state management
- Image caching and optimization
- Background data sync

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write tests for new functionality
5. Submit a pull request

## License

This project is part of the DevTrails 2026 hackathon project for Sri Eshwar College of Engineering.

## Support

For any issues or questions:
1. Check the API documentation
2. Review the backend service status
3. Verify all services are running on correct ports
4. Check network connectivity

## Version History

- **v1.0.0**: Initial release with core functionality
  - Authentication system
  - Basic dashboard
  - Policy management
  - Claims processing
  - Wallet functionality
  - Admin portal
