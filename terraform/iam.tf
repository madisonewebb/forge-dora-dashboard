# ── ECS task execution role ───────────────────────────────────────────────────
# Used by the ECS agent to pull images from ECR and fetch secrets from SSM.

data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_execution" {
  name               = "dora-ecs-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecs_execution_managed" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Grant the execution role permission to read the SSM parameters at startup
data "aws_iam_policy_document" "ssm_read" {
  statement {
    sid     = "ReadDoraSecrets"
    actions = ["ssm:GetParameters", "ssm:GetParameter"]
    resources = [
      "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/dora/*"
    ]
  }

  statement {
    sid     = "DecryptSSMKMS"
    actions = ["kms:Decrypt"]
    # Scoped to the AWS-managed SSM key only, not all keys in the account.
    resources = ["arn:aws:kms:${var.aws_region}:${data.aws_caller_identity.current.account_id}:alias/aws/ssm"]
  }
}

resource "aws_iam_policy" "ssm_read" {
  name   = "dora-ssm-read"
  policy = data.aws_iam_policy_document.ssm_read.json
}

resource "aws_iam_role_policy_attachment" "ssm_read" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = aws_iam_policy.ssm_read.arn
}

# ── ECS task role ──────────────────────────────────────────────────────────────
# Assumed by the running containers themselves (not the agent).
# Add permissions here if containers need to call AWS services directly.

resource "aws_iam_role" "ecs_task" {
  name               = "dora-ecs-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}
