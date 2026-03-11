# 05 Questions Round 2 - CI/CD Pipeline and Deployment

## 1. AWS Compute: ECS Fargate vs EC2

How should the Docker containers run on AWS?

- [ ] (A) **ECS Fargate** — serverless containers; AWS manages the servers; you just define CPU/memory; most modern approach for Docker on AWS; easiest to set up via GitHub Actions
- [ ] (B) **EC2 with Docker** — a virtual machine you SSH into; install Docker yourself; more control but more setup (security groups, AMI, SSH keys)
- [ ] (C) **App Runner** — even simpler than Fargate; point it at an ECR image and it runs; least config but least control
- [ ] (D) Other (describe)

## 2. Container Registry

Where should Docker images be pushed before deploying to AWS?

- [ ] (A) **Amazon ECR (Elastic Container Registry)** — AWS-native, integrates cleanly with ECS/Fargate, free tier for private repos
- [ ] (B) **GitHub Container Registry (ghcr.io)** — free, lives alongside the code, works well with GitHub Actions
- [ ] (C) **Docker Hub** — most familiar, free tier with rate limits
- [ ] (D) Other (describe)

## 3. AWS Credentials

Do you already have an AWS account with IAM credentials (Access Key ID + Secret)?

- [ ] (A) **Yes, I have credentials ready** — I can add them as GitHub Actions Secrets
- [ ] (B) **Yes, but I need to create an IAM user/role for CI** — I have account access but need to set up the right permissions
- [ ] (C) **No AWS account yet** — I need to create one first
- [ ] (D) Other (describe)

## 4. PostgreSQL on AWS

Where should the production PostgreSQL database live?

- [ ] (A) **Amazon RDS (free tier)** — managed PostgreSQL, automatic backups, most realistic enterprise setup
- [ ] (B) **A second Docker container alongside the app** — simpler, no separate AWS service, but data is tied to the container lifecycle
- [ ] (C) Other (describe)
