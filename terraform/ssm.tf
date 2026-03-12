# SSM SecureString parameters — values are set from Terraform variables so they
# are never checked into source control.  The ECS execution role reads these at
# task startup and injects them as container environment variables.

resource "aws_ssm_parameter" "db_url" {
  name  = "/dora/DB_URL"
  type  = "SecureString"
  # Construct the JDBC URL from the RDS endpoint once RDS is provisioned
  value = "jdbc:postgresql://${aws_db_instance.main.endpoint}/dora"

  tags = { Name = "dora/DB_URL" }
}

resource "aws_ssm_parameter" "db_username" {
  name  = "/dora/DB_USERNAME"
  type  = "String" # not sensitive — avoids unnecessary KMS decrypt on every task startup
  value = "dora"

  tags = { Name = "dora/DB_USERNAME" }
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/dora/DB_PASSWORD"
  type  = "SecureString"
  value = var.db_password

  tags = { Name = "dora/DB_PASSWORD" }
}

resource "aws_ssm_parameter" "anthropic_api_key" {
  name  = "/dora/ANTHROPIC_API_KEY"
  type  = "SecureString"
  value = var.anthropic_api_key

  tags = { Name = "dora/ANTHROPIC_API_KEY" }
}

resource "aws_ssm_parameter" "github_client_id" {
  name  = "/dora/GITHUB_CLIENT_ID"
  type  = "SecureString"
  value = var.github_client_id

  tags = { Name = "dora/GITHUB_CLIENT_ID" }
}
