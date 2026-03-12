resource "aws_ecs_cluster" "main" {
  name = "dora-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = { Name = "dora-cluster" }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
  }
}

# ── Task definition ───────────────────────────────────────────────────────────

locals {
  backend_image  = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
  frontend_image = "${aws_ecr_repository.frontend.repository_url}:${var.frontend_image_tag}"
}

resource "aws_ecs_task_definition" "app" {
  family                   = "dora-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.ecs_task_cpu
  memory                   = var.ecs_task_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = local.backend_image
      essential = true

      portMappings = [
        { containerPort = 8080, protocol = "tcp" }
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" }
      ]

      secrets = [
        { name = "DB_URL",            valueFrom = aws_ssm_parameter.db_url.arn },
        { name = "DB_USERNAME",       valueFrom = aws_ssm_parameter.db_username.arn },
        { name = "DB_PASSWORD",       valueFrom = aws_ssm_parameter.db_password.arn },
        { name = "ANTHROPIC_API_KEY", valueFrom = aws_ssm_parameter.anthropic_api_key.arn },
        { name = "GITHUB_CLIENT_ID",  valueFrom = aws_ssm_parameter.github_client_id.arn }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "backend"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    },
    {
      name      = "frontend"
      image     = local.frontend_image
      essential = true

      portMappings = [
        { containerPort = 80, protocol = "tcp" }
      ]

      environment = [
        # Both containers share the task network namespace, so the backend is on localhost
        { name = "BACKEND_HOST", value = "localhost" }
      ]

      dependsOn = [
        { containerName = "backend", condition = "HEALTHY" }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "frontend"
        }
      }
    }
  ])

  tags = { Name = "dora-task" }
}

# ── Service ───────────────────────────────────────────────────────────────────

resource "aws_ecs_service" "app" {
  name            = "dora-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.ecs_service_desired_count
  launch_type     = "FARGATE"

  # Replace old tasks before stopping new ones during deployments
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "frontend"
    container_port   = 80
  }

  depends_on = [
    aws_lb_listener.http,
    aws_iam_role_policy_attachment.ecs_execution_managed,
  ]

  # Ignore task_definition changes — the CD pipeline updates the service directly
  lifecycle {
    ignore_changes = [task_definition]
  }

  tags = { Name = "dora-service" }
}
