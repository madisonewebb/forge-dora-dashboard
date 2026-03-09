# DORA Metrics Dashboard — Project Handoff

## What This Is

A greenfield capstone project for a Liatrio apprentice. The goal is to build an
**AI-native DORA Metrics Dashboard** from scratch and deploy it.

This document captures all context from the planning conversation so it can be
used as input when starting fresh in a new repository.

---

## Capstone Context

**Assignment:** Greenfield AI-Native App
> Build a brand-new product from scratch with AI. Implement at least one advanced
> feature and deploy it.

**Why this project fits:**
- Directly aligned with Liatrio's core business (engineering effectiveness consulting)
- Pulls real data from the GitHub API
- Implements an advanced AI feature (Claude API for metric insights)
- Deployable via Docker
- Demonstrable with any public GitHub repo

---

## What Liatrio Does

Liatrio is an AI enablement and DevOps consulting firm (~132 people) that helps
large enterprises (PNC Bank, Kaiser Permanente, Ritchie Bros.) move faster and
safer with software delivery. Their core practices include:

- Platform Engineering
- Engineering Effectiveness
- Cloud-Native Modernization
- MLOps & Data Science
- Security-Driven Digital Transformation

They recently acquired SuperOrbital for Kubernetes/GitOps training.
GitHub org: https://github.com/liatrio
Labs org: https://github.com/liatrio-labs

---

## What Are DORA Metrics?

DORA = DevOps Research and Assessment (now part of Google). Four metrics that
predict software delivery performance and business outcomes:

| Metric | What it measures | Elite benchmark |
|---|---|---|
| **Deployment Frequency** | How often you ship to production | Multiple times/day |
| **Lead Time for Changes** | Time from first commit → running in prod | < 1 hour |
| **Change Failure Rate** | % of deploys that cause an incident | 0–15% |
| **Mean Time to Restore (MTTR)** | How fast you recover from an outage | < 1 hour |

Liatrio uses these metrics on day 1 of every client engagement. Most enterprises
don't measure them at all — that's the gap this tool fills.

---

## Proposed Application Design

### What It Does

1. User provides a GitHub org/repo and optional personal access token
2. App pulls data from the GitHub API (deployments, workflow runs, pull requests, issues)
3. Calculates all 4 DORA metrics for a configurable time window
4. Displays results in a dashboard with metric cards + trend charts
5. AI panel (Claude API) analyzes the metrics and provides plain-English insights
   and actionable recommendations

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Browser (Frontend)                   │
│                                                          │
│  ┌─────────────────────┐   ┌────────────────────────┐   │
│  │  Metric Cards +     │   │  AI Insights Panel     │   │
│  │  Trend Charts       │   │  (Claude-powered)      │   │
│  └─────────────────────┘   └────────────────────────┘   │
└───────────────────────────────┬─────────────────────────┘
                                │ REST API
                ┌───────────────▼──────────────────┐
                │         Spring Boot Backend       │
                │                                   │
                │  MetricsController (REST)         │
                │  MetricsService (calculation)     │
                │  GitHubApiClient (data ingestion) │
                │  InsightsService (Claude API)     │
                └──────┬──────────────────┬─────────┘
                       │                  │
               GitHub API            Claude API
               (data source)         (AI insights)
```

### Suggested Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Backend | Spring Boot 3 + Java 17 | Aligns with apprentice's existing skills |
| HTTP Client | Spring WebClient or Feign | GitHub API calls |
| Frontend | Thymeleaf + Chart.js OR React | TBD (see questions) |
| AI | Claude API (Anthropic) via Spring AI | Advanced AI feature |
| Database | H2 (dev) / PostgreSQL (prod) | Cache API responses |
| Containerization | Docker + Docker Compose | Deployment requirement |
| Testing | JUnit 5, Mockito, Playwright | TDD per project standards |

---

## Proposed Spec Breakdown

Because this is a full application, it should be broken into 4 specs:

| Spec | Title | Delivers |
|---|---|---|
| 1 | GitHub Data Ingestion | GitHub API client, data models, raw data storage |
| 2 | DORA Metrics Engine | Calculation logic for all 4 metrics, REST API |
| 3 | Dashboard UI | Frontend metric cards + trend charts |
| 4 | AI Insights Panel | Claude API integration, plain-English analysis |

Start with Spec 1+2 for a working backend, add Spec 3+4 for the demo wow factor.

---

## Questions Still To Answer

These clarifying questions need answers before the full spec is written.
See the questions file alongside this document, or answer inline below:

### 1. Target User
Who is the primary user of this dashboard?
- [ ] (A) Me / a developer wanting to see their own repo's metrics
- [ ] (B) A Liatrio consultant showing metrics to a client
- [ ] (C) An engineering manager tracking team performance
- [ ] (D) Any developer who can point it at any GitHub org/repo

### 2. GitHub Scope
- [ ] (A) Single repository at a time
- [ ] (B) All repos across a GitHub organization
- [ ] (C) Single repo first, org support as stretch goal

### 3. Data Source for Metrics
- [ ] (A) GitHub Deployments API
- [ ] (B) GitHub Actions workflow runs
- [ ] (C) Pull Requests + merge times
- [ ] (D) A mix — whatever is available

### 4. Change Failure Rate & MTTR Source
- [ ] (A) GitHub Issues tagged "incident" or "bug"
- [ ] (B) PRs with "hotfix" or "revert" label
- [ ] (C) Skip these two for now — focus on Deploy Frequency + Lead Time

### 5. Frontend Technology
- [ ] (A) Thymeleaf + Chart.js (simpler, server-rendered)
- [ ] (B) React SPA (more modern, separate from backend)
- [ ] (C) Whatever is fastest to demo

### 6. AI Insights
- [ ] (A) Summarize current state in plain English
- [ ] (B) Identify trends with explanations
- [ ] (C) Give specific, actionable advice
- [ ] (D) All of the above

### 7. Time Range
- [ ] (A) Last 30 days (fixed)
- [ ] (B) Last 90 days (fixed)
- [ ] (C) User-selectable (30 / 90 / 180 days)

### 8. Authentication
- [ ] (A) No login — GitHub token in env file
- [ ] (B) No login — public repos only
- [ ] (C) User enters GitHub PAT in the UI
- [ ] (D) OAuth — "Sign in with GitHub"

### 9. Deployment Target
- [ ] (A) Docker Compose locally
- [ ] (B) Any cloud via Docker
- [ ] (C) Specific cloud provider (which one?)

### 10. Capstone Priorities
- [ ] (A) Works end-to-end with real GitHub data
- [ ] (B) Looks impressive in a demo
- [ ] (C) Good test coverage and clean code
- [ ] (D) Real deployment (not just local)
- [ ] (E) All of the above

---

## Next Steps

1. Create a new GitHub repository (suggested name: `dora-metrics-dashboard`)
2. Answer the questions above
3. Run `/SDD-1-generate-spec` with this file as context to generate the full spec
4. Run `/SDD-2-generate-task-list-from-spec` to break it into tasks
5. Build it following strict TDD (RED → GREEN → REFACTOR)
6. Deploy it

---

## Reference Links

- [Liatrio Website](https://www.liatrio.com/)
- [Liatrio GitHub](https://github.com/liatrio)
- [Liatrio Labs GitHub](https://github.com/liatrio-labs)
- [DORA Research](https://dora.dev)
- [GitHub REST API Docs](https://docs.github.com/en/rest)
- [Spring AI Docs](https://docs.spring.io/spring-ai/reference/)
- [Anthropic Claude API](https://docs.anthropic.com/)
