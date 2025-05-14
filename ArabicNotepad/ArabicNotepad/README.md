[ArabicNotepad]

ArabicNotepad is a Java-based text editor designed to handle Arabic and markdown content, providing users with basic text formatting, file management, and analytics tools. The application supports CRUD operations for managing files/books, integrates with MySQL for data storage, and offers advanced features like pagination, transliteration, and TF-IDF analytics. It is built for Windows, with potential to expand to a web version in the future, supporting multiple presentation layers.

[Features]

Markdown Support: Basic formatting and text structuring using markdown language.
File Management: Create, read, upload, delete, and discard files and books with ease (CRUD operations).
Database Integration: MySQL is the primary database, with support for multiple databases through the use of facade and abstract factory patterns.

Analytics: Perform text analytics such as Term Frequency-Inverse Document Frequency (TF-IDF) and more to come.
Transliteration: Convert between Arabic script and other writing systems.
Pagination: Efficiently handle large files by displaying content page by page.

Basic Keyboard Shortcuts: Support for commands like:
Ctrl + C: Copy
Ctrl + V: Paste
Ctrl + X: Cut
Ctrl + Z: Undo
Ctrl + Y: Redo

Architecture: Three-layered architecture to separate concerns:
Presentation Layer (UI)
Business Logic Layer
Data Access Layer

Dependency Inversion & Injection: Minimize coupling and promote scalable code design.
Multiple Presentation Layers: Planned expansion to add a web interface alongside the Windows desktop app.

[Installation]

Prerequisites
Java 8 or higher.
MySQL for database management.
Any additional dependencies (e.g., JDBC drivers).

Steps
Clone the repository:
bash
git clone https://github.com/SoftwareConstructionAndDev/24f-prj-scd-21f-9462-21f9463-21f-9500.git

Set up MySQL and configure the connection details in the project (update the config.properties or similar file).

Build and run the project:
If using an IDE, open the project and run the main application file.
If using a command line, compile the Java source files and run the app:
bash
javac -cp .:path/to/mysql-connector.jar ArabicNotepad.java
java ArabicNotepad

[Usage]
Creating/Uploading Files: Use the menu options or drag-and-drop to create or upload books.
Text Editing: Use markdown syntax for formatting text.
Analytics: Analyze text using TF-IDF from the analytics panel.

[Future Enhancements]
Web-based interface for broader accessibility.
Advanced analytics features.
Multi-language support and enhanced text processing tools.

[Contributing]
Feel free to fork the repository and submit pull requests for any features or fixes you’d like to contribute.

[License]
This project is licensed under the MIT License - see the LICENSE file for details.