variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment tag"
  type        = string
  default     = "production"
}

# ── Secrets ───────────────────────────────────────────────────────────────────

variable "db_password" {
  description = "RDS PostgreSQL master password"
  type        = string
  sensitive   = true
}

variable "anthropic_api_key" {
  description = "Anthropic API key for AI insights"
  type        = string
  sensitive   = true
}

variable "github_client_id" {
  description = "GitHub OAuth App client ID for the device flow login"
  type        = string
  sensitive   = true
}

# ── Optional overrides ────────────────────────────────────────────────────────

variable "acm_certificate_arn" {
  description = "ACM certificate ARN to enable HTTPS on the ALB. Leave empty for HTTP only."
  type        = string
  default     = ""
}

variable "backend_image_tag" {
  description = "ECR image tag for the backend container (updated by CD pipeline)"
  type        = string
  default     = "latest"
}

variable "frontend_image_tag" {
  description = "ECR image tag for the frontend container (updated by CD pipeline)"
  type        = string
  default     = "latest"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "ecs_task_cpu" {
  description = "vCPU units for the ECS task (256, 512, 1024, 2048, 4096)"
  type        = string
  default     = "512"
}

variable "ecs_task_memory" {
  description = "Memory (MiB) for the ECS task"
  type        = string
  default     = "1024"
}

variable "ecs_service_desired_count" {
  description = "Number of ECS task instances to run"
  type        = number
  default     = 1
}
