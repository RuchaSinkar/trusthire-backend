# TrustHire

TrustHire is a full-stack web application that helps students, fresh graduates, and job seekers identify potentially fraudulent job opportunities before applying.
The platform evaluates job postings using multiple trust signals such as domain age, email legitimacy, company presence, community reports, and scam-pattern detection to generate a transparent trust score.

---

## Why TrustHire?

The idea for TrustHire originated from a personal experience.
After receiving an internship offer that initially appeared legitimate, several warning signs—including an unusually high stipend and a pressured acceptance deadline—revealed that the company was likely fraudulent.
This experience highlighted how vulnerable students and freshers can be during their job search and inspired the creation of TrustHire.

---

## Features

### Job Scam Analysis
Analyze job descriptions and offer letters using multiple credibility signals.

### Trust Score Generation
Generates a trust score between 0 and 100 based on:

- Domain age verification
- Company LinkedIn presence
- Email legitimacy checks
- Community scam reports
- Suspicious language detection
- Hiring process validation

### Community Reporting
Users can report suspicious companies and job postings to help protect other job seekers.

### Explainable Results
TrustHire provides a detailed score breakdown explaining exactly why a posting was marked as legitimate, suspicious, or fraudulent.

### Secure Authentication
JWT-based authentication and authorization system.

---

## How TrustHire Works

TrustHire evaluates opportunities using a weighted scoring engine.

### Positive Signals

| Signal | Score |
|----------|---------|
| Established domain | +5 |
| LinkedIn company presence | +5 |
| Domain-matched email | Positive |
| Coding assessment mentioned | +10 |
| Structured interview process | +10 |

### Risk Signals

| Signal | Penalty |
|----------|---------|
| New domain (< 6 months) | -25 |
| No LinkedIn presence | -15 |
| Training or registration fees | -35 |
| Upfront payment requests | -35 |
| Personal document requests | -30 |
| Urgency tactics | -20 |
| Guaranteed placement claims | -20 |
| Unrealistic stipend promises | -10 |

---

## Tech Stack

### Frontend

- React
- Vite
- JavaScript
- CSS

### Backend

- Spring Boot
- Spring Security
- JWT Authentication

### Database

- PostgreSQL
- Flyway

### External Services

- WHOIS API
- Google Safe Browsing API

### DevOps

- Docker
- Nginx
- Render

---

## Architecture

```text
React Frontend
       |
       v
Spring Boot REST API
       |
       v
Scam Detection Engine
       |
       +---- Domain Verification
       |
       +---- LinkedIn Verification
       |
       +---- Email Validation
       |
       +---- Community Reports
       |
       +---- Pattern Detection
       |
       v
PostgreSQL Database
```
---

## Core Components

### Scam Detection Service

Analyzes:

- Training fees
- Upfront payment requests
- Personal document requests
- Urgency tactics
- Guaranteed placements
- Unrealistic stipends
- Vague job descriptions

### Domain Verification
Uses WHOIS data to verify domain age and detect newly registered websites.

### Email Validation
Checks whether contact emails match the organization's domain.

### Community Intelligence
Incorporates user-submitted scam reports into risk calculations.

---

## Screenshots

### Home Page
<img width="959" height="409" alt="image" src="https://github.com/user-attachments/assets/3f8fbede-a2c8-4572-8970-33574b4b4b55" />

### Analysis Page
<img width="959" height="396" alt="image" src="https://github.com/user-attachments/assets/025ad054-da07-4203-b1f9-9220196adc52" />

### Community Reports
<img width="959" height="412" alt="image" src="https://github.com/user-attachments/assets/6d58d995-a039-4029-853b-88b27c75084e" />

---

## What I Learned
Through TrustHire, I gained practical experience in:

- Full-stack development
- REST API design
- Spring Security
- JWT authentication
- PostgreSQL database design
- Docker containerization
- External API integration
- Secure application deployment
- Product design and problem validation
 
 ---
 
## Future Improvements
- AI-powered scam detection
- Browser extension for instant verification
- Resume and job matching
- Company reputation scoring
- Admin moderation dashboard
- Machine learning based risk prediction

---

## Live Demo
https://trusthire-frontend-1orw.onrender.com/

---

## Author
Rucha Sinkar
Designed, developed, deployed, and maintained independently.
