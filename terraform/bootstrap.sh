#!/usr/bin/env bash
# bootstrap.sh — Create the S3 bucket and DynamoDB table for Terraform remote state.
#
# Run this ONCE before the first `terraform init` with the S3 backend enabled.
# These resources are intentionally managed outside of Terraform to avoid the
# chicken-and-egg problem of storing state for the state backend itself.
#
# Prerequisites:
#   - AWS CLI installed and configured (aws configure or env vars)
#   - Permissions: s3:*, dynamodb:CreateTable, dynamodb:DescribeTable
#
# Usage:
#   chmod +x bootstrap.sh
#   AWS_REGION=us-east-1 ./bootstrap.sh

set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET="dora-terraform-state-${ACCOUNT_ID}-${REGION}"

echo "==> Bootstrapping Terraform remote state"
echo "    Account : $ACCOUNT_ID"
echo "    Region  : $REGION"
echo "    Bucket  : $BUCKET"
echo "    Locking : S3 native (use_lockfile = true, no DynamoDB needed)"
echo ""

# ── S3 bucket ────────────────────────────────────────────────────────────────

if aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  echo "✓ S3 bucket already exists: $BUCKET"
else
  echo "--> Creating S3 bucket..."
  if [ "$REGION" = "us-east-1" ]; then
    aws s3api create-bucket --bucket "$BUCKET" --region "$REGION"
  else
    aws s3api create-bucket \
      --bucket "$BUCKET" \
      --region "$REGION" \
      --create-bucket-configuration LocationConstraint="$REGION"
  fi
  echo "✓ Created S3 bucket"
fi

echo "--> Enabling versioning..."
aws s3api put-bucket-versioning \
  --bucket "$BUCKET" \
  --versioning-configuration Status=Enabled

echo "--> Enabling server-side encryption..."
aws s3api put-bucket-encryption \
  --bucket "$BUCKET" \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"},
      "BucketKeyEnabled": true
    }]
  }'

echo "--> Blocking public access..."
aws s3api put-public-access-block \
  --bucket "$BUCKET" \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

echo "✓ S3 bucket configured"

# ── Print backend config ──────────────────────────────────────────────────────
# Terraform 1.10+ supports S3 native locking (use_lockfile = true) via
# conditional writes, so no DynamoDB table is required.

cat <<EOF

==> Bootstrap complete. Enable remote state in terraform/main.tf:

  backend "s3" {
    bucket       = "$BUCKET"
    key          = "dora-dashboard/terraform.tfstate"
    region       = "$REGION"
    encrypt      = true
    use_lockfile = true
  }

Then run: terraform init -migrate-state
EOF
