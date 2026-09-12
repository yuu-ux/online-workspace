# Online Workspace Application Requirements Specification

## Table of Contents

1. [Purpose](#1-purpose)
2. [Scope and Assumptions](#2-scope-and-assumptions)
3. [Technology Stack](#3-technology-stack)
4. [Scope](#4-scope)
5. [Functional Requirements](#5-functional-requirements)
6. [Screen Requirements](#6-screen-requirements)
7. [Non-functional Requirements](#7-non-functional-requirements)
8. [Security Requirements](#8-security-requirements)

## 1. Purpose

The purpose of this application is to **provide an online place to work together for people who find it difficult to make progress working at home or who want to work with someone else**.

### 1.1 Difference from Existing Applications

Existing online workspaces are designed on the assumption that users become friends, so we felt that they do not meet the need of wanting to work together with anyone. Therefore, this application allows logged-in users to create rooms that can be joined without requiring a friendship relationship.

## 2. Scope and Assumptions

- Delivery format: Web application
- Primary users: General users
- Supported environment: Google Chrome
    - Version 150 (we plan to align this with the stable version at the time of submission, but have provisionally set it for now)
    - Must run on campus PCs
    - Reference: https://chromiumdash.appspot.com/schedule
- Target release date: July 10, 2026

## 3. Technology Stack

### 3.1 Frontend

- React
    - React Router
- Tailwind CSS

### 3.2 Backend

- Spring Boot
    - Spring Security
    - Mapper
    - Bean Validation
    - Flyway

### 3.3 Database

- PostgreSQL

## 4. Scope

### 4.1 Existing (MVP) Features

- User registration / login
- Room listing
- Room creation / joining / leaving
- In-room chat
- Real-time updates through WebSocket
- In-app notifications

### 4.2 Must-have Features for the Final Version (by July 10)

1. Friend features
2. Room settings
3. System monitoring
4. My Page, profile editing, and account withdrawal
5. Privacy policy / terms of service

See Section 5, Functional Requirements, for details of each feature.

### 4.3 Optional Features

- Google login
- Two-factor authentication
- Browser push notifications
- In-app notification features
    - Messages for terms-of-service violations
    - Friend request messages
- When a room is created and an existing room with a similar category exists, display a message encouraging the user to join it.
  (Example: "There is a similar room. Would you like to join it?")
  Note: This is idea-based, so whether to adopt it should be considered.

## 5. Functional Requirements

### 5.1 Friend Features

- Users can register other users as friends.
- Friend registration does not have a request and approval flow; the friend is added immediately when the action is performed.
- Users can remove registered friends.
- Users can display their friend list.

### 5.2 Room Settings

- Users can set a room category when creating a room.
    - Categories are selected from candidates prepared in advance by the operations team.
- Users can set a room description when creating a room.
- Users can set a work style when creating a room.
    - Examples: Focus quietly / Chat is welcome
- Users can set a participant limit when creating a room (between 2 and 12 people).
- Logged-in users can join rooms that are available for participation.

### 5.3 System Monitoring

- Prometheus can collect key metrics and Grafana can visualize them.
- When a 500 error occurs, operations staff can be notified immediately.
    - Discord integration is being considered as the destination for immediate notifications.
- The health check endpoint (`/actuator/health`) can be monitored and an alert can be sent when it fails.
- To be considered:
    - Define the metrics to monitor (API response time, error rate, CPU/memory, database connections, concurrent connections, and WebSocket connections).
    - Define alert conditions (for example, 5xx rate, P95 response time, and CPU utilization thresholds and durations).
    - Define the retention period for metrics and logs.
    - Define the required items to display on the monitoring dashboard.

### 5.4 My Page, Profile Editing, and Account Withdrawal

- Users can view their profile information on My Page.
    - Name
    - Icon
    - Bio
    - Work category
- Users can edit their profile information.
- Users can view profile details by tapping a profile icon.
- Users can set whether their profile is visible.
    - When a profile is hidden, other users see only the name and icon.
- Users can search for other users by username.
- Users can view other users' profiles from search results.
- Users can start the account withdrawal process from My Page.
- Display a confirmation message when withdrawing an account to prevent accidental actions.
- After withdrawal is completed, the account cannot be used to log in.

### 5.5 Privacy Policy / Terms of Service

- Provide privacy policy and terms-of-service screens that users can view.
- The privacy policy and terms of service can be viewed regardless of whether the user is logged in, in accordance with the public page requirements in Section 8.1.

## 6. Screen Requirements

### 6.1 User Screens

- Home screen
- Login screen
- User registration screen
- Room creation screen
- Work room screen
- My Page screen
- Profile editing screen
- Withdrawal confirmation screen
- Friend management screen
- User search results screen
- Other user's profile details screen
- Privacy policy screen
- Terms-of-service screen

### 6.2 Reference File

- Screen mockup (draw.io): [screen_wireframe.drawio](./screen_wireframe.drawio)

### 6.3 Common Screen Requirements

- Display a success or failure notification after major operations.
- Check the authentication state during screen transitions and do not display protected screens to logged-out users.
- Display a message that users can understand when an error occurs.

## 7. Non-functional Requirements

### 7.1 Performance

- Concurrent users: Target a maximum of 20 people.
- Page display: Within 2 seconds.
- Chat update: Within 1 second under normal conditions.

### 7.2 Availability

- Service availability: 99.9%

### 7.3 Quality

- Must operate correctly on the latest stable version of Google Chrome.

## 8. Security Requirements

### 8.1 Public Page Requirements

- The privacy policy and terms-of-service screens must be publicly viewable by anyone.

### 8.2 Authentication and Authorization

- All functions other than the public pages in Section 8.1 must require login and enforce access control.

### 8.3 Account Protection

- Do not store passwords in plaintext; store them hashed with bcrypt.
- Apply a rate limit of five consecutive login attempts.
- Manage sessions and invalidate the login session on logout.

### 8.4 Communication and Data Protection

- Communication must use HTTPS and WSS.
- Store and access personal information, such as profile information, under appropriate access control.
- Retain records of important authentication and authorization operations and access for protection against attacks.

### 8.5 Vulnerability Protection

- Protect against SQL injection (using placeholders as a prerequisite).
- Escape user input when displaying it to protect against XSS.
- Implement CSRF protection.

### 8.6 Operational Security

- When an anomaly is detected by the monitoring infrastructure (Prometheus / Grafana), it must be possible to track it during operation.
