resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/dora-task"
  retention_in_days = 30

  tags = { Name = "dora-ecs-logs" }
}
