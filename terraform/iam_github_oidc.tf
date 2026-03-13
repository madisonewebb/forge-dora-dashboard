variable "github_repository" {
  description = "GitHub repository in owner/repo format (e.g. madisonewebb/forge-dora-dashboard)"
  type        = string
}

# ── OIDC provider ─────────────────────────────────────────────────────────────
# Allows GitHub Actions to authenticate to AWS without long-lived credentials.

data "tls_certificate" "github" {
  url = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = ["sts.amazonaws.com"]

  thumbprint_list = [
    data.tls_certificate.github.certificates[0].sha1_fingerprint
  ]
}

# ── Shared trust policy helper ─────────────────────────────────────────────────

locals {
  oidc_provider_arn = aws_iam_openid_connect_provider.github_actions.arn
}

# ── CD role (deploy job) ───────────────────────────────────────────────────────
# Trusts only the `production` environment on the target repo.

data "aws_iam_policy_document" "cd_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [local.oidc_provider_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:environment:production"]
    }
  }
}

resource "aws_iam_role" "cd" {
  name               = "dora-github-actions-cd"
  assume_role_policy = data.aws_iam_policy_document.cd_assume.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "cd_permissions" {
  # ECR — push images
  statement {
    sid = "ECRAuth"
    actions = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }
  statement {
    sid = "ECRPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
    ]
    resources = [
      aws_ecr_repository.backend.arn,
      aws_ecr_repository.frontend.arn,
    ]
  }
  # ECS — register + deploy
  statement {
    sid = "ECSRegisterTask"
    actions = ["ecs:RegisterTaskDefinition", "ecs:DescribeTaskDefinition"]
    resources = ["*"]
  }
  statement {
    sid = "ECSDeploy"
    actions = [
      "ecs:UpdateService",
      "ecs:DescribeServices",
    ]
    resources = [aws_ecs_service.app.id]
  }
  # IAM PassRole — required for ECS RegisterTaskDefinition to accept the roles.
  # The PassedToService condition is required; without it ECS returns "Role is not valid".
  statement {
    sid     = "IAMPassRole"
    actions = ["iam:PassRole"]
    resources = [
      aws_iam_role.ecs_execution.arn,
      aws_iam_role.ecs_task.arn,
    ]
    condition {
      test     = "StringLike"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_policy" "cd" {
  name   = "dora-github-actions-cd"
  policy = data.aws_iam_policy_document.cd_permissions.json
}

resource "aws_iam_role_policy_attachment" "cd" {
  role       = aws_iam_role.cd.name
  policy_arn = aws_iam_policy.cd.arn
}

# ── Terraform role (plan + apply) ─────────────────────────────────────────────
# Plan trusts any ref on the repo; apply trusts only pushes to main.

data "aws_iam_policy_document" "terraform_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [local.oidc_provider_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      # Allow plan on PRs and apply on main
      values   = ["repo:${var.github_repository}:*"]
    }
  }
}

resource "aws_iam_role" "terraform" {
  name               = "dora-github-actions-terraform"
  assume_role_policy = data.aws_iam_policy_document.terraform_assume.json
  max_session_duration = 3600
}

# Terraform needs broad permissions to manage all resources it owns.
# Scoped to the "dora-" prefix wherever possible.
data "aws_iam_policy_document" "terraform_permissions" {
  statement {
    sid       = "TerraformStateAccess"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"]
    resources = [
      "arn:aws:s3:::dora-terraform-state-*",
      "arn:aws:s3:::dora-terraform-state-*/*",
    ]
  }
  statement {
    sid    = "CoreInfra"
    effect = "Allow"
    actions = [
      "ec2:*", "elasticloadbalancing:*", "ecs:*", "ecr:*",
      "rds:*", "ssm:*", "logs:*", "kms:*",
      "application-autoscaling:*",
    ]
    resources = ["*"]
  }
  # IAM permissions scoped to dora-* resources only to prevent privilege escalation.
  # This role cannot create or modify IAM resources outside the dora- namespace.
  statement {
    sid    = "IAMScopedToProject"
    effect = "Allow"
    actions = [
      "iam:CreateRole", "iam:DeleteRole", "iam:GetRole", "iam:ListRoles",
      "iam:UpdateRole", "iam:TagRole", "iam:UntagRole",
      "iam:AttachRolePolicy", "iam:DetachRolePolicy", "iam:ListAttachedRolePolicies",
      "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:GetRolePolicy", "iam:ListRolePolicies",
      "iam:CreatePolicy", "iam:DeletePolicy", "iam:GetPolicy", "iam:ListPolicies",
      "iam:GetPolicyVersion", "iam:CreatePolicyVersion", "iam:DeletePolicyVersion",
      "iam:ListPolicyVersions", "iam:TagPolicy", "iam:UntagPolicy",
      "iam:PassRole",
      "iam:CreateOpenIDConnectProvider", "iam:GetOpenIDConnectProvider",
      "iam:DeleteOpenIDConnectProvider", "iam:UpdateOpenIDConnectProviderThumbprint",
      "iam:AddClientIDToOpenIDConnectProvider", "iam:RemoveClientIDFromOpenIDConnectProvider",
      "iam:ListOpenIDConnectProviders",
    ]
    resources = [
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/dora-*",
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:policy/dora-*",
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/token.actions.githubusercontent.com",
    ]
  }
}

resource "aws_iam_policy" "terraform" {
  name   = "dora-github-actions-terraform"
  policy = data.aws_iam_policy_document.terraform_permissions.json
}

resource "aws_iam_role_policy_attachment" "terraform" {
  role       = aws_iam_role.terraform.name
  policy_arn = aws_iam_policy.terraform.arn
}

# ── Outputs for GitHub Actions secrets ────────────────────────────────────────

output "github_actions_cd_role_arn" {
  description = "Set as GH secret AWS_ROLE_ARN in the production environment"
  value       = aws_iam_role.cd.arn
}

output "github_actions_terraform_role_arn" {
  description = "Set as GH secret AWS_TERRAFORM_ROLE_ARN in the repository"
  value       = aws_iam_role.terraform.arn
}
