# Contributing to MobileGPT

Thank you for considering contributing to MobileGPT! We appreciate your time and effort to help improve this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Pull Request Process](#pull-request-process)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Enhancements](#suggesting-enhancements)

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please be respectful, inclusive, and considerate in all interactions.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When you create a bug report, include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples**
- **Describe the behavior you observed and what you expected**
- **Include screenshots if applicable**
- **Include your environment details**:
  - Android version
  - Device model
  - Python version
  - Server logs if relevant

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **A clear and descriptive title**
- **A detailed description of the proposed functionality**
- **Explain why this enhancement would be useful**
- **List any alternatives you've considered**

### Your First Code Contribution

Unsure where to begin? You can start by looking through `good-first-issue` and `help-wanted` issues:

- **Good first issues**: Issues that should only require a few lines of code
- **Help wanted issues**: Issues that may be more involved

## Development Setup

### Prerequisites

- Android Studio Arctic Fox or later
- Python 3.8+
- Git
- OpenAI API key (for testing AI features)

### Setting Up Your Development Environment

1. **Fork the repository** on GitHub

2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/mobilegpt.git
   cd mobilegpt
   ```

3. **Set up the Android app:**
   ```bash
   cp local.properties.example local.properties
   # Edit local.properties with your configuration
   ```

4. **Set up the Flask server:**
   ```bash
   cd app/mobilegpt-server
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   cp .env.example .env
   # Edit .env with your OpenAI API key
   ```

5. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Coding Standards

### Kotlin (Android)

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Use Jetpack Compose best practices
- Format code using Android Studio's built-in formatter (Ctrl+Alt+L)

**Example:**
```kotlin
// Good
fun calculateSessionDuration(startTime: Long, endTime: Long): Long {
    return endTime - startTime
}

// Bad
fun calc(s: Long, e: Long): Long {
    return e - s
}
```

### Python (Flask Server)

- Follow [PEP 8](https://pep8.org/) style guide
- Use type hints where applicable
- Add docstrings for functions and classes
- Keep functions focused and single-purpose
- Use descriptive variable names

**Example:**
```python
# Good
def analyze_user_session(session_data: dict) -> list:
    """
    Analyze user session data and generate learning steps.

    Args:
        session_data: Dictionary containing session events

    Returns:
        List of generated learning steps
    """
    # Implementation here
    pass

# Bad
def analyze(data):
    # Implementation here
    pass
```

### General Guidelines

- **Don't commit sensitive data**: Never commit API keys, passwords, or personal information
- **Write meaningful commit messages**: Use the present tense ("Add feature" not "Added feature")
- **Keep commits atomic**: One logical change per commit
- **Update documentation**: If you change functionality, update the README or other docs

## Pull Request Process

1. **Update documentation** if you've made changes to:
   - API endpoints
   - Configuration options
   - User-facing features

2. **Test your changes thoroughly**:
   ```bash
   # For Android
   ./gradlew test
   ./gradlew connectedAndroidTest

   # For Python (if you add tests)
   cd app/mobilegpt-server
   pytest
   ```

3. **Ensure your code follows the coding standards** mentioned above

4. **Update the README.md** if necessary with details of:
   - New environment variables
   - New dependencies
   - Changed functionality

5. **Create a Pull Request** with:
   - A clear title describing the change
   - A detailed description of what changed and why
   - Reference to any related issues (e.g., "Fixes #123")
   - Screenshots or GIFs for UI changes

6. **Wait for review**: Maintainers will review your PR and may request changes

### Pull Request Template

```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] I have tested these changes locally
- [ ] I have added tests that prove my fix/feature works
- [ ] All existing tests pass

## Screenshots (if applicable)
Add screenshots here

## Related Issues
Fixes #(issue number)
```

## Branch Naming Convention

Use descriptive branch names:

- `feature/add-session-export` - New features
- `fix/accessibility-crash` - Bug fixes
- `docs/update-readme` - Documentation changes
- `refactor/cleanup-network-layer` - Code refactoring
- `test/add-unit-tests` - Adding tests

## Commit Message Guidelines

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(server): add endpoint for exporting sessions to PDF

fix(accessibility): resolve crash when service is disabled

docs(readme): update installation instructions for Windows
```

## Questions?

Feel free to:
- Open an issue with the `question` label
- Reach out to the maintainers
- Start a discussion in the Discussions tab

## Recognition

Contributors will be recognized in:
- The project README
- Release notes for significant contributions
- Our contributors page

---

Thank you for contributing to MobileGPT!
