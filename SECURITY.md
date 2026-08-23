# Security Policy

## Supported Versions

This is a personal study project. Only the latest commit on `main` receives fixes.

| Version | Supported |
| ------- | --------- |
| `main`  | Yes       |
| Older   | No        |

## Reporting a Vulnerability

Report privately through GitHub: open the **Security** tab of this repository and choose
**Report a vulnerability**. Do not open a public issue for a security problem.

Include the affected endpoint or class, the steps to reproduce, and the impact you observed.

Reports are handled on a best-effort basis, with no response time guarantee.

## Automated Checks

Every pull request to `main` runs CodeQL static analysis and a dependency review that
fails on high or critical advisories. Dependabot opens pull requests for vulnerable and
outdated dependencies, and secret scanning with push protection guards the repository
history.
