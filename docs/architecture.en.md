# MVP

- User registration
- User login
- Room creation

# Functional Requirements

- Authentication
    - Login
    - User registration
- Online workspace
    - Create a workspace
        - Participant limit
        - Logged-in users can join
- A feature to exclude people who are not there for work purposes
- Rather than random matching, open rooms so that logged-in users can join them.
- A feature to merge a room with a similar room when creating it could also be interesting.
    - Allow each room to have a genre.

# Creation Pages

- User
    - Registration
    - Login
    - My Page
        - Edit user information
- Workspace list (top page)
- Workspace creation screen
- Workspace details

# Table Design

## User

- id
- name
- email
- password
- created_at
- modified_at

## Workspace

- id
- title
- description
- created_at
- modified_at
